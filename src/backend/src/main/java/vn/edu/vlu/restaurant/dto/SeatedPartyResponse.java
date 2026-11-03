package vn.edu.vlu.restaurant.dto;

/** The waiting-list party that was just seated (FR-08). */
public record SeatedPartyResponse(String partyName, int partySize, int tableNumber) {
}
