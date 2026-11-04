package vn.edu.vlu.restaurant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Change the quantity and note of a line that is not sent yet (FR-12). */
public record OrderLineUpdateRequest(
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be between 1 and 50")
        @Max(value = 50, message = "Quantity must be between 1 and 50")
        Integer quantity,

        @Size(max = 200, message = "Note must be at most 200 characters")
        String note) {
}
