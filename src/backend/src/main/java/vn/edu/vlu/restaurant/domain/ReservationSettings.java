package vn.edu.vlu.restaurant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Manager-configurable timings used by the no-double-booking rule (BR-01). Single row, id = 1. */
@Entity
@Table(name = "reservation_settings")
public class ReservationSettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(name = "dining_minutes", nullable = false)
    private int diningMinutes;

    @Column(name = "turn_minutes", nullable = false)
    private int turnMinutes;

    protected ReservationSettings() {
    }

    public ReservationSettings(int diningMinutes, int turnMinutes) {
        this.id = SINGLETON_ID;
        update(diningMinutes, turnMinutes);
    }

    public void update(int diningMinutes, int turnMinutes) {
        this.diningMinutes = diningMinutes;
        this.turnMinutes = turnMinutes;
    }

    public Long getId() {
        return id;
    }

    public int getDiningMinutes() {
        return diningMinutes;
    }

    public int getTurnMinutes() {
        return turnMinutes;
    }
}
