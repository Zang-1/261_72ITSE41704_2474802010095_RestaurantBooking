package vn.edu.vlu.restaurant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A dish sent back by the guest goes from Ready to In Progress again (UC-09 alternative 3a). */
public record SendBackRequest(
        @NotBlank(message = "Reason is required")
        @Size(max = 200, message = "Reason must be at most 200 characters")
        String reason) {
}
