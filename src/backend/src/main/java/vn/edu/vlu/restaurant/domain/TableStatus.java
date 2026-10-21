package vn.edu.vlu.restaurant.domain;

/** Table lifecycle states (BR-05, Figure C.19). */
public enum TableStatus {
    AVAILABLE,
    RESERVED,
    OCCUPIED,
    NEEDS_CLEANING
}
