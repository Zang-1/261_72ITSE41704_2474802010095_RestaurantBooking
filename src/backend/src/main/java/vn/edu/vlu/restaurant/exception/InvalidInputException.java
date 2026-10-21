package vn.edu.vlu.restaurant.exception;

/**
 * An input value that is invalid in the current state of the data, e.g. a duplicate table number
 * or a date in the past. Returned as 400 with the message attached to the field (NFR-11).
 */
public class InvalidInputException extends RuntimeException {

    private final String field;

    public InvalidInputException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
