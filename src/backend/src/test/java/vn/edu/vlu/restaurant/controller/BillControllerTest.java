package vn.edu.vlu.restaurant.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import vn.edu.vlu.restaurant.domain.CustomerOrder;
import vn.edu.vlu.restaurant.domain.OrderStatus;
import vn.edu.vlu.restaurant.domain.TableStatus;

/** UC-12 Generate bill, UC-13 Split bill, UC-14 Record payment and close bill, through the REST API. */
class BillControllerTest extends BillingTestSupport {

    @Test
    @DisplayName("TC-BIL-21 A Waiter/Host generates the bill of a finished order (FR-21, FR-22)")
    void generateBill() throws Exception {
        openShift();
        CustomerOrder order = cookedOrder(3);
        long total = order.total();

        mockMvc.perform(post("/api/bills").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("orderId", order.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.tableNumber").value(order.getTable().getNumber()))
                .andExpect(jsonPath("$.lines.length()").value(3))
                .andExpect(jsonPath("$.lines[0].itemName").value(order.getLines().get(0).getMenuItem().getName()))
                .andExpect(jsonPath("$.total").value(total))
                .andExpect(jsonPath("$.paid").value(0))
                .andExpect(jsonPath("$.remaining").value(total))
                .andExpect(jsonPath("$.splitType").value("NONE"));

        assertEquals(OrderStatus.LOCKED, orderRepository.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("TC-BIL-22 No bill while the kitchen is still preparing a dish (BR-09)")
    void kitchenMustBeDone() throws Exception {
        openShift();
        CustomerOrder order = orderWithSentDishes(2);

        mockMvc.perform(post("/api/bills").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("orderId", order.getId()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("The kitchen is still preparing")));
    }

    @Test
    @DisplayName("TC-BIL-23 No bill while no shift is open (UC-12 precondition, BR-13)")
    void billNeedsAnOpenShift() throws Exception {
        CustomerOrder order = cookedOrder(2);

        mockMvc.perform(post("/api/bills").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("orderId", order.getId()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No shift is open; ask the Manager to open a shift first (BR-13)"));
    }

    @Test
    @DisplayName("TC-BIL-24 One bill per order; Kitchen Staff cannot bill; unknown order and bill")
    void oneBillPerOrderAndAccess() throws Exception {
        openShift();
        CustomerOrder order = cookedOrder(2);
        generateBill(order);

        mockMvc.perform(post("/api/bills").with(WAITER).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("orderId", order.getId()))))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/bills").with(KITCHEN).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("orderId", order.getId()))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/bills").with(WAITER).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("orderId", 999_999))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.orderId").value("Order not found"));
        mockMvc.perform(post("/api/bills").with(WAITER).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.orderId").value("Order is required"));
        mockMvc.perform(get("/api/bills/999999").with(WAITER))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TC-BIL-25 The bill screen finds the unpaid bill of a table")
    void openBillOfTable() throws Exception {
        openShift();
        CustomerOrder order = cookedOrder(2);
        long billId = generateBill(order);

        mockMvc.perform(get("/api/bills").param("tableId", order.getTable().getId().toString()).with(WAITER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(billId));
    }

    @Test
    @DisplayName("TC-BIL-26 Even split through the API; 1 or 21 shares are rejected next to the field (FR-23, BR-10)")
    void splitEvenly() throws Exception {
        openShift();
        CustomerOrder order = cookedOrder(3);
        long billId = generateBill(order);
        long total = order.total();
        long each = total / 3 / 1000 * 1000;

        mockMvc.perform(post("/api/bills/" + billId + "/split/even").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("shares", 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.splitType").value("EVEN"))
                .andExpect(jsonPath("$.shares.length()").value(3))
                .andExpect(jsonPath("$.shares[0].amount").value(total - 2 * each))
                .andExpect(jsonPath("$.shares[1].amount").value(each))
                .andExpect(jsonPath("$.shares[2].remaining").value(each));

        // splitting again replaces the shares
        mockMvc.perform(post("/api/bills/" + billId + "/split/even").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("shares", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shares.length()").value(2));

        for (int shares : new int[] {1, 21}) {
            mockMvc.perform(post("/api/bills/" + billId + "/split/even").with(WAITER)
                            .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("shares", shares))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.shares").value("The number of shares must be between 2 and 20"));
        }

        mockMvc.perform(delete("/api/bills/" + billId + "/split").with(WAITER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.splitType").value("NONE"))
                .andExpect(jsonPath("$.shares").isEmpty());
    }

    @Test
    @DisplayName("TC-BIL-27 Split by item: every dish in exactly one share (FR-24)")
    void splitByItem() throws Exception {
        openShift();
        CustomerOrder order = cookedOrder(3);
        long billId = generateBill(order);
        String bill = mockMvc.perform(get("/api/bills/" + billId).with(WAITER)).andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(bill, "$.lines[*].id");
        List<Integer> totals = JsonPath.read(bill, "$.lines[*].lineTotal");

        mockMvc.perform(post("/api/bills/" + billId + "/split/by-item").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("shares", List.of(List.of(ids.get(0), ids.get(2)), List.of(ids.get(1)))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.splitType").value("BY_ITEM"))
                .andExpect(jsonPath("$.shares[0].amount").value(totals.get(0) + totals.get(2)))
                .andExpect(jsonPath("$.shares[1].amount").value(totals.get(1)))
                .andExpect(jsonPath("$.lines[1].shareNumber").value(2));

        mockMvc.perform(post("/api/bills/" + billId + "/split/by-item").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("shares", List.of(List.of(ids.get(0)), List.of(ids.get(1)))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.shares").value(startsWith("Every dish must be in a share")));
    }

    @Test
    @DisplayName("TC-BIL-28 Cash payment with change closes the bill; order closed, table Needs cleaning (FR-25, FR-26)")
    void cashPaymentClosesBill() throws Exception {
        openShift();
        CustomerOrder order = cookedOrder(2);
        long billId = generateBill(order);
        long total = order.total();

        mockMvc.perform(post("/api/bills/" + billId + "/payments").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("method", "CASH", "amount", total + 50_000))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.remaining").value(0))
                .andExpect(jsonPath("$.payments[0].change").value(50_000))
                .andExpect(jsonPath("$.payments[0].applied").value(total))
                .andExpect(jsonPath("$.payments[0].recordedBy").value("waiter"));

        assertEquals(OrderStatus.CLOSED, orderRepository.findById(order.getId()).orElseThrow().getStatus());
        assertEquals(TableStatus.NEEDS_CLEANING, tableRepository.findById(order.getTable().getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("TC-BIL-29 A card payment larger than the remaining amount is rejected (BR-11)")
    void cardCannotOverpay() throws Exception {
        openShift();
        CustomerOrder order = cookedOrder(2);
        long billId = generateBill(order);

        mockMvc.perform(post("/api/bills/" + billId + "/payments").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("method", "CARD", "amount", order.total() + 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").value(startsWith("A card payment cannot be more than")));
        mockMvc.perform(post("/api/bills/" + billId + "/payments").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("method", "CARD", "amount", 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").value("The amount must be greater than 0"));
        mockMvc.perform(post("/api/bills/" + billId + "/payments").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"CHEQUE\",\"amount\":1000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-BIL-30 Partial payments by transfer and e-wallet, then a closed bill takes no more (UC-14 5a, CR-04, BR-12)")
    void partialThenClosed() throws Exception {
        openShift();
        CustomerOrder order = cookedOrder(3);
        long billId = generateBill(order);
        long total = order.total();

        String first = pay(billId, "BANK_TRANSFER", 50_000);
        assertEquals("OPEN", JsonPath.read(first, "$.status"));
        assertEquals(total - 50_000, ((Number) JsonPath.read(first, "$.remaining")).longValue());

        String second = pay(billId, "E_WALLET", total - 50_000);
        assertEquals("CLOSED", JsonPath.read(second, "$.status"));

        mockMvc.perform(post("/api/bills/" + billId + "/payments").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("method", "CASH", "amount", 10_000))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This bill is closed and can no longer be changed (BR-12)"));
    }

    @Test
    @DisplayName("TC-BIL-31 Paying a split bill share by share; the share must be named (FR-25)")
    void payShares() throws Exception {
        openShift();
        CustomerOrder order = cookedOrder(2);
        long billId = generateBill(order);
        String split = mockMvc.perform(post("/api/bills/" + billId + "/split/even").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("shares", 2))))
                .andReturn().getResponse().getContentAsString();
        List<Integer> shareIds = JsonPath.read(split, "$.shares[*].id");
        List<Integer> amounts = JsonPath.read(split, "$.shares[*].amount");

        mockMvc.perform(post("/api/bills/" + billId + "/payments").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("method", "CARD", "amount", 1_000))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.shareId").value("Choose which share is being paid"));

        mockMvc.perform(post("/api/bills/" + billId + "/payments").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("method", "CARD", "amount", amounts.get(0), "shareId", shareIds.get(0)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.shares[0].remaining").value(0))
                .andExpect(jsonPath("$.payments[0].shareNumber").value(1));

        mockMvc.perform(post("/api/bills/" + billId + "/payments").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("method", "CASH", "amount", amounts.get(1), "shareId", shareIds.get(1)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    @DisplayName("TC-BIL-32 Cancel an unpaid bill so the guests can order more; refused after a payment (BR-09)")
    void cancelBill() throws Exception {
        openShift();
        CustomerOrder order = cookedOrder(2);
        long billId = generateBill(order);

        mockMvc.perform(post("/api/bills/" + billId + "/cancel").with(WAITER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertEquals(OrderStatus.OPEN, orderRepository.findById(order.getId()).orElseThrow().getStatus());

        long again = generateBill(order);  // a new bill can be generated after the cancelled one
        pay(again, "CARD", 1_000);
        mockMvc.perform(post("/api/bills/" + again + "/cancel").with(WAITER))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A payment is already recorded, so the bill cannot be cancelled"));
    }

    @Test
    @DisplayName("TC-BIL-33 Guests cannot leave (Needs cleaning) while their bill is unpaid (BR-05, UC-06 1a)")
    void tableWithUnpaidBillCannotBeCleaned() throws Exception {
        openShift();
        CustomerOrder order = cookedOrder(2);
        generateBill(order);

        mockMvc.perform(post("/api/tables/" + order.getTable().getId() + "/needs-cleaning").with(WAITER))
                .andExpect(status().isConflict());
    }
}
