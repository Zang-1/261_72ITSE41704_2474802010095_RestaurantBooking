package vn.edu.vlu.restaurant.dto;

import vn.edu.vlu.restaurant.domain.MenuCategory;

public record MenuCategoryResponse(Long id, String name) {

    public static MenuCategoryResponse from(MenuCategory category) {
        return new MenuCategoryResponse(category.getId(), category.getName());
    }
}
