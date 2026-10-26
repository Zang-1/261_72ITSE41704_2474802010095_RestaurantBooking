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
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import vn.edu.vlu.restaurant.exception.BusinessRuleException;

/**
 * The order of one table (FR-09). Named CustomerOrder because ORDER is a reserved word in SQL; the
 * class diagram calls it Order. Lines can be added or removed only while the order is OPEN.
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

    protected CustomerOrder() {
    }

    public CustomerOrder(DiningTable table, User waiter, LocalDateTime createdAt) {
        this.table = table;
        this.waiter = waiter;
        this.createdAt = createdAt;
    }

    /** Adds a dish. Refused when the order is locked (BR-08) or the dish is out of stock (BR-07). */
    public OrderLine addLine(MenuItem item, int quantity, String note) {
        requireOpen();
        if (!item.isOrderable()) {
            throw new BusinessRuleException("\"" + item.getName() + "\" is not available right now");
        }
        OrderLine line = new OrderLine(this, item, quantity, note);
        lines.add(line);
        return line;
    }

    public void removeLine(OrderLine line) {
        requireOpen();
        if (!lines.remove(line)) {
            throw new BusinessRuleException("The line does not belong to this order");
        }
    }

    /** The kitchen started a dish: the order can no longer be edited (BR-08). */
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
}
