package vn.edu.vlu.restaurant.domain;

/** Accepted payment methods (FR-25 as changed by CR-04). Only cash can be more than the amount due (BR-11). */
public enum PaymentMethod {
    CASH,
    CARD,
    BANK_TRANSFER,
    E_WALLET
}
