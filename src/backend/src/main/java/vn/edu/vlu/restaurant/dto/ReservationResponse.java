package vn.edu.vlu.restaurant.dto;

import java.time.LocalDateTime;
import vn.edu.vlu.restaurant.domain.Reservation;
import vn.edu.vlu.restaurant.domain.ReservationStatus;

public record ReservationResponse(Long id, String code, Long tableId, int tableNumber, String guestName,
                                  String phone, int partySize, LocalDateTime startTime, String note,
                                  ReservationStatus status) {

    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(reservation.getId(), reservation.getCode(),
                reservation.getTable().getId(), reservation.getTable().getNumber(),
                reservation.getGuestName(), reservation.getPhone(), reservation.getPartySize(),
                reservation.getStartTime(), reservation.getNote(), reservation.getStatus());
    }
}
