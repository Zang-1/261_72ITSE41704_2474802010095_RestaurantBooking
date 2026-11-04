package vn.edu.vlu.restaurant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Move an open order to another free table (FR-19). The party size is checked against the new table (BR-02). */
public record MoveOrderRequest(
        @NotNull(message = "Table is required")
        Long tableId,

        @NotNull(message = "Party size is required")
        @Min(value = 1, message = "Party size must be at least 1")
        @Max(value = 20, message = "Party size must be at most 20")
        Integer partySize) {
}
