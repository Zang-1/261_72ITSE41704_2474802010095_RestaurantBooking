package vn.edu.vlu.restaurant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Split a bill evenly (FR-23, BR-10). */
public record EvenSplitRequest(
        @NotNull(message = "Number of shares is required")
        @Min(value = 2, message = "The number of shares must be between 2 and 20")
        @Max(value = 20, message = "The number of shares must be between 2 and 20")
        Integer shares) {
}
