package vn.edu.vlu.restaurant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Seat a walk-in party directly at a free table (UC-05). */
public record WalkInRequest(
        @NotNull(message = "Party size is required")
        @Min(value = 1, message = "Party size must be at least 1")
        @Max(value = 20, message = "Party size must be at most 20")
        Integer partySize) {
}
