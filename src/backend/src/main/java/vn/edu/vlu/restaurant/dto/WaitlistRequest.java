package vn.edu.vlu.restaurant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Add a walk-in party to the waiting list (FR-07). */
public record WaitlistRequest(
        @NotBlank(message = "Party name is required")
        @Size(max = 100, message = "Party name must be at most 100 characters")
        String partyName,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = ValidationPatterns.PHONE, message = ValidationPatterns.PHONE_MESSAGE)
        String phone,

        @NotNull(message = "Party size is required")
        @Min(value = 1, message = "Party size must be at least 1")
        @Max(value = 20, message = "Party size must be at most 20")
        Integer partySize) {
}
