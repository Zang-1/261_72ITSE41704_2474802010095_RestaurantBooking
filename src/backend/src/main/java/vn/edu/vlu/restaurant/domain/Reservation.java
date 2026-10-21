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
import vn.edu.vlu.restaurant.exception.BusinessRuleException;

/**
 * An advance booking of one table. Only a Confirmed reservation whose start time has not passed
 * can be modified or cancelled (FR-05). endTime() and overlaps() for BR-01 follow in Week 7.
 */
@Entity
@Table(name = "reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id", nullable = false)
    private DiningTable table;

    @Column(name = "guest_name", nullable = false, length = 100)
    private String guestName;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(name = "party_size", nullable = false)
    private int partySize;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(length = 200)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.CONFIRMED;

    protected Reservation() {
    }

    public Reservation(String code, DiningTable table, String guestName, String phone,
                       int partySize, LocalDateTime startTime, String note) {
        this.code = code;
        setDetails(table, guestName, phone, partySize, startTime, note);
    }

    public void modify(DiningTable table, String guestName, String phone,
                       int partySize, LocalDateTime startTime, String note, LocalDateTime now) {
        requireChangeable(now);
        setDetails(table, guestName, phone, partySize, startTime, note);
    }

    public void cancel(LocalDateTime now) {
        requireChangeable(now);
        this.status = ReservationStatus.CANCELLED;
    }

    public boolean hasStarted(LocalDateTime now) {
        return !now.isBefore(startTime);
    }

    private void requireChangeable(LocalDateTime now) {
        if (status != ReservationStatus.CONFIRMED) {
            throw new BusinessRuleException("Only a confirmed reservation can be changed");
        }
        if (hasStarted(now)) {
            throw new BusinessRuleException("The reservation has already started and can no longer be changed");
        }
    }

    private void setDetails(DiningTable table, String guestName, String phone,
                            int partySize, LocalDateTime startTime, String note) {
        this.table = table;
        this.guestName = guestName;
        this.phone = phone;
        this.partySize = partySize;
        this.startTime = startTime;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public DiningTable getTable() {
        return table;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getPhone() {
        return phone;
    }

    public int getPartySize() {
        return partySize;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public String getNote() {
        return note;
    }

    public ReservationStatus getStatus() {
        return status;
    }
}
