package vn.edu.vlu.restaurant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/** Create or modify a reservation (FR-04, FR-05). Checks that need the database are in ReservationService. */
public record ReservationRequest(
        @NotNull(message = "Choose a table")
        Long tableId,

        @NotBlank(message = "Guest name is required")
        @Size(max = 100, message = "Guest name must be at most 100 characters")
        String guestName,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = ValidationPatterns.PHONE, message = ValidationPatterns.PHONE_MESSAGE)
        String phone,

        @NotNull(message = "Party size is required")
        @Min(value = 1, message = "Party size must be at least 1")
        @Max(value = 20, message = "Party size must be at most 20")
        Integer partySize,

        @NotNull(message = "Date is required")
        LocalDate date,

        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @Size(max = 200, message = "Note must be at most 200 characters")
        String note) {
}
