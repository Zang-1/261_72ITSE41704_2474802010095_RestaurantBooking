package vn.edu.vlu.restaurant.dto;

import vn.edu.vlu.restaurant.domain.Course;
import vn.edu.vlu.restaurant.domain.ItemStatus;
import vn.edu.vlu.restaurant.domain.OrderLine;

/** ticketItemId and kitchenStatus are null while the line is not sent; the waiter sees the kitchen status (FR-15). */
public record OrderLineResponse(
        Long id,
        Long menuItemId,
        String menuItemName,
        Course course,
        int quantity,
        long unitPrice,
        String note,
        long lineTotal,
        Long ticketItemId,
        ItemStatus kitchenStatus) {

    public static OrderLineResponse from(OrderLine line) {
        return new OrderLineResponse(line.getId(), line.getMenuItem().getId(), line.getMenuItem().getName(),
                line.getMenuItem().getCourse(), line.getQuantity(), line.getUnitPrice(), line.getNote(),
                line.lineTotal(),
                line.isSent() ? line.getTicketItem().getId() : null,
                line.isSent() ? line.getTicketItem().getStatus() : null);
    }
}
