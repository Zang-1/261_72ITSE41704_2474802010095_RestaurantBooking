package vn.edu.vlu.restaurant.domain;

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
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** One step in the history of a ticket item, e.g. a dish sent back with its reason (UC-09 alternative 3a). */
@Entity
@Table(name = "item_status_change")
public class ItemStatusChange {

    public static final int MAX_REASON_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_item_id", nullable = false)
    private KitchenTicketItem item;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemStatus status;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(length = MAX_REASON_LENGTH)
    private String reason;

    protected ItemStatusChange() {
    }

    ItemStatusChange(KitchenTicketItem item, ItemStatus status, LocalDateTime changedAt, String reason) {
        this.item = item;
        this.status = status;
        this.changedAt = changedAt;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public String getReason() {
        return reason;
    }
}
