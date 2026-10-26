package vn.edu.vlu.restaurant.dto;

import vn.edu.vlu.restaurant.domain.Course;
import vn.edu.vlu.restaurant.domain.MenuItem;

public record MenuItemResponse(
        Long id,
        String name,
        String description,
        long price,
        Course course,
        Long categoryId,
        String categoryName,
        boolean inStock,
        boolean active) {

    public static MenuItemResponse from(MenuItem item) {
        return new MenuItemResponse(item.getId(), item.getName(), item.getDescription(), item.getPrice(),
                item.getCourse(), item.getCategory().getId(), item.getCategory().getName(),
                item.isInStock(), item.isActive());
    }
}
