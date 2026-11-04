package vn.edu.vlu.restaurant.dto;

import java.time.LocalDateTime;
import java.util.List;
import vn.edu.vlu.restaurant.domain.KitchenTicket;

/** One ticket on the kitchen display; items are in course order (BR-06). */
public record KitchenTicketResponse(
        Long id,
        Long orderId,
        int tableNumber,
        LocalDateTime sentAt,
        List<KitchenTicketItemResponse> items) {

    public static KitchenTicketResponse from(KitchenTicket ticket) {
        return new KitchenTicketResponse(ticket.getId(), ticket.getOrder().getId(),
                ticket.getOrder().getTable().getNumber(), ticket.getSentAt(),
                ticket.getItems().stream().map(KitchenTicketItemResponse::from).toList());
    }
}
