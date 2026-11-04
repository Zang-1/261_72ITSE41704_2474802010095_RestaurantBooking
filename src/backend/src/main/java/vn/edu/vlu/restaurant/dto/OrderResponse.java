package vn.edu.vlu.restaurant.dto;

import java.time.LocalDateTime;
import java.util.List;
import vn.edu.vlu.restaurant.domain.CustomerOrder;
import vn.edu.vlu.restaurant.domain.OrderStatus;

public record OrderResponse(
        Long id,
        Long tableId,
        int tableNumber,
        String waiter,
        OrderStatus status,
        LocalDateTime createdAt,
        List<OrderLineResponse> lines,
        long total) {

    public static OrderResponse from(CustomerOrder order) {
        return new OrderResponse(order.getId(), order.getTable().getId(), order.getTable().getNumber(),
                order.getWaiter().getUsername(), order.getStatus(), order.getCreatedAt(),
                order.getLines().stream().map(OrderLineResponse::from).toList(), order.total());
    }
}
