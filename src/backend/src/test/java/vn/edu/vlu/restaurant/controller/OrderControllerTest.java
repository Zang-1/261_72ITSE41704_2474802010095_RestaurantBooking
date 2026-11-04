package vn.edu.vlu.restaurant.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import vn.edu.vlu.restaurant.domain.DiningTable;
import vn.edu.vlu.restaurant.domain.MenuItem;
import vn.edu.vlu.restaurant.domain.TableStatus;
import vn.edu.vlu.restaurant.repository.CustomerOrderRepository;
import vn.edu.vlu.restaurant.repository.DiningTableRepository;
import vn.edu.vlu.restaurant.repository.MenuItemRepository;

/** UC-08 Create order (FR-11) and edit its lines before sending (FR-12). */
class OrderControllerTest extends ApiTestSupport {

    private static final RequestPostProcessor WAITER = user("waiter").roles("WAITER_HOST");
    private static final RequestPostProcessor KITCHEN = user("kitchen").roles("KITCHEN_STAFF");

    @Autowired
    private DiningTableRepository tableRepository;

    @Autowired
    private MenuItemRepository itemRepository;

    @Autowired
    private CustomerOrderRepository orderRepository;

    @Test
    @DisplayName("TC-ORD-01 A Waiter/Host creates an empty order for an Occupied table")
    void createOrder() throws Exception {
        DiningTable table = occupiedTable();

        mockMvc.perform(post("/api/orders").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("tableId", table.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tableNumber").value(table.getNumber()))
                .andExpect(jsonPath("$.waiter").value("waiter"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.lines").isEmpty())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    @DisplayName("TC-ORD-02 An order cannot be created for a table that is not Occupied")
    void tableMustBeOccupied() throws Exception {
        DiningTable table = tableRepository.findByActiveTrueOrderByNumberAsc().get(0);

        mockMvc.perform(post("/api/orders").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("tableId", table.getId()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Table " + table.getNumber()
                        + " is not occupied; seat the guests before taking an order"));
    }

    @Test
    @DisplayName("TC-ORD-03 A table cannot have two open orders")
    void onlyOneOpenOrderPerTable() throws Exception {
        DiningTable table = occupiedTable();
        createOrder(table);

        mockMvc.perform(post("/api/orders").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("tableId", table.getId()))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("TC-ORD-04 Kitchen Staff cannot create an order")
    void kitchenCannotCreateOrder() throws Exception {
        mockMvc.perform(post("/api/orders").with(KITCHEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("tableId", occupiedTable().getId()))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-ORD-05 Adding a dish copies its price and updates the total")
    void addLine() throws Exception {
        long orderId = createOrder(occupiedTable());
        MenuItem item = orderableItem();

        mockMvc.perform(post("/api/orders/{id}/lines", orderId).with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(lineBody(item.getId(), 2, "  no onion  "))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lines[0].menuItemName").value(item.getName()))
                .andExpect(jsonPath("$.lines[0].course").value(item.getCourse().name()))
                .andExpect(jsonPath("$.lines[0].quantity").value(2))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(item.getPrice()))
                .andExpect(jsonPath("$.lines[0].note").value("no onion"))
                .andExpect(jsonPath("$.total").value(item.getPrice() * 2));
    }

    @Test
    @DisplayName("TC-ORD-06 Quantity must be between 1 and 50")
    void quantityLimits() throws Exception {
        long orderId = createOrder(occupiedTable());
        Long itemId = orderableItem().getId();

        for (int quantity : new int[] {0, 51}) {
            mockMvc.perform(post("/api/orders/{id}/lines", orderId).with(WAITER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(lineBody(itemId, quantity, null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.quantity").value("Quantity must be between 1 and 50"));
        }
    }

    @Test
    @DisplayName("TC-ORD-07 A note longer than 200 characters is rejected")
    void noteLimit() throws Exception {
        long orderId = createOrder(occupiedTable());

        mockMvc.perform(post("/api/orders/{id}/lines", orderId).with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(lineBody(orderableItem().getId(), 1, "x".repeat(201)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.note").value("Note must be at most 200 characters"));
    }

    @Test
    @DisplayName("TC-ORD-08 An out-of-stock dish cannot be added (BR-07)")
    void outOfStockItemRefused() throws Exception {
        long orderId = createOrder(occupiedTable());
        MenuItem item = orderableItem();
        item.markOutOfStock();

        mockMvc.perform(post("/api/orders/{id}/lines", orderId).with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(lineBody(item.getId(), 1, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("\"" + item.getName() + "\" is not available right now"));
    }

    @Test
    @DisplayName("TC-ORD-09 The quantity and note of a line can be changed")
    void changeLine() throws Exception {
        long orderId = createOrder(occupiedTable());
        MenuItem item = orderableItem();
        long lineId = addLine(orderId, item.getId(), 1);

        mockMvc.perform(put("/api/orders/{id}/lines/{lineId}", orderId, lineId).with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("quantity", 3, "note", "less spicy"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].quantity").value(3))
                .andExpect(jsonPath("$.lines[0].note").value("less spicy"))
                .andExpect(jsonPath("$.total").value(item.getPrice() * 3));
    }

    @Test
    @DisplayName("TC-ORD-10 A line can be removed")
    void removeLine() throws Exception {
        long orderId = createOrder(occupiedTable());
        long lineId = addLine(orderId, orderableItem().getId(), 1);

        mockMvc.perform(delete("/api/orders/{id}/lines/{lineId}", orderId, lineId).with(WAITER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines").isEmpty())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    @DisplayName("TC-ORD-11 A locked order cannot be changed (BR-09)")
    void lockedOrderRefused() throws Exception {
        long orderId = createOrder(occupiedTable());
        long lineId = addLine(orderId, orderableItem().getId(), 1);
        orderRepository.findById(orderId).orElseThrow().lock();

        mockMvc.perform(post("/api/orders/{id}/lines", orderId).with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(lineBody(orderableItem().getId(), 1, null))))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/orders/{id}/lines/{lineId}", orderId, lineId).with(WAITER))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("TC-ORD-12 An unknown order or a line of another order returns 404")
    void unknownOrderOrLine() throws Exception {
        long orderId = createOrder(occupiedTable());
        long otherOrderId = createOrder(occupiedTable());
        long otherLineId = addLine(otherOrderId, orderableItem().getId(), 1);

        mockMvc.perform(get("/api/orders/{id}", 999_999).with(WAITER))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/orders/{id}/lines/{lineId}", orderId, otherLineId).with(WAITER))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TC-ORD-13 The order screen finds the current order of a table")
    void currentOrderOfTable() throws Exception {
        DiningTable table = occupiedTable();
        long orderId = createOrder(table);

        mockMvc.perform(get("/api/orders").param("tableId", table.getId().toString()).with(WAITER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(orderId));
    }

    /** Takes the next Available table and seats guests there, as a walk-in would. */
    private DiningTable occupiedTable() {
        DiningTable table = tableRepository.findByActiveTrueOrderByNumberAsc().stream()
                .filter(t -> t.getStatus() == TableStatus.AVAILABLE)
                .findFirst().orElseThrow();
        table.changeStatus(TableStatus.OCCUPIED);
        return table;
    }

    private MenuItem orderableItem() {
        return itemRepository.search(false, true).get(0);
    }

    private long createOrder(DiningTable table) throws Exception {
        String body = mockMvc.perform(post("/api/orders").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("tableId", table.getId()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long addLine(long orderId, Long itemId, int quantity) throws Exception {
        String body = mockMvc.perform(post("/api/orders/{id}/lines", orderId).with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(lineBody(itemId, quantity, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.lines[-1].id")).longValue();
    }

    private Map<String, Object> lineBody(Long menuItemId, int quantity, String note) {
        Map<String, Object> body = new HashMap<>();
        body.put("menuItemId", menuItemId);
        body.put("quantity", quantity);
        body.put("note", note);
        return body;
    }
}
