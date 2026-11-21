package vn.edu.vlu.restaurant.domain;

/** Life cycle of a bill (docs/diagrams/khoa/08-state-5-bill.png). */
public enum BillStatus {
    /** Generated and waiting for payment; the order is locked (BR-09). */
    OPEN,
    /** Paid in full; read-only from now on (BR-12). */
    CLOSED,
    /** Cancelled before any payment so the guests can order more; kept for the record (BR-09). */
    CANCELLED
}
