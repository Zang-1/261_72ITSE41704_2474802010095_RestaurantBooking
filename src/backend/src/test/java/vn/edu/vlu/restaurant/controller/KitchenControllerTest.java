package vn.edu.vlu.restaurant.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import vn.edu.vlu.restaurant.domain.Course;
import vn.edu.vlu.restaurant.domain.DiningTable;
import vn.edu.vlu.restaurant.domain.MenuItem;
import vn.edu.vlu.restaurant.domain.TableStatus;
import vn.edu.vlu.restaurant.repository.DiningTableRepository;
import vn.edu.vlu.restaurant.repository.KitchenTicketRepository;
import vn.edu.vlu.restaurant.repository.MenuItemRepository;

/** UC-08 steps 4-7 Send to kitchen (FR-13, FR-14, FR-18) and UC-09 Process kitchen ticket (FR-15). */
class KitchenControllerTest extends ApiTestSupport {

    private static final RequestPostProcessor WAITER = user("waiter").roles("WAITER_HOST");
    private static final RequestPostProcessor KITCHEN = user("kitchen").roles("KITCHEN_STAFF");

    @Autowired
    private DiningTableRepository tableRepository;

    @Autowired
    private MenuItemRepository itemRepository;

    @Autowired
    private KitchenTicketRepository ticketRepository;

