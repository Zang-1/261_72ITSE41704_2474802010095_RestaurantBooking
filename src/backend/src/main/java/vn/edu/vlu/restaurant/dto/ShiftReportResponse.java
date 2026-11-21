package vn.edu.vlu.restaurant.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import vn.edu.vlu.restaurant.domain.PaymentMethod;
import vn.edu.vlu.restaurant.domain.SplitType;

/**
 * Shift report (FR-28): number of closed bills, revenue, revenue per payment method and number of split
 * bills. provisional is true while the shift is still open (UC-15 alternative 2a). The cash figures are
 * FR-29: expectedCash is the cash taken, countedCash what the Manager counted, cashDifference = counted - expected.
 */
public record ShiftReportResponse(
        ShiftResponse shift,
        boolean provisional,
        int closedBills,
        long revenue,
        Map<PaymentMethod, Long> revenueByMethod,
        int payments,
        int splitBills,
        long expectedCash,
        Long countedCash,
        Long cashDifference,
        List<ReportBill> bills) {

    public record ReportBill(Long id, int tableNumber, long total, SplitType splitType, LocalDateTime closedAt) {
    }
}
