package vn.edu.vlu.restaurant.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit tests of the menu item rules, without Spring or a database. */
class MenuItemTest {

    private final MenuCategory mains = new MenuCategory("Main dishes");

    @Test
    void newItemIsOrderable() {
        assertTrue(newItem().isOrderable());
    }

    @Test
    void outOfStockItemIsNotOrderableUntilRestocked() {
        MenuItem item = newItem();

        item.markOutOfStock();
        assertFalse(item.isOrderable());

        item.restock();
        assertTrue(item.isOrderable());
    }

    @Test
    void inactiveItemIsNotOrderable() {
        MenuItem item = newItem();

        item.deactivate();

        assertFalse(item.isOrderable());
    }

    @Test
    void priceMustBeBetweenOneAndTenMillion() {
        assertThrows(IllegalArgumentException.class,
                () -> new MenuItem(mains, "Free dish", null, 0, Course.MAIN));
        assertThrows(IllegalArgumentException.class,
                () -> new MenuItem(mains, "Gold dish", null, 10_000_001, Course.MAIN));
    }

    private MenuItem newItem() {
        return new MenuItem(mains, "Beef pho", null, 75_000, Course.MAIN);
    }
}
