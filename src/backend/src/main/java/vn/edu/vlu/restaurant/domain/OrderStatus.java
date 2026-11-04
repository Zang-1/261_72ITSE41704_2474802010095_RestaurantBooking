package vn.edu.vlu.restaurant.domain;

/** Life cycle of an order (docs/diagrams/lam/08-state-4-order.png). */
public enum OrderStatus {
    /** Waiters can still add, change and send lines. */
    OPEN,
    /** The bill is generated, so the order cannot change until the unpaid bill is cancelled (BR-09). */
    LOCKED,
    /** The bill is paid. */
    CLOSED
}
