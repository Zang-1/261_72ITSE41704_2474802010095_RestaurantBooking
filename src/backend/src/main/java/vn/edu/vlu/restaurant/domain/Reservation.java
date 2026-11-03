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
 * can be modified or cancelled (FR-05).
 */
@Entity
@Table(name = "reservation")
public class Reservation {

    /** The table turns Reserved this long before the booking (BR-05). */
    public static final int RESERVED_BEFORE_MINUTES = 30;
    /** Guests may check in this early (interview, F6). */
    public static final int EARLY_CHECK_IN_MINUTES = 30;
    /** Late guests are held this long, then the booking is a no-show (FR-06, CR-01). */
    public static final int NO_SHOW_AFTER_MINUTES = 45;

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

    public LocalDateTime endTime(ReservationSettings settings) {
        return startTime.plusMinutes(settings.getDiningMinutes());
    }

    /** BR-01: two bookings of the same table overlap if startA < endB + turn and startB < endA + turn. */
    public boolean overlaps(Reservation other, ReservationSettings settings) {
        return overlap(startTime, other.startTime, settings);
    }

    public static boolean overlap(LocalDateTime startA, LocalDateTime startB, ReservationSettings s) {
        long block = s.getDiningMinutes() + s.getTurnMinutes();
        return startA.isBefore(startB.plusMinutes(block)) && startB.isBefore(startA.plusMinutes(block));
    }

    /** Guests arrived: allowed from 30 minutes before the start until the no-show limit (BR-05, CR-01). */
    public void checkIn(LocalDateTime now) {
        if (status != ReservationStatus.CONFIRMED) {
            throw new BusinessRuleException("Only a confirmed reservation can be checked in");
        }
        if (now.isBefore(startTime.minusMinutes(EARLY_CHECK_IN_MINUTES))) {
            throw new BusinessRuleException("Check-in opens " + EARLY_CHECK_IN_MINUTES + " minutes before the booking");
        }
        if (isNoShow(now)) {
            throw new BusinessRuleException("The guests are more than " + NO_SHOW_AFTER_MINUTES + " minutes late; seat them as a walk-in");
        }
        status = ReservationStatus.CHECKED_IN;
    }

    public boolean isNoShow(LocalDateTime now) {
        return !now.isBefore(startTime.plusMinutes(NO_SHOW_AFTER_MINUTES));
    }

    /** FR-06: guests who have not checked in 45 minutes after the start (CR-01). */
    public void markNoShow(LocalDateTime now) {
        if (status == ReservationStatus.CONFIRMED && isNoShow(now)) {
            status = ReservationStatus.NO_SHOW;
        }
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
