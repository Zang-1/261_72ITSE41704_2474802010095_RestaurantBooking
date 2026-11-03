package vn.edu.vlu.restaurant.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import vn.edu.vlu.restaurant.exception.BusinessRuleException;

/** BR-05 table lifecycle, without Spring or a database. */
class DiningTableTest {

    private DiningTable table() {
        return new DiningTable(5, 4, new TableSection("Hearth Hall"));
    }

    @Test
    void followsTheFullLifecycle() {
        DiningTable t = table();
        t.changeStatus(TableStatus.RESERVED);
        t.changeStatus(TableStatus.OCCUPIED);
        t.changeStatus(TableStatus.NEEDS_CLEANING);
        t.changeStatus(TableStatus.AVAILABLE);
        assertEquals(TableStatus.AVAILABLE, t.getStatus());
    }

    @Test
    void walkInOrEarlyCheckInGoesStraightToOccupied() {
        DiningTable t = table();
        t.changeStatus(TableStatus.OCCUPIED);
        assertEquals(TableStatus.OCCUPIED, t.getStatus());
    }

    @Test
    void reservedTableCanBeReleased() {
        DiningTable t = table();
        t.changeStatus(TableStatus.RESERVED);
        t.changeStatus(TableStatus.AVAILABLE);
        assertEquals(TableStatus.AVAILABLE, t.getStatus());
    }

    @Test
    void occupiedTableCannotSkipCleaning() {
        DiningTable t = table();
        t.changeStatus(TableStatus.OCCUPIED);
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> t.changeStatus(TableStatus.AVAILABLE));
        assertEquals("Table 5 cannot go from Occupied to Available", ex.getMessage());
    }

    @Test
    void tableThatNeedsCleaningCannotBeSeated() {
        DiningTable t = table();
        t.changeStatus(TableStatus.OCCUPIED);
        t.changeStatus(TableStatus.NEEDS_CLEANING);
        assertThrows(BusinessRuleException.class, () -> t.changeStatus(TableStatus.OCCUPIED));
        assertThrows(BusinessRuleException.class, () -> t.changeStatus(TableStatus.RESERVED));
    }

    @Test
    void sameStatusIsANoOp() {
        DiningTable t = table();
        t.changeStatus(TableStatus.AVAILABLE);
        assertEquals(TableStatus.AVAILABLE, t.getStatus());
    }
}
