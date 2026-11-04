package vn.edu.vlu.restaurant.dto;

import java.util.List;

/**
 * Send dishes to the kitchen (FR-13, FR-14). Without lineIds every unsent line is sent; with lineIds
 * only those lines, e.g. the starters first (BR-06).
 */
public record SendOrderRequest(List<Long> lineIds) {
}
