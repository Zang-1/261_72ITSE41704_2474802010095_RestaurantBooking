package vn.edu.vlu.restaurant.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import vn.edu.vlu.restaurant.domain.DiningTable;
import vn.edu.vlu.restaurant.domain.TableStatus;
import vn.edu.vlu.restaurant.repository.CustomerOrderRepository;
import vn.edu.vlu.restaurant.repository.DiningTableRepository;
import vn.edu.vlu.restaurant.repository.MenuItemRepository;

/** UC-08 extension: move an open order to another table (FR-19). */
class OrderMoveControllerTest extends ApiTestSupport {

    private static final RequestPostProcessor WAITER = user("waiter").roles("WAITER_HOST");
    private static final RequestPostProcessor KITCHEN = user("kitchen").roles("KITCHEN_STAFF");

    @Autowired
    private DiningTableRepository tableRepository;

    @Autowired
    private MenuItemRepository itemRepository;

    @Autowired
    private CustomerOrderRepository orderRepository;

    @Test
    @DisplayName("TC-ORD-14 An order moves to a free table; the old table needs cleaning and the kitchen sees the new table")
    void moveOrder() throws Exception {
        List<DiningTable> free = availableTables();
        DiningTable from = free.get(0);
        DiningTable to = free.get(1);
        from.changeStatus(TableStatus.OCCUPIED);
        long orderId = createOrder(from);
        addLine(orderId);
        mockMvc.perform(post("/api/orders/{id}/send", orderId).with(WAITER)).andExpect(status().isOk());

        mockMvc.perform(post("/api/orders/{id}/move", orderId).with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("tableId", to.getId(), "partySize", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tableId").value(to.getId()))
                .andExpect(jsonPath("$.tableNumber").value(to.getNumber()));

        assertEquals(TableStatus.OCCUPIED, to.getStatus());
        assertEquals(TableStatus.NEEDS_CLEANING, from.getStatus());
        mockMvc.perform(get("/api/kitchen/tickets").with(KITCHEN))
                .andExpect(jsonPath("$[0].tableNumber").value(to.getNumber()));
    }

    @Test
    @DisplayName("TC-ORD-15 An order cannot move to a table that is not free")
    void targetMustBeAvailable() throws Exception {
        List<DiningTable> free = availableTables();
        DiningTable from = free.get(0);
        DiningTable to = free.get(1);
        from.changeStatus(TableStatus.OCCUPIED);
        to.changeStatus(TableStatus.OCCUPIED);
        long orderId = createOrder(from);

        mockMvc.perform(post("/api/orders/{id}/move", orderId).with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("tableId", to.getId(), "partySize", 2))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Table " + to.getNumber() + " is not available"));
        assertEquals(TableStatus.OCCUPIED, from.getStatus());
    }

    @Test
    @DisplayName("TC-ORD-16 The party must fit the new table (BR-02)")
    void partyMustFit() throws Exception {
        List<DiningTable> free = availableTables();
        DiningTable from = free.get(0);
        DiningTable to = free.get(1);
        from.changeStatus(TableStatus.OCCUPIED);
        long orderId = createOrder(from);

        mockMvc.perform(post("/api/orders/{id}/move", orderId).with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("tableId", to.getId(), "partySize", to.getCapacity() + 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.partySize").exists());
        assertEquals(TableStatus.AVAILABLE, to.getStatus());
    }

    @Test
    @DisplayName("TC-ORD-17 A locked order cannot move (BR-09)")
    void lockedOrderCannotMove() throws Exception {
        List<DiningTable> free = availableTables();
        DiningTable from = free.get(0);
        DiningTable to = free.get(1);
        from.changeStatus(TableStatus.OCCUPIED);
        long orderId = createOrder(from);
        orderRepository.findById(orderId).orElseThrow().lock();

        mockMvc.perform(post("/api/orders/{id}/move", orderId).with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("tableId", to.getId(), "partySize", 2))))
                .andExpect(status().isConflict());
        assertEquals(TableStatus.AVAILABLE, to.getStatus());
    }

    @Test
    @DisplayName("TC-ORD-18 Moving to the same table or without a party size is rejected")
    void invalidInput() throws Exception {
        DiningTable from = availableTables().get(0);
        from.changeStatus(TableStatus.OCCUPIED);
        long orderId = createOrder(from);

        mockMvc.perform(post("/api/orders/{id}/move", orderId).with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("tableId", from.getId(), "partySize", 2))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.tableId").value("The order is already at table " + from.getNumber()));
        mockMvc.perform(post("/api/orders/{id}/move", orderId).with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("tableId", from.getId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.partySize").value("Party size is required"));
    }

    /** Free tables with at least 2 seats, so a party of 2 fits each of them. */
    private List<DiningTable> availableTables() {
        return tableRepository.findByActiveTrueOrderByNumberAsc().stream()
                .filter(t -> t.getStatus() == TableStatus.AVAILABLE && t.getCapacity() >= 2)
                .toList();
    }

    private long createOrder(DiningTable table) throws Exception {
        String body = mockMvc.perform(post("/api/orders").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("tableId", table.getId()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private void addLine(long orderId) throws Exception {
        mockMvc.perform(post("/api/orders/{id}/lines", orderId).with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("menuItemId", itemRepository.search(false, true).get(0).getId(),
                                "quantity", 1))))
                .andExpect(status().isCreated());
    }
}
