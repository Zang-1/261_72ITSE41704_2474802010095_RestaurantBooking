package vn.edu.vlu.restaurant.exception;

import java.util.Map;

/**
 * Error body returned by every endpoint. fieldErrors maps a request field to its message so the
 * client can show it next to the field.
 */
public record ApiError(String message, Map<String, String> fieldErrors) {

    public static ApiError of(String message) {
        return new ApiError(message, Map.of());
    }
}
