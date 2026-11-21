package vn.edu.vlu.restaurant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import vn.edu.vlu.restaurant.domain.PaymentMethod;

/** Record a payment (FR-25). shareId is required when the bill is split and must be empty otherwise. */
public record PaymentRequest(
        @NotNull(message = "Payment method is required")
        PaymentMethod method,

        @NotNull(message = "Amount is required")
        @Min(value = 1, message = "The amount must be greater than 0")
        @Max(value = 1_000_000_000, message = "The amount must be at most 1,000,000,000 VND")
        Long amount,

        Long shareId) {
}
