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
import vn.edu.vlu.restaurant.exception.BusinessRuleException;

/**
 * A physical table in the dining room. Named DiningTable because TABLE is a reserved word in SQL.
 * Its status only moves along the lifecycle of BR-05 (docs/diagrams/giang/08-state-1-table.png).
 */
@Entity
@Table(name = "dining_table")
public class DiningTable {

    public static final int MIN_CAPACITY = 1;
    public static final int MAX_CAPACITY = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private int number;

    @Column(nullable = false)
    private int capacity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private TableSection section;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TableStatus status = TableStatus.AVAILABLE;

    @Column(nullable = false)
    private boolean active = true;

    protected DiningTable() {
    }

    public DiningTable(int number, int capacity, TableSection section) {
        updateDetails(number, capacity, section);
    }

    public void updateDetails(int number, int capacity, TableSection section) {
        if (capacity < MIN_CAPACITY || capacity > MAX_CAPACITY) {
            throw new IllegalArgumentException("Capacity must be between 1 and 20");
        }
        this.number = number;
        this.capacity = capacity;
        this.section = section;
    }

    /** Capacity fit: a party only fits a table with at least as many seats (BR-02). */
    public boolean canSeat(int partySize) {
        return partySize <= capacity;
    }

    /**
     * Table lifecycle (BR-05): Available → Reserved → Occupied → Needs cleaning → Available.
     * Available → Occupied covers walk-ins and early check-in; Reserved → Available covers a no-show,
     * a cancellation or a moved booking. Any other move is refused.
     */
    public void changeStatus(TableStatus target) {
        if (target == status) {
            return;
        }
        boolean allowed = switch (status) {
            case AVAILABLE -> target == TableStatus.RESERVED || target == TableStatus.OCCUPIED;
            case RESERVED -> target == TableStatus.OCCUPIED || target == TableStatus.AVAILABLE;
            case OCCUPIED -> target == TableStatus.NEEDS_CLEANING;
            case NEEDS_CLEANING -> target == TableStatus.AVAILABLE;
        };
        if (!allowed) {
            throw new BusinessRuleException("Table " + number + " cannot go from " + label(status) + " to " + label(target));
        }
        status = target;
    }

    private static String label(TableStatus s) {
        return switch (s) {
            case AVAILABLE -> "Available";
            case RESERVED -> "Reserved";
            case OCCUPIED -> "Occupied";
            case NEEDS_CLEANING -> "Needs cleaning";
        };
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public int getNumber() {
        return number;
    }

    public int getCapacity() {
        return capacity;
    }

    public TableSection getSection() {
        return section;
    }

    public TableStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return active;
    }
}
