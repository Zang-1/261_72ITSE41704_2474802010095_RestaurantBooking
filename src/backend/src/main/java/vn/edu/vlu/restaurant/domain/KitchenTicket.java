package vn.edu.vlu.restaurant.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * The dishes of one order that were sent to the kitchen together (FR-14). Items are stored in course
 * order, Starter → Main → Dessert, which is the order the kitchen display shows them in (BR-06).
 * Tickets are created through {@link CustomerOrder#sendToKitchen}.
 */
@Entity
@Table(name = "kitchen_ticket")
public class KitchenTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<KitchenTicketItem> items = new ArrayList<>();

    protected KitchenTicket() {
    }

    KitchenTicket(CustomerOrder order, List<OrderLine> lines, LocalDateTime sentAt) {
        this.order = order;
        this.sentAt = sentAt;
        lines.stream()
                .sorted(Comparator.comparing(line -> line.getMenuItem().getCourse()))
                .forEach(line -> items.add(new KitchenTicketItem(this, line, sentAt)));
    }

    public Long getId() {
        return id;
    }

    public CustomerOrder getOrder() {
        return order;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public List<KitchenTicketItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
