package vn.edu.vlu.restaurant.dto;

import vn.edu.vlu.restaurant.domain.Role;

public record CurrentUserResponse(String username, Role role) {
}