    @Test
    @DisplayName("TC-KIT-01 Sending creates one ticket that the kitchen sees in course order")
    void sendCreatesTicket() throws Exception {
        DiningTable table = occupiedTable();
        long orderId = createOrder(table);
        addLine(orderId, item(Course.MAIN));
        addLine(orderId, item(Course.STARTER));

        mockMvc.perform(post("/api/orders/{id}/send", orderId).with(WAITER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].kitchenStatus").value("NEW"))
                .andExpect(jsonPath("$.lines[1].kitchenStatus").value("NEW"));

        mockMvc.perform(get("/api/kitchen/tickets").with(KITCHEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tableNumber").value(table.getNumber()))
                .andExpect(jsonPath("$[0].items[0].course").value("STARTER"))
                .andExpect(jsonPath("$[0].items[1].course").value("MAIN"))
                .andExpect(jsonPath("$[0].items[0].status").value("NEW"));
    }

    @Test
    @DisplayName("TC-KIT-02 Sending again creates no duplicate ticket (NFR-07)")
    void sendTwice() throws Exception {
        long orderId = createOrder(occupiedTable());
        addLine(orderId, item(Course.MAIN));
        mockMvc.perform(post("/api/orders/{id}/send", orderId).with(WAITER)).andExpect(status().isOk());

        mockMvc.perform(post("/api/orders/{id}/send", orderId).with(WAITER))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("There is nothing new to send"));
        assertEquals(1, ticketRepository.count());
    }

    @Test
    @DisplayName("TC-KIT-03 A Main cannot be sent while a Starter is unsent (BR-06)")
    void courseOrder() throws Exception {
        long orderId = createOrder(occupiedTable());
        addLine(orderId, item(Course.STARTER));
        long mainLine = addLine(orderId, item(Course.MAIN));

        mockMvc.perform(post("/api/orders/{id}/send", orderId).with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("lineIds", List.of(mainLine)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Send the Starter dishes before the Main dishes (BR-06)"));
    }

    @Test
    @DisplayName("TC-KIT-04 An out-of-stock dish stops the whole send; nothing is sent (BR-07, NFR-07)")
    void sendIsAtomic() throws Exception {
        long orderId = createOrder(occupiedTable());
        addLine(orderId, item(Course.STARTER));
        MenuItem main = item(Course.MAIN);
        addLine(orderId, main);
        main.markOutOfStock();

        mockMvc.perform(post("/api/orders/{id}/send", orderId).with(WAITER))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/orders/{id}", orderId).with(WAITER))
                .andExpect(jsonPath("$.lines[0].kitchenStatus").doesNotExist())
                .andExpect(jsonPath("$.lines[1].kitchenStatus").doesNotExist());
        assertEquals(0, ticketRepository.count());
    }

    @Test
    @DisplayName("TC-KIT-05 Kitchen moves an item New, In Progress, Ready; the waiter sees the status (FR-15)")
    void itemLifecycle() throws Exception {
        long orderId = createOrder(occupiedTable());
        addLine(orderId, item(Course.MAIN));
        long itemId = sendAndGetTicketItem(orderId);

        mockMvc.perform(post("/api/kitchen/items/{id}/start", itemId).with(KITCHEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mockMvc.perform(post("/api/kitchen/items/{id}/ready", itemId).with(KITCHEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));
        mockMvc.perform(get("/api/orders/{id}", orderId).with(WAITER))
                .andExpect(jsonPath("$.lines[0].kitchenStatus").value("READY"));
        mockMvc.perform(get("/api/kitchen/tickets").with(KITCHEN))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("TC-KIT-06 An item cannot skip In Progress")
    void cannotSkipInProgress() throws Exception {
        long orderId = createOrder(occupiedTable());
        addLine(orderId, item(Course.MAIN));
        long itemId = sendAndGetTicketItem(orderId);

        mockMvc.perform(post("/api/kitchen/items/{id}/ready", itemId).with(KITCHEN))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("TC-KIT-07 A dish sent back needs a reason")
    void sendBack() throws Exception {
        long orderId = createOrder(occupiedTable());
        addLine(orderId, item(Course.MAIN));
        long itemId = sendAndGetTicketItem(orderId);
        mockMvc.perform(post("/api/kitchen/items/{id}/start", itemId).with(KITCHEN));
        mockMvc.perform(post("/api/kitchen/items/{id}/ready", itemId).with(KITCHEN));

        mockMvc.perform(post("/api/kitchen/items/{id}/send-back", itemId).with(KITCHEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("reason", " "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.reason").value("Reason is required"));
        mockMvc.perform(post("/api/kitchen/items/{id}/send-back", itemId).with(KITCHEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("reason", "Too salty"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("TC-KIT-08 A dish in progress cannot be changed or cancelled (FR-18, BR-08)")
    void lockedAfterCookingStarts() throws Exception {
        long orderId = createOrder(occupiedTable());
        long lineId = addLine(orderId, item(Course.MAIN));
        long itemId = sendAndGetTicketItem(orderId);
        mockMvc.perform(post("/api/kitchen/items/{id}/start", itemId).with(KITCHEN));

        mockMvc.perform(delete("/api/orders/{id}/lines/{lineId}", orderId, lineId).with(WAITER))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/orders/{id}/lines/{lineId}", orderId, lineId).with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("quantity", 2))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("TC-KIT-09 A sent dish not yet started can be cancelled; it leaves the total")
    void cancelBeforeCooking() throws Exception {
        long orderId = createOrder(occupiedTable());
        long lineId = addLine(orderId, item(Course.MAIN));
        sendAndGetTicketItem(orderId);

        mockMvc.perform(delete("/api/orders/{id}/lines/{lineId}", orderId, lineId).with(WAITER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].kitchenStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    @DisplayName("TC-KIT-10 Only the kitchen changes item status; only front-of-house sends orders")
    void roles() throws Exception {
        long orderId = createOrder(occupiedTable());
        addLine(orderId, item(Course.MAIN));

        mockMvc.perform(post("/api/orders/{id}/send", orderId).with(KITCHEN))
                .andExpect(status().isForbidden());
        long itemId = sendAndGetTicketItem(orderId);
        mockMvc.perform(post("/api/kitchen/items/{id}/start", itemId).with(WAITER))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/kitchen/tickets").with(WAITER))
                .andExpect(status().isForbidden());
    }

    private DiningTable occupiedTable() {
        DiningTable table = tableRepository.findByActiveTrueOrderByNumberAsc().stream()
                .filter(t -> t.getStatus() == TableStatus.AVAILABLE)
                .findFirst().orElseThrow();
        table.changeStatus(TableStatus.OCCUPIED);
        return table;
    }

    private MenuItem item(Course course) {
        return itemRepository.search(false, true).stream()
                .filter(i -> i.getCourse() == course)
                .findFirst().orElseThrow();
    }

    private long createOrder(DiningTable table) throws Exception {
        String body = mockMvc.perform(post("/api/orders").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("tableId", table.getId()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long addLine(long orderId, MenuItem item) throws Exception {
        String body = mockMvc.perform(post("/api/orders/{id}/lines", orderId).with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("menuItemId", item.getId(), "quantity", 1))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.lines[-1].id")).longValue();
    }

    /** Sends every unsent line and returns the ticket item id of the first line. */
    private long sendAndGetTicketItem(long orderId) throws Exception {
        String body = mockMvc.perform(post("/api/orders/{id}/send", orderId).with(WAITER))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.lines[0].ticketItemId")).longValue();
    }
}
