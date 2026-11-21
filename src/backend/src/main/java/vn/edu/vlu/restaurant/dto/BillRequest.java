package vn.edu.vlu.restaurant.dto;

import jakarta.validation.constraints.NotNull;

/** Generate the bill of an order (FR-21). */
public record BillRequest(
        @NotNull(message = "Order is required")
        Long orderId) {
}
