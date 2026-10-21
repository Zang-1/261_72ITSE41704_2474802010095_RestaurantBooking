package vn.edu.vlu.restaurant.controller;

/** @PreAuthorize expressions shared by the controllers. A Manager can do everything a Waiter/Host can. */
final class AccessRules {

    static final String FRONT_OF_HOUSE = "hasAnyRole('WAITER_HOST', 'MANAGER')";
    static final String MANAGER = "hasRole('MANAGER')";

    private AccessRules() {
    }
}
