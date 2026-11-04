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
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import vn.edu.vlu.restaurant.exception.BusinessRuleException;

/**
 * One dish on a kitchen ticket. It reads the quantity and note from its order line, which can no
 * longer change once the line is sent. Every status change is kept as an ItemStatusChange.
 */
@Entity
@Table(name = "kitchen_ticket_item")
public class KitchenTicketItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private KitchenTicket ticket;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_line_id", nullable = false, unique = true)
    private OrderLine orderLine;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemStatus status = ItemStatus.NEW;

    /** Optimistic lock: a cook and a waiter changing the same dish at once cannot both succeed. */
    @Version
    private long version;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<ItemStatusChange> history = new ArrayList<>();

    protected KitchenTicketItem() {
    }

    KitchenTicketItem(KitchenTicket ticket, OrderLine orderLine, LocalDateTime sentAt) {
        this.ticket = ticket;
        this.orderLine = orderLine;
        orderLine.attach(this);
        history.add(new ItemStatusChange(this, ItemStatus.NEW, sentAt, null));
    }

    /**
     * Item lifecycle (BR-08): New → In Progress → Ready, Ready → In Progress when a dish is sent back
     * (a reason is required), and New → Cancelled. Any other move is refused.
     */
    public void changeStatus(ItemStatus target, String reason, LocalDateTime at) {
        boolean allowed = switch (status) {
            case NEW -> target == ItemStatus.IN_PROGRESS || target == ItemStatus.CANCELLED;
            case IN_PROGRESS -> target == ItemStatus.READY;
            case READY -> target == ItemStatus.IN_PROGRESS;
            case CANCELLED -> false;
        };
        if (!allowed) {
            throw new BusinessRuleException("\"" + dishName() + "\" cannot go from " + label(status)
                    + " to " + label(target));
        }
        boolean sentBack = status == ItemStatus.READY;
        if (sentBack && (reason == null || reason.isBlank())) {
            throw new BusinessRuleException("Give a reason when a dish is sent back");
        }
        if (reason != null && reason.length() > ItemStatusChange.MAX_REASON_LENGTH) {
            throw new IllegalArgumentException("Reason must be at most 200 characters");
        }
        status = target;
        history.add(new ItemStatusChange(this, target, at, reason == null || reason.isBlank() ? null : reason.trim()));
    }

    /** New or In Progress: the kitchen still has work on this dish (used by BR-09). */
    public boolean isPending() {
        return status == ItemStatus.NEW || status == ItemStatus.IN_PROGRESS;
    }

    private String dishName() {
        return orderLine.getMenuItem().getName();
    }

    private static String label(ItemStatus s) {
        return switch (s) {
            case NEW -> "New";
            case IN_PROGRESS -> "In Progress";
            case READY -> "Ready";
            case CANCELLED -> "Cancelled";
        };
    }

    public Long getId() {
        return id;
    }

    public KitchenTicket getTicket() {
        return ticket;
    }

    public OrderLine getOrderLine() {
        return orderLine;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public List<ItemStatusChange> getHistory() {
        return Collections.unmodifiableList(history);
    }
}
