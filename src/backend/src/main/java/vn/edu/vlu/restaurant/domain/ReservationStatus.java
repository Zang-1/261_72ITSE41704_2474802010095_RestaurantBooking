package vn.edu.vlu.restaurant.domain;

/** Reservation states (docs/diagrams/giang/08-state-2-reservation.png). */
public enum ReservationStatus {
    CONFIRMED,
    CHECKED_IN,
    CANCELLED,
    NO_SHOW
}
