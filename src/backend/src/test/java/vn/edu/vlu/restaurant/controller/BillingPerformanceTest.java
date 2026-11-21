package vn.edu.vlu.restaurant.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import vn.edu.vlu.restaurant.domain.Bill;
import vn.edu.vlu.restaurant.domain.CustomerOrder;
import vn.edu.vlu.restaurant.domain.DiningTable;
import vn.edu.vlu.restaurant.domain.ItemStatus;
import vn.edu.vlu.restaurant.domain.KitchenTicketItem;
import vn.edu.vlu.restaurant.domain.MenuItem;
import vn.edu.vlu.restaurant.domain.OrderLine;
import vn.edu.vlu.restaurant.domain.PaymentMethod;
import vn.edu.vlu.restaurant.domain.Shift;
import vn.edu.vlu.restaurant.domain.TableStatus;
import vn.edu.vlu.restaurant.domain.User;
import vn.edu.vlu.restaurant.repository.BillRepository;

/** NFR-04: a bill of 50 lines is shown within 2 seconds; a shift report of 300 bills within 3 seconds. */
class BillingPerformanceTest extends BillingTestSupport {

    @Autowired
    private BillRepository billRepository;

    @Test
    @DisplayName("TC-BIL-34 A bill with 50 lines is returned in under 2 seconds (NFR-04)")
    void fiftyLineBill() throws Exception {
        openShift();
        CustomerOrder order = cookedOrder(50);
        long billId = generateBill(order);

        long slowest = timeFiveCalls("/api/bills/" + billId, "$.lines.length()", 50);

        System.out.println("NFR-04 bill with 50 lines: slowest of 5 calls " + slowest + " ms");
        assertTrue(slowest < 2000, "slowest call took " + slowest + " ms");
    }

    @Test
    @DisplayName("TC-SHF-10 A shift report covering 300 bills is generated in under 3 seconds (NFR-04)")
    void reportOfThreeHundredBills() throws Exception {
        Shift shift = openShift();
        User waiter = staffUser("waiter");
        List<DiningTable> tables = tableRepository.findByActiveTrueOrderByNumberAsc();
        List<MenuItem> items = itemRepository.findAll().stream().filter(MenuItem::isOrderable).toList();
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        PaymentMethod[] methods = PaymentMethod.values();

        // 300 bills of 3 dishes each, paid with every method, every fifth one split evenly
        for (int i = 0; i < 300; i++) {
            DiningTable table = tables.get(i % tables.size());
            table.changeStatus(TableStatus.OCCUPIED);
            CustomerOrder order = new CustomerOrder(table, waiter, start);
            for (int d = 0; d < 3; d++) {
                order.addLine(items.get((i + d) % items.size()), 1 + d, null);
            }
            order.sendToKitchen(order.unsentLines(), start);
            for (OrderLine line : order.getLines()) {
                KitchenTicketItem item = line.getTicketItem();
                item.changeStatus(ItemStatus.IN_PROGRESS, null, start);
                item.changeStatus(ItemStatus.READY, null, start);
            }
            orderRepository.save(order);
            Bill bill = Bill.generate(order, start);
            if (i % 5 == 0) {
                bill.splitEvenly(2);
                bill.getShares().forEach(share -> bill.pay(share, PaymentMethod.CARD, share.getAmount(), shift, waiter, start));
            } else {
                bill.pay(null, methods[i % methods.length], bill.getTotal(), shift, waiter, start);
            }
            billRepository.save(bill);
            table.changeStatus(TableStatus.AVAILABLE);
            if (i % 50 == 49) {
                billRepository.flush();
            }
        }
        billRepository.flush();

        long slowest = timeFiveCalls("/api/shifts/" + shift.getId() + "/report", "$.closedBills", 300);

        System.out.println("NFR-04 shift report with 300 bills: slowest of 5 calls " + slowest + " ms");
        assertTrue(slowest < 3000, "slowest call took " + slowest + " ms");
    }

    /** The first call warms up the JIT and the query plans; the next five are timed. */
    private long timeFiveCalls(String url, String path, int expected) throws Exception {
        var role = url.contains("shifts") ? MANAGER : WAITER;
        mockMvc.perform(get(url).with(role)).andExpect(status().isOk()).andExpect(jsonPath(path).value(expected));
        long slowest = 0;
        for (int i = 0; i < 5; i++) {
            long begin = System.nanoTime();
            mockMvc.perform(get(url).with(role)).andExpect(status().isOk()).andExpect(jsonPath(path).value(expected));
            slowest = Math.max(slowest, (System.nanoTime() - begin) / 1_000_000);
        }
        return slowest;
    }
}
