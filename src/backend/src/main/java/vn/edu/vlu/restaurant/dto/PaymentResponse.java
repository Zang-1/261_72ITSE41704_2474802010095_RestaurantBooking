package vn.edu.vlu.restaurant.dto;

import java.time.LocalDateTime;
import vn.edu.vlu.restaurant.domain.Payment;
import vn.edu.vlu.restaurant.domain.PaymentMethod;

/** amount is what the guest handed over, change what was given back, applied what pays the bill. */
public record PaymentResponse(Long id, PaymentMethod method, long amount, long change, long applied,
                              Integer shareNumber, LocalDateTime paidAt, String recordedBy, Long shiftId) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getMethod(), payment.getAmount(), payment.getChangeAmount(),
                payment.appliedAmount(), payment.getShare() == null ? null : payment.getShare().getNumber(),
                payment.getPaidAt(), payment.getRecordedBy().getUsername(), payment.getShift().getId());
    }
}
