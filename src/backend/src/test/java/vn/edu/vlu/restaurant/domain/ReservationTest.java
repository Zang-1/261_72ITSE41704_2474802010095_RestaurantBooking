package vn.edu.vlu.restaurant.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import vn.edu.vlu.restaurant.exception.BusinessRuleException;

/** Unit tests of the domain classes, without Spring or a database. */
class ReservationTest {

    private final TableSection indoor = new TableSection("Indoor");
    private final DiningTable table = new DiningTable(5, 4, indoor);
    private final LocalDateTime start = LocalDateTime.of(2026, 10, 24, 19, 0);

    @Test
    void cancelBeforeStartTime() {
        Reservation reservation = newReservation();

        reservation.cancel(start.minusHours(2));

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
    }

    @Test
    void cannotCancelAfterStartTime() {
        Reservation reservation = newReservation();

        assertThrows(BusinessRuleException.class, () -> reservation.cancel(start.plusMinutes(1)));
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
    }

    @Test
    void cannotModifyCancelledReservation() {
        Reservation reservation = newReservation();
        reservation.cancel(start.minusDays(1));

        assertThrows(BusinessRuleException.class, () -> reservation.modify(table, "Ngo Minh Tuan", "0909876543",
                3, start.plusHours(1), null, start.minusDays(1)));
    }

    @Test
    void tableSeatsPartyUpToItsCapacity() {
        assertTrue(table.canSeat(4));
        assertFalse(table.canSeat(5));
    }

    @Test
    void tableCapacityMustBeBetween1And20() {
        assertThrows(IllegalArgumentException.class, () -> new DiningTable(99, 21, indoor));
        assertThrows(IllegalArgumentException.class, () -> new DiningTable(99, 0, indoor));
    }

    /** BR-01 boundary with the defaults: 180 dining + 15 turn = a 195-minute block. */
    @Test
    void bookingsOverlapInsideTheBlockOnly() {
        ReservationSettings settings = new ReservationSettings(180, 15);
        assertTrue(Reservation.overlap(start, start.plusMinutes(194), settings));
        assertFalse(Reservation.overlap(start, start.plusMinutes(195), settings));
        assertTrue(Reservation.overlap(start, start.minusMinutes(194), settings));
        assertFalse(Reservation.overlap(start, start.minusMinutes(195), settings));
    }

    @Test
    void checkInOpensThirtyMinutesBefore() {
        Reservation early = newReservation();
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> early.checkIn(start.minusMinutes(31)));
        assertEquals("Check-in opens 30 minutes before the booking", ex.getMessage());

        Reservation onTime = newReservation();
        onTime.checkIn(start.minusMinutes(30));
        assertEquals(ReservationStatus.CHECKED_IN, onTime.getStatus());
    }

    @Test
    void lateGuestsCanCheckInUntilTheNoShowLimit() {
        Reservation late = newReservation();
        late.checkIn(start.plusMinutes(44));
        assertEquals(ReservationStatus.CHECKED_IN, late.getStatus());

        Reservation tooLate = newReservation();
        assertThrows(BusinessRuleException.class, () -> tooLate.checkIn(start.plusMinutes(45)));
    }

    @Test
    void noShowAfterFortyFiveMinutes() {
        Reservation reservation = newReservation();
        reservation.markNoShow(start.plusMinutes(44));
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());

        reservation.markNoShow(start.plusMinutes(45));
        assertEquals(ReservationStatus.NO_SHOW, reservation.getStatus());
    }

    @Test
    void checkedInReservationIsNeverANoShow() {
        Reservation reservation = newReservation();
        reservation.checkIn(start);
        reservation.markNoShow(start.plusHours(2));
        assertEquals(ReservationStatus.CHECKED_IN, reservation.getStatus());
    }

    private Reservation newReservation() {
        return new Reservation("RABC2345", table, "Ngo Minh Tuan", "0909876543", 3, start, null);
    }
}
