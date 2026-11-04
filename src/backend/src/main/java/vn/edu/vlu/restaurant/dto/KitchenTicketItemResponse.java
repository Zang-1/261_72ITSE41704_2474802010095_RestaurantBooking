package vn.edu.vlu.restaurant.dto;

import vn.edu.vlu.restaurant.domain.Course;
import vn.edu.vlu.restaurant.domain.ItemStatus;
import vn.edu.vlu.restaurant.domain.KitchenTicketItem;
import vn.edu.vlu.restaurant.domain.OrderLine;

public record KitchenTicketItemResponse(
        Long id,
        Long ticketId,
        String menuItemName,
        Course course,
        int quantity,
        String note,
        ItemStatus status) {

    public static KitchenTicketItemResponse from(KitchenTicketItem item) {
        OrderLine line = item.getOrderLine();
        return new KitchenTicketItemResponse(item.getId(), item.getTicket().getId(), line.getMenuItem().getName(),
                line.getMenuItem().getCourse(), line.getQuantity(), line.getNote(), item.getStatus());
    }
}
