package vn.edu.vlu.restaurant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Create or edit a table (FR-02). */
public record TableRequest(
        @NotNull(message = "Table number is required")
        @Min(value = 1, message = "Table number must be between 1 and 999")
        @Max(value = 999, message = "Table number must be between 1 and 999")
        Integer number,

        @NotNull(message = "Capacity is required")
        @Min(value = 1, message = "Capacity must be between 1 and 20")
        @Max(value = 20, message = "Capacity must be between 1 and 20")
        Integer capacity,

        @NotNull(message = "Section is required")
        Long sectionId) {
}
