package vn.edu.vlu.restaurant.domain;

/** Status of one dish in the kitchen (docs/diagrams/lam/08-state-3-ticket-item.png). */
public enum ItemStatus {
    /** Sent, not started. The Waiter/Host can still cancel it. */
    NEW,
    /** Being cooked; locked for the Waiter/Host (BR-08). */
    IN_PROGRESS,
    READY,
    /** Cancelled by the Waiter/Host before cooking started. */
    CANCELLED
}
