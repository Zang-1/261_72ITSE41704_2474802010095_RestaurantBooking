package vn.edu.vlu.restaurant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Close the open shift (FR-27). countedCash is optional (FR-29, CR-05). */
public record CloseShiftRequest(
        @Min(value = 0, message = "Counted cash cannot be negative")
        @Max(value = 10_000_000_000L, message = "Counted cash is too large")
        Long countedCash) {
}
