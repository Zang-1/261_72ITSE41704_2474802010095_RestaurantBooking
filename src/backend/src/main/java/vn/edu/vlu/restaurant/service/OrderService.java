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

/**
 * Orders of the Waiter/Host (UC-08): open an order (FR-11), edit its lines before sending (FR-12)
 * and send them to the kitchen (FR-13, FR-14, FR-18).
 */
@Service
@Transactional
public class OrderService {

    /** An order that is not paid yet; a table has at most one at a time. */
    private static final Set<OrderStatus> ACTIVE = EnumSet.of(OrderStatus.OPEN, OrderStatus.LOCKED);

    private final CustomerOrderRepository orderRepository;
    private final DiningTableRepository tableRepository;
    private final MenuItemRepository itemRepository;
    private final UserRepository userRepository;
    private final TableService tableService;
    private final Clock clock;

    public OrderService(CustomerOrderRepository orderRepository, DiningTableRepository tableRepository,
                        MenuItemRepository itemRepository, UserRepository userRepository,
                        TableService tableService, Clock clock) {
        this.orderRepository = orderRepository;
        this.tableRepository = tableRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.tableService = tableService;
        this.clock = clock;
    }

    /**
     * Moves an open order to a free table (FR-19). The new table is taken with Giang's seatParty, so
     * BR-01 to BR-03 and BR-05 apply as for a walk-in; the old table then needs cleaning. Both table
     * rows are locked, lower id first, so two moves at the same time cannot deadlock.
     */
    public OrderResponse moveOrder(Long orderId, Long targetTableId, int partySize) {
        CustomerOrder order = findOrderForUpdate(orderId);
        DiningTable from = order.getTable();
        if (from.getId().equals(targetTableId)) {
            throw new InvalidInputException("tableId", "The order is already at table " + from.getNumber());
        }
        DiningTable to;
        if (from.getId() < targetTableId) {
            tableService.lockTable(from.getId());
            to = lockTarget(targetTableId);
        } else {
            to = lockTarget(targetTableId);
            tableService.lockTable(from.getId());
        }
        order.moveTo(to);
        tableService.seatParty(to, partySize);
        from.changeStatus(TableStatus.NEEDS_CLEANING);
        return OrderResponse.from(order);
    }

    private DiningTable lockTarget(Long tableId) {
        return tableRepository.findByIdForUpdate(tableId)
                .orElseThrow(() -> new InvalidInputException("tableId", "Table not found"));
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

    /** Adds a dish; out-of-stock or hidden items are refused (BR-07), and so is a locked order (BR-09). */
    public OrderResponse addLine(Long orderId, OrderLineRequest request) {
        CustomerOrder order = findOrderForUpdate(orderId);
        MenuItem item = itemRepository.findById(request.menuItemId())
                .orElseThrow(() -> new InvalidInputException("menuItemId", "Menu item not found"));
        order.addLine(item, request.quantity(), cleanNote(request.note()));
        orderRepository.flush();  // gives the new line its id for the response
        return OrderResponse.from(order);
    }

    public OrderResponse changeLine(Long orderId, Long lineId, OrderLineUpdateRequest request) {
        CustomerOrder order = findOrderForUpdate(orderId);
        order.changeLine(findLine(order, lineId), request.quantity(), cleanNote(request.note()));
        return OrderResponse.from(order);
    }

    /** An unsent line is deleted; a sent line is cancelled, which only works before cooking starts (BR-08). */
    public OrderResponse removeLine(Long orderId, Long lineId) {
        CustomerOrder order = findOrderForUpdate(orderId);
        OrderLine line = findLine(order, lineId);
        if (line.isSent()) {
            order.cancelLine(line, LocalDateTime.now(clock));
        } else {
            order.removeLine(line);
        }
        orderRepository.flush();  // a cook starting the dish at the same moment fails here, not after the response
        return OrderResponse.from(order);
    }

    /**
     * Sends dishes to the kitchen as one ticket (FR-13, FR-14). The order row is locked and the whole
     * send is one transaction: if anything fails nothing is saved, and a second click finds nothing
     * left to send, so no duplicate ticket is ever created (NFR-07).
     */
    public OrderResponse sendToKitchen(Long orderId, List<Long> lineIds) {
        CustomerOrder order = findOrderForUpdate(orderId);
        List<OrderLine> selection = lineIds == null || lineIds.isEmpty()
                ? order.unsentLines()
                : lineIds.stream().distinct()
                        .map(id -> order.findLine(id)
                                .orElseThrow(() -> new InvalidInputException("lineIds", "Order line not found")))
                        .toList();
        order.sendToKitchen(selection, LocalDateTime.now(clock));
        orderRepository.flush();  // gives the ticket items their ids for the response
        return OrderResponse.from(order);
    }

    private CustomerOrder findOrder(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new NotFoundException("Order not found"));
    }

    private CustomerOrder findOrderForUpdate(Long id) {
        return orderRepository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Order not found"));
    }

    private OrderLine findLine(CustomerOrder order, Long lineId) {
        return order.findLine(lineId).orElseThrow(() -> new NotFoundException("Order line not found"));
    }

    /** A blank note is stored as no note. */
    private String cleanNote(String note) {
        return note == null || note.isBlank() ? null : note.trim();
    }
}
