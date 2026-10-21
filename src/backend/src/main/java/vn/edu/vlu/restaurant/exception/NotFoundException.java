package vn.edu.vlu.restaurant.exception;

/** The requested record does not exist. Returned as 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
