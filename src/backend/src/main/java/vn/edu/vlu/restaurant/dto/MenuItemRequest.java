package vn.edu.vlu.restaurant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.edu.vlu.restaurant.domain.Course;

/** Create or edit a menu item (FR-08). The price is in VND. */
public record MenuItemRequest(
        @NotBlank(message = "Item name is required")
        @Size(max = 100, message = "Item name must be at most 100 characters")
        String name,

        @Size(max = 300, message = "Description must be at most 300 characters")
        String description,

        @NotNull(message = "Price is required")
        @Min(value = 1, message = "Price must be between 1 and 10,000,000 VND")
        @Max(value = 10_000_000, message = "Price must be between 1 and 10,000,000 VND")
        Long price,

        @NotNull(message = "Course is required")
        Course course,

        @NotNull(message = "Category is required")
        Long categoryId) {
}
