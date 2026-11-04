package vn.edu.vlu.restaurant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Add a dish to an order (FR-12). */
public record OrderLineRequest(
        @NotNull(message = "Menu item is required")
        Long menuItemId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be between 1 and 50")
        @Max(value = 50, message = "Quantity must be between 1 and 50")
        Integer quantity,

        @Size(max = 200, message = "Note must be at most 200 characters")
        String note) {
}
