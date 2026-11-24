package vn.edu.vlu.restaurant.dto;

import java.time.LocalDateTime;

/** What the guest sees after booking. The table number is left out: staff may still move the party. */
public record OnlineBookingResponse(String code, String guestName, int partySize, LocalDateTime startTime) {

    public static OnlineBookingResponse from(ReservationResponse reservation) {
        return new OnlineBookingResponse(reservation.code(), reservation.guestName(), reservation.partySize(),
                reservation.startTime());
    }
}
