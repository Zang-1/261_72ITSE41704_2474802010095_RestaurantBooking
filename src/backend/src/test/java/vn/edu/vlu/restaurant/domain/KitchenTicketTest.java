package vn.edu.vlu.restaurant.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import vn.edu.vlu.restaurant.exception.BusinessRuleException;

/** Unit tests of sending to the kitchen (BR-06, NFR-07) and of the ticket item lifecycle (BR-08). */
class KitchenTicketTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 4, 19, 0);

    private final MenuCategory category = new MenuCategory("Dishes");
    private final MenuItem rolls = new MenuItem(category, "Fresh spring rolls", null, 55_000, Course.STARTER);
    private final MenuItem pho = new MenuItem(category, "Beef pho", null, 75_000, Course.MAIN);
    private final MenuItem flan = new MenuItem(category, "Caramel flan", null, 30_000, Course.DESSERT);
    private final CustomerOrder order = new CustomerOrder(new DiningTable(5, 4, new TableSection("Indoor")),
            new User("waiter", "hash", Role.WAITER_HOST), NOW.minusMinutes(10));

    @Test
    void sendCreatesOneTicketInCourseOrder() {
        order.addLine(flan, 1, null);
        order.addLine(pho, 2, null);
        order.addLine(rolls, 1, null);

        KitchenTicket ticket = order.sendToKitchen(order.unsentLines(), NOW);

        assertEquals(NOW, ticket.getSentAt());
        assertEquals(List.of(Course.STARTER, Course.MAIN, Course.DESSERT),
                ticket.getItems().stream().map(i -> i.getOrderLine().getMenuItem().getCourse()).toList());
        assertTrue(ticket.getItems().stream().allMatch(i -> i.getStatus() == ItemStatus.NEW));
        assertTrue(order.unsentLines().isEmpty());
        assertEquals(1, order.getTickets().size());
    }

    @Test
    void laterCourseCannotBeSentWhileAnEarlierCourseIsUnsent() {
        order.addLine(rolls, 1, null);
        OrderLine main = order.addLine(pho, 1, null);

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> order.sendToKitchen(List.of(main), NOW));

        assertEquals("Send the Starter dishes before the Main dishes (BR-06)", error.getMessage());
        assertEquals(2, order.unsentLines().size());
        assertTrue(order.getTickets().isEmpty());
    }

    @Test
    void earlierCourseCanBeSentFirst() {
        OrderLine starter = order.addLine(rolls, 1, null);
        order.addLine(pho, 1, null);

        order.sendToKitchen(List.of(starter), NOW);
        KitchenTicket second = order.sendToKitchen(order.unsentLines(), NOW.plusMinutes(15));

        assertEquals(2, order.getTickets().size());
        assertEquals(Course.MAIN, second.getItems().get(0).getOrderLine().getMenuItem().getCourse());
    }

    @Test
    void sendingTwiceNeverCreatesADuplicateTicket() {
        order.addLine(pho, 1, null);
        order.sendToKitchen(order.unsentLines(), NOW);

        assertThrows(BusinessRuleException.class, () -> order.sendToKitchen(order.unsentLines(), NOW));
        assertEquals(1, order.getTickets().size());
    }

    @Test
    void outOfStockDishIsNotSentAndNothingElseIsSentEither() {
        order.addLine(rolls, 1, null);
        order.addLine(pho, 1, null);
        pho.markOutOfStock();

        assertThrows(BusinessRuleException.class, () -> order.sendToKitchen(order.unsentLines(), NOW));
        assertEquals(2, order.unsentLines().size());
        assertTrue(order.getTickets().isEmpty());
    }

    @Test
    void sentLineCannotBeChanged() {
        OrderLine line = order.addLine(pho, 1, null);
        order.sendToKitchen(order.unsentLines(), NOW);

        assertThrows(BusinessRuleException.class, () -> order.changeLine(line, 2, null));
        assertThrows(BusinessRuleException.class, () -> order.removeLine(line));
        assertEquals(1, line.getQuantity());
    }

    @Test
    void newItemCanBeCancelledAndLeavesTheTotal() {
        OrderLine line = order.addLine(pho, 2, null);
        order.sendToKitchen(order.unsentLines(), NOW);

        order.cancelLine(line, NOW.plusMinutes(1));

        assertEquals(ItemStatus.CANCELLED, line.getTicketItem().getStatus());
        assertTrue(line.isCancelled());
        assertEquals(0, order.total());
    }

    @Test
    void itemInProgressCannotBeCancelled() {
        OrderLine line = order.addLine(pho, 1, null);
        order.sendToKitchen(order.unsentLines(), NOW);
        line.getTicketItem().changeStatus(ItemStatus.IN_PROGRESS, null, NOW.plusMinutes(2));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> order.cancelLine(line, NOW.plusMinutes(3)));

        assertEquals("\"Beef pho\" is already being cooked and cannot be cancelled (BR-08)", error.getMessage());
        assertEquals(ItemStatus.IN_PROGRESS, line.getTicketItem().getStatus());
    }

    @Test
    void newDishesAfterCookingStartsGoOnANewTicket() {
        OrderLine first = order.addLine(pho, 1, null);
        order.sendToKitchen(order.unsentLines(), NOW);
        first.getTicketItem().changeStatus(ItemStatus.IN_PROGRESS, null, NOW.plusMinutes(2));

        order.addLine(flan, 1, null);
        KitchenTicket second = order.sendToKitchen(order.unsentLines(), NOW.plusMinutes(20));

        assertEquals(2, order.getTickets().size());
        assertEquals(1, second.getItems().size());
    }

    @Test
    void itemLifecycleFollowsTheStateDiagram() {
        OrderLine line = order.addLine(pho, 1, null);
        order.sendToKitchen(order.unsentLines(), NOW);
        KitchenTicketItem item = line.getTicketItem();

        assertThrows(BusinessRuleException.class, () -> item.changeStatus(ItemStatus.READY, null, NOW));
        item.changeStatus(ItemStatus.IN_PROGRESS, null, NOW.plusMinutes(1));
        item.changeStatus(ItemStatus.READY, null, NOW.plusMinutes(12));
        assertThrows(BusinessRuleException.class, () -> item.changeStatus(ItemStatus.IN_PROGRESS, " ", NOW));
        item.changeStatus(ItemStatus.IN_PROGRESS, "Too salty", NOW.plusMinutes(14));

        assertEquals(ItemStatus.IN_PROGRESS, item.getStatus());
        assertEquals(List.of(ItemStatus.NEW, ItemStatus.IN_PROGRESS, ItemStatus.READY, ItemStatus.IN_PROGRESS),
                item.getHistory().stream().map(ItemStatusChange::getStatus).toList());
        assertEquals("Too salty", item.getHistory().get(3).getReason());
    }

    @Test
    void kitchenWorkIsPendingUntilEveryItemIsReadyOrCancelled() {
        OrderLine line = order.addLine(pho, 1, null);
        assertFalse(order.hasKitchenWorkPending());

        order.sendToKitchen(order.unsentLines(), NOW);
        assertTrue(order.hasKitchenWorkPending());

        line.getTicketItem().changeStatus(ItemStatus.IN_PROGRESS, null, NOW.plusMinutes(1));
        line.getTicketItem().changeStatus(ItemStatus.READY, null, NOW.plusMinutes(10));
        assertFalse(order.hasKitchenWorkPending());
    }
}
