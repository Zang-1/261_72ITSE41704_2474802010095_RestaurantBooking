package vn.edu.vlu.restaurant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SectionRequest(
        @NotBlank(message = "Section name is required")
        @Size(max = 50, message = "Section name must be at most 50 characters")
        String name) {
}
