package vn.edu.vlu.restaurant.domain;

/** Life cycle of an order (docs/diagrams/lam/08-state-4-order.png). */
public enum OrderStatus {
    /** Waiters can still add and remove lines. */
    OPEN,
    /** The kitchen has started a dish, so the sent lines can no longer change (BR-08). */
    LOCKED,
    /** The bill is paid. */
    CLOSED
}
