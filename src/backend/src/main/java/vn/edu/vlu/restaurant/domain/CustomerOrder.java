package vn.edu.vlu.restaurant.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import vn.edu.vlu.restaurant.exception.BusinessRuleException;

/**
 * The order of one table (FR-09). Named CustomerOrder because ORDER is a reserved word in SQL; the
 * class diagram calls it Order. Lines can be added, changed and sent only while the order is OPEN;
 * a sent line follows its kitchen ticket item (BR-08).
 */
@Entity
@Table(name = "customer_order")
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id", nullable = false)
    private DiningTable table;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "waiter_id", nullable = false)
    private User waiter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.OPEN;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @OrderBy("sentAt ASC, id ASC")
    private List<KitchenTicket> tickets = new ArrayList<>();

    protected CustomerOrder() {
    }

    public CustomerOrder(DiningTable table, User waiter, LocalDateTime createdAt) {
        this.table = table;
        this.waiter = waiter;
        this.createdAt = createdAt;
    }

    /**
     * Adds a dish. Refused when the bill is generated (BR-09) or the dish is out of stock (BR-07).
     * Dishes can still be added after cooking started; they go on a new ticket (BR-08).
     */
    public OrderLine addLine(MenuItem item, int quantity, String note) {
        requireOpen();
        if (!item.isOrderable()) {
            throw new BusinessRuleException("\"" + item.getName() + "\" is not available right now");
        }
        OrderLine line = new OrderLine(this, item, quantity, note);
        lines.add(line);
        return line;
    }

    /** Changes the quantity and note of a line that is not sent yet (FR-12). */
    public void changeLine(OrderLine line, int quantity, String note) {
        requireOpen();
        requireOwnLine(line);
        requireUnsent(line, "changed");
        line.change(quantity, note);
    }

    /** Removes a line that is not sent yet. A sent line is cancelled instead, see {@link #cancelLine}. */
    public void removeLine(OrderLine line) {
        requireOpen();
        requireOwnLine(line);
        requireUnsent(line, "removed");
        lines.remove(line);
    }

    /** Cancels a sent dish the kitchen has not started; once cooking starts it is locked (FR-18, BR-08). */
    public void cancelLine(OrderLine line, LocalDateTime at) {
        requireOpen();
        requireOwnLine(line);
        if (!line.isSent()) {
            throw new BusinessRuleException("\"" + line.getMenuItem().getName() + "\" is not sent yet; remove it instead");
        }
        if (line.getTicketItem().getStatus() != ItemStatus.NEW) {
            throw new BusinessRuleException(lockedMessage(line, "cancelled"));
        }
        line.getTicketItem().changeStatus(ItemStatus.CANCELLED, null, at);
    }

    /**
     * Sends the chosen unsent lines to the kitchen as one new ticket (FR-13, FR-14). Every check runs
     * before anything changes, so a refused send leaves the order exactly as it was (NFR-07):
     * the lines must be unsent (no line is ever sent twice), in stock (BR-07), and no unsent line
     * of an earlier course may be left behind (BR-06).
     */
    public KitchenTicket sendToKitchen(List<OrderLine> selection, LocalDateTime at) {
        requireOpen();
        if (selection.isEmpty()) {
            throw new BusinessRuleException("There is nothing new to send");
        }
        for (OrderLine line : selection) {
            requireOwnLine(line);
            if (line.isSent()) {
                throw new BusinessRuleException("\"" + line.getMenuItem().getName() + "\" was already sent");
            }
            if (!line.getMenuItem().isOrderable()) {
                throw new BusinessRuleException("\"" + line.getMenuItem().getName()
                        + "\" ran out; remove it before sending");
            }
        }
        Course latestSent = selection.stream().map(line -> line.getMenuItem().getCourse())
                .max(Comparator.naturalOrder()).orElseThrow();
        unsentLines().stream()
                .filter(line -> !selection.contains(line))
                .map(line -> line.getMenuItem().getCourse())
                .filter(course -> course.compareTo(latestSent) < 0)
                .min(Comparator.naturalOrder())
                .ifPresent(earlier -> {
                    throw new BusinessRuleException("Send the " + courseLabel(earlier) + " dishes before the "
                            + courseLabel(latestSent) + " dishes (BR-06)");
                });
        KitchenTicket ticket = new KitchenTicket(this, selection, at);
        tickets.add(ticket);
        return ticket;
    }

    public List<OrderLine> unsentLines() {
        return lines.stream().filter(line -> !line.isSent()).toList();
    }

    /** True while a sent dish is New or In Progress; Billing generates a bill only when this is false (BR-09). */
    public boolean hasKitchenWorkPending() {
        return lines.stream().anyMatch(line -> line.isSent() && line.getTicketItem().isPending());
    }

    public Optional<OrderLine> findLine(Long lineId) {
        return lines.stream().filter(line -> lineId.equals(line.getId())).findFirst();
    }

    /**
     * The guests move to another table (FR-19). Only an open order moves; the table statuses are
     * changed by the caller through {@link DiningTable#changeStatus}.
     */
    public void moveTo(DiningTable target) {
        requireOpen();
        this.table = target;
    }

    /** The bill is generated: no more changes until the bill is cancelled (BR-09). */
    public void lock() {
        if (status == OrderStatus.OPEN) {
            status = OrderStatus.LOCKED;
        }
    }

    public void close() {
        status = OrderStatus.CLOSED;
    }

    public boolean isEditable() {
        return status == OrderStatus.OPEN;
    }

    public long total() {
        return lines.stream().mapToLong(OrderLine::lineTotal).sum();
    }

    private void requireOpen() {
        if (!isEditable()) {
            throw new BusinessRuleException("This order is locked and can no longer be changed");
        }
    }

    private void requireOwnLine(OrderLine line) {
        if (!lines.contains(line)) {
            throw new BusinessRuleException("The line does not belong to this order");
        }
    }

    private void requireUnsent(OrderLine line, String action) {
        if (!line.isSent()) {
            return;
        }
        if (line.getTicketItem().getStatus() == ItemStatus.NEW) {
            throw new BusinessRuleException("\"" + line.getMenuItem().getName()
                    + "\" was already sent to the kitchen; cancel it and add it again");
        }
        throw new BusinessRuleException(lockedMessage(line, action));
    }

    private static String lockedMessage(OrderLine line, String action) {
        return line.isCancelled()
                ? "\"" + line.getMenuItem().getName() + "\" is already cancelled"
                : "\"" + line.getMenuItem().getName() + "\" is already being cooked and cannot be " + action + " (BR-08)";
    }

    private static String courseLabel(Course course) {
        return switch (course) {
            case STARTER -> "Starter";
            case MAIN -> "Main";
            case DESSERT -> "Dessert";
        };
    }

    public Long getId() {
        return id;
    }

    public DiningTable getTable() {
        return table;
    }

    public User getWaiter() {
        return waiter;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public List<KitchenTicket> getTickets() {
        return Collections.unmodifiableList(tickets);
    }
}
