package vn.edu.vlu.restaurant.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import vn.edu.vlu.restaurant.domain.MenuCategory;
import vn.edu.vlu.restaurant.domain.MenuItem;
import vn.edu.vlu.restaurant.repository.MenuCategoryRepository;
import vn.edu.vlu.restaurant.repository.MenuItemRepository;

/** UC-08 Manage menu (FR-08) and the sold-out switch of UC-10 (BR-07). */
class MenuControllerTest extends ApiTestSupport {

    private static final RequestPostProcessor MANAGER = user("manager").roles("MANAGER");
    private static final RequestPostProcessor WAITER = user("waiter").roles("WAITER_HOST");
    private static final RequestPostProcessor KITCHEN = user("kitchen").roles("KITCHEN_STAFF");

    @Autowired
    private MenuItemRepository itemRepository;

    @Autowired
    private MenuCategoryRepository categoryRepository;

    @Test
    @DisplayName("TC-MENU-01 A Manager adds a menu item, which starts in stock and active")
    void createItem() throws Exception {
        mockMvc.perform(post("/api/menu/items").with(MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(itemBody("Grilled squid", 120_000, "MAIN", categoryId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Grilled squid"))
                .andExpect(jsonPath("$.price").value(120_000))
                .andExpect(jsonPath("$.course").value("MAIN"))
                .andExpect(jsonPath("$.inStock").value(true))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("TC-MENU-02 A price of 0 is rejected")
    void priceMustBePositive() throws Exception {
        mockMvc.perform(post("/api/menu/items").with(MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(itemBody("Free soup", 0, "STARTER", categoryId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.price").value("Price must be between 1 and 10,000,000 VND"));
    }

    @Test
    @DisplayName("TC-MENU-03 A blank name is rejected")
    void nameIsRequired() throws Exception {
        mockMvc.perform(post("/api/menu/items").with(MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(itemBody("   ", 50_000, "MAIN", categoryId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").value("Item name is required"));
    }

    @Test
    @DisplayName("TC-MENU-04 An item name that already exists in the category is rejected")
    void duplicateNameInCategory() throws Exception {
        MenuItem existing = itemRepository.search(false, false).get(0);

        mockMvc.perform(post("/api/menu/items").with(MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(itemBody(existing.getName().toUpperCase(), 50_000, "MAIN",
                                existing.getCategory().getId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    @DisplayName("TC-MENU-05 Only a Manager can change the menu")
    void waiterCannotCreateItem() throws Exception {
        mockMvc.perform(post("/api/menu/items").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(itemBody("Secret dish", 50_000, "MAIN", categoryId()))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-MENU-06 Kitchen marks an item out of stock and it leaves the order screen list")
    void outOfStockItemIsHiddenFromOrderScreens() throws Exception {
        MenuItem item = itemRepository.search(false, false).get(0);

        mockMvc.perform(post("/api/menu/items/{id}/out-of-stock", item.getId()).with(KITCHEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inStock").value(false));

        mockMvc.perform(get("/api/menu/items").param("availableOnly", "true").with(WAITER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + item.getId() + ")]").isEmpty());
        mockMvc.perform(get("/api/menu/items").with(MANAGER))
                .andExpect(jsonPath("$[?(@.id == " + item.getId() + ")]").isNotEmpty());
    }

    @Test
    @DisplayName("TC-MENU-07 A restocked item returns to the order screen list")
    void restockedItemReturns() throws Exception {
        MenuItem item = itemRepository.search(false, false).get(0);
        item.markOutOfStock();

        mockMvc.perform(post("/api/menu/items/{id}/restock", item.getId()).with(KITCHEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inStock").value(true));

        mockMvc.perform(get("/api/menu/items").param("availableOnly", "true").with(WAITER))
                .andExpect(jsonPath("$[?(@.id == " + item.getId() + ")]").isNotEmpty());
    }

    @Test
    @DisplayName("A waiter cannot mark an item out of stock")
    void waiterCannotMarkOutOfStock() throws Exception {
        MenuItem item = itemRepository.search(false, false).get(0);

        mockMvc.perform(post("/api/menu/items/{id}/out-of-stock", item.getId()).with(WAITER))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A deactivated item is hidden from staff and still listed for the Manager")
    void deactivatedItem() throws Exception {
        MenuItem item = itemRepository.search(false, false).get(0);

        mockMvc.perform(post("/api/menu/items/{id}/deactivate", item.getId()).with(MANAGER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/menu/items").with(WAITER))
                .andExpect(jsonPath("$[?(@.id == " + item.getId() + ")]").isEmpty());
        mockMvc.perform(get("/api/menu/items").param("includeInactive", "true").with(MANAGER))
                .andExpect(jsonPath("$[?(@.id == " + item.getId() + ")]").isNotEmpty());

        mockMvc.perform(post("/api/menu/items/{id}/activate", item.getId()).with(MANAGER))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("Only a Manager can list inactive items")
    void waiterCannotListInactiveItems() throws Exception {
        mockMvc.perform(get("/api/menu/items").param("includeInactive", "true").with(WAITER))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A Manager can edit the name, price and course of an item")
    void updateItem() throws Exception {
        MenuItem item = itemRepository.search(false, false).get(0);

        mockMvc.perform(put("/api/menu/items/{id}", item.getId()).with(MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(itemBody("Renamed dish", 99_000, "DESSERT", item.getCategory().getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed dish"))
                .andExpect(jsonPath("$.price").value(99_000))
                .andExpect(jsonPath("$.course").value("DESSERT"));
    }

    @Test
    @DisplayName("Editing an item that does not exist returns 404")
    void unknownItem() throws Exception {
        mockMvc.perform(put("/api/menu/items/{id}", 999_999).with(MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(itemBody("Ghost", 50_000, "MAIN", categoryId()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Menu item not found"));
    }

    @Test
    @DisplayName("An unknown category is rejected with a message on the category field")
    void unknownCategory() throws Exception {
        mockMvc.perform(post("/api/menu/items").with(MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(itemBody("Lost dish", 50_000, "MAIN", 999_999L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.categoryId").value("Category not found"));
    }

    @Test
    @DisplayName("A course that is not Starter, Main or Dessert is rejected")
    void invalidCourse() throws Exception {
        mockMvc.perform(post("/api/menu/items").with(MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(itemBody("Odd dish", 50_000, "SNACK", categoryId()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A Manager can add a category, and a duplicate name is rejected")
    void createCategory() throws Exception {
        mockMvc.perform(post("/api/menu/categories").with(MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Hot pots"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Hot pots"));

        mockMvc.perform(post("/api/menu/categories").with(MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "hot pots"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    private Long categoryId() {
        MenuCategory category = categoryRepository.findAllByOrderByNameAsc().get(0);
        return category.getId();
    }

    private Map<String, Object> itemBody(String name, long price, String course, Long categoryId) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("description", null);
        body.put("price", price);
        body.put("course", course);
        body.put("categoryId", categoryId);
        return body;
    }
}
