package vn.edu.vlu.restaurant.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.vlu.restaurant.domain.CustomerOrder;
import vn.edu.vlu.restaurant.domain.DiningTable;
import vn.edu.vlu.restaurant.domain.MenuItem;
import vn.edu.vlu.restaurant.domain.OrderLine;
import vn.edu.vlu.restaurant.domain.OrderStatus;
import vn.edu.vlu.restaurant.domain.TableStatus;
import vn.edu.vlu.restaurant.domain.User;
import vn.edu.vlu.restaurant.dto.OrderLineRequest;
import vn.edu.vlu.restaurant.dto.OrderLineUpdateRequest;
import vn.edu.vlu.restaurant.dto.OrderResponse;
import vn.edu.vlu.restaurant.exception.BusinessRuleException;
import vn.edu.vlu.restaurant.exception.InvalidInputException;
import vn.edu.vlu.restaurant.exception.NotFoundException;
import vn.edu.vlu.restaurant.repository.CustomerOrderRepository;
import vn.edu.vlu.restaurant.repository.DiningTableRepository;
import vn.edu.vlu.restaurant.repository.MenuItemRepository;
import vn.edu.vlu.restaurant.repository.UserRepository;

/** Orders of the Waiter/Host (UC-08): open an order (FR-11) and edit its lines before sending (FR-12). */
@Service
@Transactional
public class OrderService {

    /** An order that is not paid yet; a table has at most one at a time. */
    private static final Set<OrderStatus> ACTIVE = EnumSet.of(OrderStatus.OPEN, OrderStatus.LOCKED);

    private final CustomerOrderRepository orderRepository;
    private final DiningTableRepository tableRepository;
    private final MenuItemRepository itemRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public OrderService(CustomerOrderRepository orderRepository, DiningTableRepository tableRepository,
                        MenuItemRepository itemRepository, UserRepository userRepository, Clock clock) {
        this.orderRepository = orderRepository;
        this.tableRepository = tableRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    /**
     * Opens an order for an Occupied table (FR-11). The table row is locked so that two waiters
     * pressing "New order" at the same time cannot create two orders for one table.
     */
    public OrderResponse createOrder(Long tableId, String username) {
        DiningTable table = tableRepository.findByIdForUpdate(tableId)
                .orElseThrow(() -> new InvalidInputException("tableId", "Table not found"));
        if (table.getStatus() != TableStatus.OCCUPIED) {
            throw new BusinessRuleException("Table " + table.getNumber()
                    + " is not occupied; seat the guests before taking an order");
        }
        if (orderRepository.existsByTableIdAndStatusIn(tableId, ACTIVE)) {
            throw new BusinessRuleException("Table " + table.getNumber() + " already has an open order");
        }
        User waiter = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
        CustomerOrder order = new CustomerOrder(table, waiter, LocalDateTime.now(clock));
        return OrderResponse.from(orderRepository.save(order));
    }

    /** The unpaid order of a table, for the order screen. */
    @Transactional(readOnly = true)
    public List<OrderResponse> listActiveOrders(Long tableId) {
        return orderRepository.findByTableIdAndStatusInOrderByCreatedAtAsc(tableId, ACTIVE).stream()
                .map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        return OrderResponse.from(findOrder(id));
    }

    /** Adds a dish; out-of-stock or hidden items are refused (BR-07), and so is a locked order (BR-08). */
    public OrderResponse addLine(Long orderId, OrderLineRequest request) {
        CustomerOrder order = findOrder(orderId);
        MenuItem item = itemRepository.findById(request.menuItemId())
                .orElseThrow(() -> new InvalidInputException("menuItemId", "Menu item not found"));
        order.addLine(item, request.quantity(), cleanNote(request.note()));
        orderRepository.flush();  // gives the new line its id for the response
        return OrderResponse.from(order);
    }

    public OrderResponse changeLine(Long orderId, Long lineId, OrderLineUpdateRequest request) {
        CustomerOrder order = findOrder(orderId);
        order.changeLine(findLine(order, lineId), request.quantity(), cleanNote(request.note()));
        return OrderResponse.from(order);
    }

    public OrderResponse removeLine(Long orderId, Long lineId) {
        CustomerOrder order = findOrder(orderId);
        order.removeLine(findLine(order, lineId));
        return OrderResponse.from(order);
    }

    private CustomerOrder findOrder(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new NotFoundException("Order not found"));
    }

    private OrderLine findLine(CustomerOrder order, Long lineId) {
        return order.findLine(lineId).orElseThrow(() -> new NotFoundException("Order line not found"));
    }

    /** A blank note is stored as no note. */
    private String cleanNote(String note) {
        return note == null || note.isBlank() ? null : note.trim();
    }
}
