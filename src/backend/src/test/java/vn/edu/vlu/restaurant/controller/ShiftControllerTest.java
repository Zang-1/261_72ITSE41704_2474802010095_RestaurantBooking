package vn.edu.vlu.restaurant.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import vn.edu.vlu.restaurant.domain.CustomerOrder;
import vn.edu.vlu.restaurant.domain.Shift;

/** UC-11 Open and close shift (FR-27, FR-29, BR-13) and UC-15 Generate shift report (FR-28). */
class ShiftControllerTest extends BillingTestSupport {

    @Test
    @DisplayName("TC-SHF-02 The Manager opens a shift; a second open shift is refused (FR-27, BR-13)")
    void opensShift() throws Exception {
        mockMvc.perform(post("/api/shifts").with(MANAGER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.open").value(true))
                .andExpect(jsonPath("$.openedBy").value("manager"));

        mockMvc.perform(post("/api/shifts").with(MANAGER))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(startsWith("A shift is already open since")));
    }

    @Test
    @DisplayName("TC-SHF-03 Only a Manager opens, closes and reports shifts; staff can see whether one is open (NFR-10)")
    void accessRules() throws Exception {
        Shift shift = openShift();

        mockMvc.perform(post("/api/shifts").with(WAITER)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/shifts/" + shift.getId() + "/close").with(WAITER)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/shifts/" + shift.getId() + "/report").with(WAITER)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/shifts").with(KITCHEN)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/shifts/current").with(WAITER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(shift.getId()));
    }

    @Test
    @DisplayName("TC-SHF-04 No shift open: current answers 204 and a payment is refused (BR-13)")
    void noShiftNoPayment() throws Exception {
        Shift shift = openShift();
        CustomerOrder order = cookedOrder(2);
        long billId = generateBill(order);
        shift.close(staffUser("manager"), LocalDateTime.now(), null);
        shiftRepository.saveAndFlush(shift);

        mockMvc.perform(get("/api/shifts/current").with(WAITER)).andExpect(status().isNoContent());
        mockMvc.perform(post("/api/bills/" + billId + "/payments").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("method", "CASH", "amount", order.total()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "No shift is open; ask the Manager to open a shift before taking payments (BR-13)"));
    }

    @Test
    @DisplayName("TC-SHF-05 A shift with an unpaid bill cannot be closed; the tables are listed (BR-13, UC-11 3a)")
    void unpaidBillBlocksClosing() throws Exception {
        Shift shift = openShift();
        CustomerOrder order = cookedOrder(2);
        generateBill(order);

        mockMvc.perform(post("/api/shifts/" + shift.getId() + "/close").with(MANAGER))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The shift cannot be closed: unpaid bills at table "
                        + order.getTable().getNumber() + " (BR-13)"));
        assertTrue(shiftRepository.findById(shift.getId()).orElseThrow().isOpen());
    }

    @Test
    @DisplayName("TC-SHF-06 Closing with counted cash; the report shows expected cash and the difference (FR-27, FR-29)")
    void closeWithCountedCash() throws Exception {
        Shift shift = openShift();
        CustomerOrder order = cookedOrder(2);
        long total = order.total();
        pay(generateBill(order), "CASH", total + 20_000);

        mockMvc.perform(post("/api/shifts/" + shift.getId() + "/close").with(MANAGER)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("countedCash", total - 10_000))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(false))
                .andExpect(jsonPath("$.closedBy").value("manager"))
                .andExpect(jsonPath("$.countedCash").value(total - 10_000));

        mockMvc.perform(get("/api/shifts/" + shift.getId() + "/report").with(MANAGER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provisional").value(false))
                .andExpect(jsonPath("$.expectedCash").value(total))
                .andExpect(jsonPath("$.countedCash").value(total - 10_000))
                .andExpect(jsonPath("$.cashDifference").value(-10_000));

        mockMvc.perform(post("/api/shifts/" + shift.getId() + "/close").with(MANAGER))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This shift is already closed"));
        mockMvc.perform(post("/api/shifts").with(MANAGER)).andExpect(status().isCreated());  // a new shift can open
    }

    @Test
    @DisplayName("TC-SHF-07 Counted cash cannot be negative (NFR-11)")
    void countedCashValidation() throws Exception {
        Shift shift = openShift();

        mockMvc.perform(post("/api/shifts/" + shift.getId() + "/close").with(MANAGER)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("countedCash", -1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.countedCash").value("Counted cash cannot be negative"));
    }

    @Test
    @DisplayName("TC-SHF-08 Shift report: closed bills, revenue, revenue per method, split bills (FR-28)")
    void shiftReport() throws Exception {
        Shift shift = openShift();
        CustomerOrder a = cookedOrder(2);
        CustomerOrder b = cookedOrder(3);
        CustomerOrder c = cookedOrder(1);
        pay(generateBill(a), "CASH", a.total() + 5_000);
        long billB = generateBill(b);
        String split = mockMvc.perform(post("/api/bills/" + billB + "/split/even").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("shares", 2))))
                .andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(split, "$.shares[*].id");
        List<Integer> amounts = JsonPath.read(split, "$.shares[*].amount");
        payShare(billB, "CARD", amounts.get(0), ids.get(0));
        payShare(billB, "E_WALLET", amounts.get(1), ids.get(1));
        generateBill(c);  // unpaid: not in the report

        mockMvc.perform(get("/api/shifts/" + shift.getId() + "/report").with(MANAGER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provisional").value(true))
                .andExpect(jsonPath("$.closedBills").value(2))
                .andExpect(jsonPath("$.splitBills").value(1))
                .andExpect(jsonPath("$.payments").value(3))
                .andExpect(jsonPath("$.revenue").value(a.total() + b.total()))
                .andExpect(jsonPath("$.revenueByMethod.CASH").value(a.total()))
                .andExpect(jsonPath("$.revenueByMethod.CARD").value(amounts.get(0)))
                .andExpect(jsonPath("$.revenueByMethod.E_WALLET").value(amounts.get(1)))
                .andExpect(jsonPath("$.revenueByMethod.BANK_TRANSFER").value(0))
                .andExpect(jsonPath("$.expectedCash").value(a.total()))
                .andExpect(jsonPath("$.cashDifference").doesNotExist())
                .andExpect(jsonPath("$.bills.length()").value(2))
                .andExpect(jsonPath("$.bills[1].splitType").value("EVEN"));
    }

    @Test
    @DisplayName("TC-SHF-09 A shift without bills reports zero revenue; an unknown shift gives 404 (UC-15 4a)")
    void emptyAndUnknownShift() throws Exception {
        Shift shift = openShift();

        mockMvc.perform(get("/api/shifts/" + shift.getId() + "/report").with(MANAGER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closedBills").value(0))
                .andExpect(jsonPath("$.revenue").value(0))
                .andExpect(jsonPath("$.revenueByMethod.CASH").value(0));
        mockMvc.perform(get("/api/shifts/999999/report").with(MANAGER)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/shifts").with(MANAGER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(shift.getId()));
    }

    private void payShare(long billId, String method, long amount, long shareId) throws Exception {
        mockMvc.perform(post("/api/bills/" + billId + "/payments").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("method", method, "amount", amount, "shareId", shareId))))
                .andExpect(status().isCreated());
    }
}
