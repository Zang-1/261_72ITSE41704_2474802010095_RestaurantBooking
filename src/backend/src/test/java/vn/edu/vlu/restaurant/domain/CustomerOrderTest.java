package vn.edu.vlu.restaurant.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import vn.edu.vlu.restaurant.exception.BusinessRuleException;

/** Unit tests of the order rules (BR-07, BR-08), without Spring or a database. */
class CustomerOrderTest {

    private final MenuCategory mains = new MenuCategory("Main dishes");
    private final MenuItem pho = new MenuItem(mains, "Beef pho", null, 75_000, Course.MAIN);
    private final MenuItem rice = new MenuItem(mains, "Grilled pork with rice", null, 85_000, Course.MAIN);
    private final DiningTable table = new DiningTable(5, 4, new TableSection("Indoor"));
    private final User waiter = new User("waiter", "hash", Role.WAITER_HOST);

    @Test
    void newOrderIsOpenAndEditable() {
        CustomerOrder order = newOrder();

        assertEquals(OrderStatus.OPEN, order.getStatus());
        assertTrue(order.isEditable());
    }

    @Test
    void addLineCopiesThePriceFromTheMenu() {
        CustomerOrder order = newOrder();

        OrderLine line = order.addLine(pho, 2, "no onion");
        pho.updateDetails(mains, "Beef pho", null, 90_000, Course.MAIN);

        assertEquals(75_000, line.getUnitPrice());
        assertEquals(150_000, line.lineTotal());
        assertEquals("no onion", line.getNote());
    }

    @Test
    void totalAddsAllLines() {
        CustomerOrder order = newOrder();
        order.addLine(pho, 2, null);
        order.addLine(rice, 1, null);

        assertEquals(235_000, order.total());
    }

    @Test
    void outOfStockItemCannotBeAdded() {
        CustomerOrder order = newOrder();
        pho.markOutOfStock();

        assertThrows(BusinessRuleException.class, () -> order.addLine(pho, 1, null));
        assertTrue(order.getLines().isEmpty());
    }

    @Test
    void quantityMustBeBetweenOneAndFifty() {
        CustomerOrder order = newOrder();

        assertThrows(IllegalArgumentException.class, () -> order.addLine(pho, 0, null));
        assertThrows(IllegalArgumentException.class, () -> order.addLine(pho, 51, null));
        assertTrue(order.getLines().isEmpty());
    }

    @Test
    void lockedOrderCannotBeChanged() {
        CustomerOrder order = newOrder();
        OrderLine line = order.addLine(pho, 1, null);

        order.lock();

        assertFalse(order.isEditable());
        assertThrows(BusinessRuleException.class, () -> order.addLine(rice, 1, null));
        assertThrows(BusinessRuleException.class, () -> order.removeLine(line));
        assertEquals(1, order.getLines().size());
    }

    @Test
    void removeLineOnOpenOrder() {
        CustomerOrder order = newOrder();
        OrderLine line = order.addLine(pho, 1, null);

        order.removeLine(line);

        assertTrue(order.getLines().isEmpty());
    }

    private CustomerOrder newOrder() {
        return new CustomerOrder(table, waiter, LocalDateTime.of(2026, 10, 26, 19, 0));
    }
}
