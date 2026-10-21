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

/**
 * A physical table in the dining room. Named DiningTable because TABLE is a reserved word in SQL.
 * Status changes (BR-05) are added in Week 7 through changeStatus(TableStatus).
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
