package vn.edu.vlu.restaurant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

/** Query parameters of the availability search (FR-03). */
public record AvailabilityQuery(
        @NotNull(message = "Date is required")
        LocalDate date,

        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @NotNull(message = "Party size is required")
        @Min(value = 1, message = "Party size must be at least 1")
        @Max(value = 20, message = "Party size must be at most 20")
        Integer partySize) {
}
