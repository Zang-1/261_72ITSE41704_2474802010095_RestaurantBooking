package vn.edu.vlu.restaurant.dto;

import java.time.LocalDateTime;
import java.util.List;
import vn.edu.vlu.restaurant.domain.Bill;
import vn.edu.vlu.restaurant.domain.BillStatus;
import vn.edu.vlu.restaurant.domain.SplitType;

/** A bill with its lines, total, amount paid and amount remaining (FR-22). */
public record BillResponse(
        Long id,
        Long orderId,
        Long tableId,
        int tableNumber,
        BillStatus status,
        SplitType splitType,
        long total,
        long paid,
        long remaining,
        LocalDateTime createdAt,
        LocalDateTime closedAt,
        List<BillLineResponse> lines,
        List<BillShareResponse> shares,
        List<PaymentResponse> payments) {

    public static BillResponse from(Bill bill) {
        long paid = bill.paidAmount();
        return new BillResponse(bill.getId(), bill.getOrder().getId(), bill.getOrder().getTable().getId(),
                bill.getOrder().getTable().getNumber(), bill.getStatus(), bill.getSplitType(), bill.getTotal(), paid,
                bill.getTotal() - paid, bill.getCreatedAt(), bill.getClosedAt(),
                bill.getLines().stream().map(BillLineResponse::from).toList(),
                bill.getShares().stream().map(BillShareResponse::from).toList(),
                bill.getPayments().stream().map(PaymentResponse::from).toList());
    }
}
