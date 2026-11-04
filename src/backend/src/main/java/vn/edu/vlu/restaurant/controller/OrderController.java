package vn.edu.vlu.restaurant.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.vlu.restaurant.dto.OrderLineRequest;
import vn.edu.vlu.restaurant.dto.OrderLineUpdateRequest;
import vn.edu.vlu.restaurant.dto.OrderRequest;
import vn.edu.vlu.restaurant.dto.OrderResponse;
import vn.edu.vlu.restaurant.service.OrderService;

/** UC-08 Create order: the Waiter/Host opens an order for a table and edits its lines (FR-11, FR-12). */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize(AccessRules.FRONT_OF_HOUSE)
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@Valid @RequestBody OrderRequest request, Authentication authentication) {
        return orderService.createOrder(request.tableId(), authentication.getName());
    }

    /** The unpaid order of a table; empty when the table has none yet. */
    @GetMapping
    @PreAuthorize(AccessRules.FRONT_OF_HOUSE)
    public List<OrderResponse> listActiveOrders(@RequestParam Long tableId) {
        return orderService.listActiveOrders(tableId);
    }

    @GetMapping("/{id}")
    @PreAuthorize(AccessRules.FRONT_OF_HOUSE)
    public OrderResponse getOrder(@PathVariable Long id) {
        return orderService.getOrder(id);
    }

    @PostMapping("/{id}/lines")
    @PreAuthorize(AccessRules.FRONT_OF_HOUSE)
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse addLine(@PathVariable Long id, @Valid @RequestBody OrderLineRequest request) {
        return orderService.addLine(id, request);
    }

    @PutMapping("/{id}/lines/{lineId}")
    @PreAuthorize(AccessRules.FRONT_OF_HOUSE)
    public OrderResponse changeLine(@PathVariable Long id, @PathVariable Long lineId,
                                    @Valid @RequestBody OrderLineUpdateRequest request) {
        return orderService.changeLine(id, lineId, request);
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    @PreAuthorize(AccessRules.FRONT_OF_HOUSE)
    public OrderResponse removeLine(@PathVariable Long id, @PathVariable Long lineId) {
        return orderService.removeLine(id, lineId);
    }
}
