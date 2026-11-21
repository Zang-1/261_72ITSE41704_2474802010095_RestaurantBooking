package vn.edu.vlu.restaurant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import vn.edu.vlu.restaurant.exception.BusinessRuleException;

/**
 * A working shift, opened and closed by the Manager (FR-27, UC-11). Every payment belongs to the shift
 * that is open when it is recorded (BR-13). Only one shift may be open: the service checks it, and the
 * unique key on open_marker stops two Managers opening one at the same moment.
 */
@Entity
@Table(name = "shift")
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opened_by_id", nullable = false)
    private User openedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by_id")
    private User closedBy;

    /** Cash counted in the till when the shift is closed (FR-29, CR-05); null when not entered. */
    @Column(name = "counted_cash")
    private Long countedCash;

    /** 1 while open, null once closed; unique in the database (BR-13). */
    @Column(name = "open_marker")
    private Integer openMarker = 1;

    protected Shift() {
    }

    public Shift(User openedBy, LocalDateTime openedAt) {
        this.openedBy = openedBy;
        this.openedAt = openedAt;
    }

    /**
     * Ends the shift. The caller has already checked that no bill is unpaid (BR-13).
     * countedCash may be null when the Manager does not count the till.
     */
    public void close(User closedBy, LocalDateTime at, Long countedCash) {
        if (!isOpen()) {
            throw new BusinessRuleException("This shift is already closed");
        }
        if (countedCash != null && countedCash < 0) {
            throw new IllegalArgumentException("Counted cash cannot be negative");
        }
        this.closedBy = closedBy;
        this.closedAt = at;
        this.countedCash = countedCash;
        this.openMarker = null;
    }

    public boolean isOpen() {
        return closedAt == null;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public User getOpenedBy() {
        return openedBy;
    }

    public User getClosedBy() {
        return closedBy;
    }

    public Long getCountedCash() {
        return countedCash;
    }
}
