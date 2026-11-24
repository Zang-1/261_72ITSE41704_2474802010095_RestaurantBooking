package vn.edu.vlu.restaurant.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import vn.edu.vlu.restaurant.dto.AvailabilityQuery;
import vn.edu.vlu.restaurant.dto.OnlineBookingRequest;
import vn.edu.vlu.restaurant.dto.OnlineBookingResponse;
import vn.edu.vlu.restaurant.dto.ReservationRequest;
import vn.edu.vlu.restaurant.dto.TableResponse;
import vn.edu.vlu.restaurant.exception.BusinessRuleException;
import vn.edu.vlu.restaurant.exception.InvalidInputException;

/**
 * Bookings made by guests on the public page. The guest chooses only the time and party size; the
 * smallest free table that fits is booked through ReservationService, so BR-01, BR-02 and the row
 * lock (NFR-06) apply exactly as for a booking made by the host. The booking appears on the staff
 * Reservations screen at once.
 * Not @Transactional on purpose: each attempt runs in its own transaction, so a table lost to a
 * booking made at the same moment does not roll back the try on the next table.
 */
@Service
public class OnlineBookingService {

    /** shortcut: fixed booking hours, as opening hours were not recorded in the interview; make them a setting if needed. */
    static final LocalTime FIRST_SLOT = LocalTime.of(11, 0);
    static final LocalTime LAST_SLOT = LocalTime.of(21, 0);
    static final int SLOT_MINUTES = 30;

    private final ReservationService reservationService;
    private final Clock clock;

    public OnlineBookingService(ReservationService reservationService, Clock clock) {
        this.reservationService = reservationService;
        this.clock = clock;
    }

    public record Slot(LocalTime time, boolean available) {
    }

    /** Every half hour of the day with whether a table for the party is still free (past times left out). */
    public List<Slot> slots(LocalDate date, int partySize) {
        if (partySize < 1 || partySize > 20) {
            throw new InvalidInputException("partySize", "Party size must be between 1 and 20");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        List<Slot> slots = new ArrayList<>();
        for (LocalTime time = FIRST_SLOT; !time.isAfter(LAST_SLOT); time = time.plusMinutes(SLOT_MINUTES)) {
            if (date.atTime(time).isAfter(now)) {
                boolean free = !reservationService.searchAvailableTables(new AvailabilityQuery(date, time, partySize)).isEmpty();
                slots.add(new Slot(time, free));
            }
        }
        return slots;
    }

    public OnlineBookingResponse book(OnlineBookingRequest request) {
        List<TableResponse> tables = reservationService.searchAvailableTables(
                new AvailabilityQuery(request.date(), request.startTime(), request.partySize()));
        String note = request.note() == null || request.note().isBlank()
                ? "Online booking" : "Online booking: " + request.note().trim();
        // smallest table first (the search sorts by capacity); another guest may take one meanwhile
        for (TableResponse table : tables) {
            try {
                return OnlineBookingResponse.from(reservationService.create(new ReservationRequest(table.id(),
                        request.guestName(), request.phone(), request.partySize(), request.date(), request.startTime(), note)));
            } catch (BusinessRuleException tableTakenMeanwhile) {
                // try the next table
            }
        }
        throw new BusinessRuleException("No table is free for this party at that time. Please choose another time.");
    }
}
