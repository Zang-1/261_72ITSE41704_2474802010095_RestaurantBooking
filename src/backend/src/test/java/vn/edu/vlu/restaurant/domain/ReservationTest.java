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

    private Reservation newReservation() {
        return new Reservation("RABC2345", table, "Ngo Minh Tuan", "0909876543", 3, start, null);
    }
}
