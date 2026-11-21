package vn.edu.vlu.restaurant.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.vlu.restaurant.domain.Bill;
import vn.edu.vlu.restaurant.domain.BillShare;
import vn.edu.vlu.restaurant.domain.BillStatus;
import vn.edu.vlu.restaurant.domain.Payment;
import vn.edu.vlu.restaurant.domain.PaymentMethod;
import vn.edu.vlu.restaurant.domain.Shift;
import vn.edu.vlu.restaurant.domain.SplitType;
import vn.edu.vlu.restaurant.dto.ShiftReportResponse;
import vn.edu.vlu.restaurant.repository.BillRepository;
import vn.edu.vlu.restaurant.repository.CustomerOrderRepository;
import vn.edu.vlu.restaurant.repository.ShiftRepository;
import vn.edu.vlu.restaurant.service.ShiftService;

/** The sample bills and shifts follow the billing rules. Own in-memory database, like OrderSeederTest. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:billing-seed-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "app.seed.sample-orders=true"})
@ActiveProfiles("test")
@Transactional
class BillingSeederTest {

    @Autowired
    private BillRepository billRepository;
    @Autowired
    private ShiftRepository shiftRepository;
    @Autowired
    private CustomerOrderRepository orderRepository;
    @Autowired
    private ShiftService shiftService;

    @Test
    void everySampleOrderHasOneClosedBillPaidInFull() {
        List<Bill> bills = billRepository.findAll();
        assertEquals(orderRepository.count(), bills.size());
        for (Bill bill : bills) {
            assertEquals(BillStatus.CLOSED, bill.getStatus());
            assertEquals(0, bill.remaining());
            assertEquals(bill.getTotal(), bill.getOrder().total());
            if (bill.isSplit()) {
                assertEquals(bill.getTotal(), bill.getShares().stream().mapToLong(BillShare::getAmount).sum());
            }
            for (Payment payment : bill.getPayments()) {
                assertTrue(payment.getChangeAmount() == 0 || payment.getMethod() == PaymentMethod.CASH);
                assertEquals(bill.getShift(), payment.getShift());
            }
        }
        assertTrue(bills.stream().anyMatch(b -> b.getSplitType() == SplitType.EVEN));
        assertTrue(bills.stream().anyMatch(b -> b.getSplitType() == SplitType.BY_ITEM));
        assertTrue(bills.stream().flatMap(b -> b.getPayments().stream()).anyMatch(p -> p.getChangeAmount() > 0));
    }

    @Test
    void sampleShiftsAreClosedAndReportsAddUp() {
        List<Shift> shifts = shiftRepository.findAll();
        assertFalse(shifts.isEmpty());
        assertTrue(shiftRepository.findFirstByClosedAtIsNull().isEmpty());
        long revenue = 0;
        int shortShifts = 0;
        for (Shift shift : shifts) {
            ShiftReportResponse report = shiftService.report(shift.getId());
            assertEquals(report.revenue(), report.bills().stream().mapToLong(ShiftReportResponse.ReportBill::total).sum());
            revenue += report.revenue();
            if (report.cashDifference() != null && report.cashDifference() != 0) {
                shortShifts++;
            }
        }
        assertEquals(billRepository.findAll().stream().mapToLong(Bill::getTotal).sum(), revenue);
        assertEquals(1, shortShifts);
    }
}
