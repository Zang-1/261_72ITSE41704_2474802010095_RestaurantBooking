package vn.edu.vlu.restaurant.dto;

import jakarta.validation.constraints.NotNull;

/** Open an order for an Occupied table (FR-11). */
public record OrderRequest(
        @NotNull(message = "Table is required")
        Long tableId) {
}
