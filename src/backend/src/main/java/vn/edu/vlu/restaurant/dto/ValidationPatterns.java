package vn.edu.vlu.restaurant.dto;

/** Input formats shared by several requests. */
public final class ValidationPatterns {

    /** Vietnamese phone number: 10 digits starting with 0, e.g. 0901234567. */
    public static final String PHONE = "0\\d{9}";
    public static final String PHONE_MESSAGE = "Phone number must have 10 digits and start with 0";

    private ValidationPatterns() {
    }
}
