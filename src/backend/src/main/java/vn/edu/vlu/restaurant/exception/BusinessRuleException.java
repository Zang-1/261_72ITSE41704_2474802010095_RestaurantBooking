package vn.edu.vlu.restaurant.exception;

/** A request that is well-formed but breaks a business rule. Returned as 409 Conflict. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
