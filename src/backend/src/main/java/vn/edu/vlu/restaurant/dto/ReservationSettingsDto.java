package vn.edu.vlu.restaurant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import vn.edu.vlu.restaurant.domain.ReservationSettings;

/** Dining duration and turn-time used by BR-01 (UC-02 step 5). */
public record ReservationSettingsDto(
        @NotNull(message = "Dining duration is required")
        @Min(value = 30, message = "Dining duration must be between 30 and 300 minutes")
        @Max(value = 300, message = "Dining duration must be between 30 and 300 minutes")
        Integer diningMinutes,

        @NotNull(message = "Turn-time is required")
        @Min(value = 0, message = "Turn-time must be between 0 and 60 minutes")
        @Max(value = 60, message = "Turn-time must be between 0 and 60 minutes")
        Integer turnMinutes) {

    public static ReservationSettingsDto from(ReservationSettings settings) {
        return new ReservationSettingsDto(settings.getDiningMinutes(), settings.getTurnMinutes());
    }
}
