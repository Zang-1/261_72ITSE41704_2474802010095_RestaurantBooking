package vn.edu.vlu.restaurant.domain;

/** How a bill is divided between the guests (FR-23, FR-24). */
public enum SplitType {
    /** One payer for the whole bill. */
    NONE,
    /** N equal shares rounded to 1,000 VND, the remainder on share 1 (BR-10). */
    EVEN,
    /** Every bill line belongs to exactly one share. */
    BY_ITEM
}
