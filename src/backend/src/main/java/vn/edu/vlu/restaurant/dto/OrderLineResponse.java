package vn.edu.vlu.restaurant.dto;

import vn.edu.vlu.restaurant.domain.Course;
import vn.edu.vlu.restaurant.domain.OrderLine;

public record OrderLineResponse(
        Long id,
        Long menuItemId,
        String menuItemName,
        Course course,
        int quantity,
        long unitPrice,
        String note,
        long lineTotal) {

    public static OrderLineResponse from(OrderLine line) {
        return new OrderLineResponse(line.getId(), line.getMenuItem().getId(), line.getMenuItem().getName(),
                line.getMenuItem().getCourse(), line.getQuantity(), line.getUnitPrice(), line.getNote(),
                line.lineTotal());
    }
}
