package vn.edu.vlu.restaurant.domain;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import vn.edu.vlu.restaurant.exception.BusinessRuleException;
import vn.edu.vlu.restaurant.exception.InvalidInputException;

/** Unit tests of the billing rules BR-09 to BR-13, without Spring or a database. */
class BillTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 20, 0);

    private final MenuCategory mains = new MenuCategory("Main dishes");
    private final MenuItem pho = new MenuItem(mains, "Beef pho", null, 75_000, Course.MAIN);
    private final MenuItem rice = new MenuItem(mains, "Grilled pork with rice", null, 85_000, Course.MAIN);
    private final MenuItem tea = new MenuItem(mains, "Iced tea", null, 15_000, Course.STARTER);
    private final DiningTable table = new DiningTable(5, 4, new TableSection("Indoor"));
    private final User waiter = new User("waiter", "hash", Role.WAITER_HOST);
    private final User manager = new User("manager", "hash", Role.MANAGER);
    private final Shift shift = new Shift(manager, NOW.minusHours(3));

    // --- BR-09: bill only when the kitchen is done --------------------------------------------

    @Test
    @DisplayName("TC-BIL-01 FR-21: the bill copies every charged line and locks the order")
    void generateCopiesLinesAndLocksOrder() {
        CustomerOrder order = cookedOrder();

        Bill bill = Bill.generate(order, NOW);

        assertEquals(BillStatus.OPEN, bill.getStatus());
        assertEquals(3, bill.getLines().size());
        assertEquals("Beef pho", bill.getLines().get(0).getItemName());
        assertEquals(2, bill.getLines().get(0).getQuantity());
        assertEquals(75_000, bill.getLines().get(0).getUnitPrice());
        assertEquals(150_000 + 85_000 + 30_000, bill.getTotal());
        assertEquals(0, bill.paidAmount());
        assertEquals(bill.getTotal(), bill.remaining());
        assertEquals(OrderStatus.LOCKED, order.getStatus());
    }

    @Test
    @DisplayName("TC-BIL-02 BR-09: no bill while a dish is New or In Progress")
    void noBillWhileTheKitchenIsWorking() {
        CustomerOrder order = newOrder();
        order.addLine(pho, 1, null);
        order.addLine(rice, 1, null);
        order.sendToKitchen(order.unsentLines(), NOW);
        order.getLines().get(0).getTicketItem().changeStatus(ItemStatus.IN_PROGRESS, null, NOW);

        BusinessRuleException error = assertThrows(BusinessRuleException.class, () -> Bill.generate(order, NOW));

        assertTrue(error.getMessage().contains("Beef pho, Grilled pork with rice"), error.getMessage());
        assertEquals(OrderStatus.OPEN, order.getStatus());
    }

    @Test
    @DisplayName("TC-BIL-03 BR-09: dishes not sent to the kitchen block the bill")
    void unsentDishesBlockTheBill() {
        CustomerOrder order = cookedOrder();
        order.addLine(tea, 1, null);

        BusinessRuleException error = assertThrows(BusinessRuleException.class, () -> Bill.generate(order, NOW));

        assertTrue(error.getMessage().startsWith("Iced tea not sent"), error.getMessage());
        assertTrue(order.isEditable());
    }

    @Test
    @DisplayName("TC-BIL-04 Cancelled dishes are not charged; an order with nothing charged gets no bill")
    void cancelledDishesAreNotCharged() {
        CustomerOrder order = newOrder();
        order.addLine(pho, 1, null);
        order.addLine(rice, 1, null);
        order.sendToKitchen(order.unsentLines(), NOW);
        order.cancelLine(order.getLines().get(1), NOW);
        cook(order);

        Bill bill = Bill.generate(order, NOW);

        assertEquals(1, bill.getLines().size());
        assertEquals(75_000, bill.getTotal());

        CustomerOrder empty = newOrder();
        empty.addLine(pho, 1, null);
        empty.sendToKitchen(empty.unsentLines(), NOW);
        empty.cancelLine(empty.getLines().get(0), NOW);
        assertThrows(BusinessRuleException.class, () -> Bill.generate(empty, NOW));
    }

    @Test
    @DisplayName("TC-BIL-05 BR-09: a locked order cannot get a second bill")
    void lockedOrderGetsNoSecondBill() {
        CustomerOrder order = cookedOrder();
        Bill.generate(order, NOW);

        assertThrows(BusinessRuleException.class, () -> Bill.generate(order, NOW));
    }

    @Test
    @DisplayName("TC-BIL-06 BR-09: cancelling an unpaid bill unlocks the order; after a payment it is refused")
    void cancelOnlyBeforePayment() {
        CustomerOrder order = cookedOrder();
        Bill bill = Bill.generate(order, NOW);

        bill.cancel();

        assertEquals(BillStatus.CANCELLED, bill.getStatus());
        assertTrue(order.isEditable());
        order.addLine(tea, 1, null);  // the guests can order more

        CustomerOrder paidOrder = cookedOrder();
        Bill paid = Bill.generate(paidOrder, NOW);
        paid.pay(null, PaymentMethod.CARD, 10_000, shift, waiter, NOW);
        assertThrows(BusinessRuleException.class, paid::cancel);
        assertEquals(OrderStatus.LOCKED, paidOrder.getStatus());
    }

    // --- BR-10: even split rounding -----------------------------------------------------------

    @ParameterizedTest(name = "{0} VND in {1} shares")
    @CsvSource({
            "1000000, 3, 334000, 333000",
            "265000, 2, 133000, 132000",
            "100000, 3, 34000, 33000",
            "999999, 7, 147999, 142000",
            "45500, 20, 7500, 2000"})
    @DisplayName("TC-BIL-07 BR-10: shares are rounded down to 1,000 VND and the remainder goes on share 1")
    void evenSharesAddUpExactly(long total, int count, long first, long others) {
        long[] shares = Bill.evenShares(total, count);

        assertEquals(count, shares.length);
        assertEquals(first, shares[0]);
        for (int i = 1; i < count; i++) {
            assertEquals(others, shares[i]);
        }
        assertEquals(total, Arrays.stream(shares).sum());
    }

    @Test
    @DisplayName("TC-BIL-08 BR-10: 2 to 20 shares, and every share must be at least 1,000 VND")
    void shareCountLimits() {
        assertThrows(InvalidInputException.class, () -> Bill.evenShares(100_000, 1));
        assertThrows(InvalidInputException.class, () -> Bill.evenShares(100_000, 21));
        assertThrows(BusinessRuleException.class, () -> Bill.evenShares(15_000, 20));
    }

    @Test
    @DisplayName("TC-BIL-09 FR-23: splitting evenly creates the shares; splitting again replaces them")
    void splitEvenly() {
        Bill bill = Bill.generate(cookedOrder(), NOW);  // 265,000

        bill.splitEvenly(2);
        assertEquals(SplitType.EVEN, bill.getSplitType());
        assertArrayEquals(new long[] {133_000, 132_000}, amounts(bill));

        bill.splitEvenly(3);
        assertArrayEquals(new long[] {89_000, 88_000, 88_000}, amounts(bill));
        assertEquals(List.of(1, 2, 3), bill.getShares().stream().map(BillShare::getNumber).toList());

        bill.removeSplit();
        assertFalse(bill.isSplit());
        assertTrue(bill.getShares().isEmpty());
    }

    // --- FR-24: split by item -----------------------------------------------------------------

    @Test
    @DisplayName("TC-BIL-10 FR-24: every line goes to exactly one share; share amounts are their lines")
    void splitByItem() {
        Bill bill = Bill.generate(cookedOrder(), NOW);
        List<BillLine> lines = bill.getLines();

        bill.splitByItem(List.of(List.of(lines.get(0)), List.of(lines.get(1), lines.get(2))));

        assertEquals(SplitType.BY_ITEM, bill.getSplitType());
        assertArrayEquals(new long[] {150_000, 115_000}, amounts(bill));
        assertEquals(1, lines.get(0).getShare().getNumber());
        assertEquals(2, lines.get(2).getShare().getNumber());
    }

    @Test
    @DisplayName("TC-BIL-11 FR-24: a line left out, given twice, or an empty share is refused")
    void splitByItemNeedsEveryLineOnce() {
        Bill bill = Bill.generate(cookedOrder(), NOW);
        List<BillLine> l = bill.getLines();

        assertThrows(InvalidInputException.class, () -> bill.splitByItem(List.of(List.of(l.get(0)), List.of(l.get(1)))));
        assertThrows(InvalidInputException.class,
                () -> bill.splitByItem(List.of(List.of(l.get(0), l.get(1)), List.of(l.get(1), l.get(2)))));
        assertThrows(InvalidInputException.class, () -> bill.splitByItem(List.of(List.of(l.get(0), l.get(1), l.get(2)), List.of())));
        assertThrows(InvalidInputException.class, () -> bill.splitByItem(List.of(List.copyOf(l))));
        assertFalse(bill.isSplit());
    }

    @Test
    @DisplayName("TC-BIL-12 UC-13 1a: no split once a payment is recorded")
    void noSplitAfterPayment() {
        Bill bill = Bill.generate(cookedOrder(), NOW);
        bill.pay(null, PaymentMethod.CASH, 50_000, shift, waiter, NOW);

        assertThrows(BusinessRuleException.class, () -> bill.splitEvenly(2));
        assertThrows(BusinessRuleException.class, bill::removeSplit);
    }

    // --- BR-11: valid payments ----------------------------------------------------------------

    @Test
    @DisplayName("TC-BIL-13 BR-11: the amount must be greater than 0")
    void amountMustBePositive() {
        Bill bill = Bill.generate(cookedOrder(), NOW);

        assertThrows(InvalidInputException.class, () -> bill.pay(null, PaymentMethod.CASH, 0, shift, waiter, NOW));
        assertThrows(InvalidInputException.class, () -> bill.pay(null, PaymentMethod.CARD, -1, shift, waiter, NOW));
        assertTrue(bill.getPayments().isEmpty());
    }

    @Test
    @DisplayName("TC-BIL-14 BR-11, CR-04: card, bank transfer and e-wallet cannot exceed the amount remaining")
    void nonCashCannotExceedRemaining() {
        Bill bill = Bill.generate(cookedOrder(), NOW);  // 265,000

        for (PaymentMethod method : List.of(PaymentMethod.CARD, PaymentMethod.BANK_TRANSFER, PaymentMethod.E_WALLET)) {
            InvalidInputException error = assertThrows(InvalidInputException.class,
                    () -> bill.pay(null, method, 265_001, shift, waiter, NOW));
            assertEquals("amount", error.getField());
        }
        assertTrue(bill.getPayments().isEmpty());

        bill.pay(null, PaymentMethod.E_WALLET, 265_000, shift, waiter, NOW);
        assertEquals(BillStatus.CLOSED, bill.getStatus());
    }

    @Test
    @DisplayName("TC-BIL-15 BR-11: cash can be more than the remaining amount and the change is recorded")
    void cashRecordsChange() {
        Bill bill = Bill.generate(cookedOrder(), NOW);  // 265,000

        Payment payment = bill.pay(null, PaymentMethod.CASH, 300_000, shift, waiter, NOW);

        assertEquals(300_000, payment.getAmount());
        assertEquals(35_000, payment.getChangeAmount());
        assertEquals(265_000, payment.appliedAmount());
        assertEquals(0, bill.remaining());
    }

    @Test
    @DisplayName("TC-BIL-16 UC-14 5a: a partial payment leaves the bill open with the new remaining amount")
    void partialPaymentKeepsBillOpen() {
        CustomerOrder order = cookedOrder();
        Bill bill = Bill.generate(order, NOW);

        bill.pay(null, PaymentMethod.CARD, 100_000, shift, waiter, NOW);

        assertEquals(BillStatus.OPEN, bill.getStatus());
        assertEquals(100_000, bill.paidAmount());
        assertEquals(165_000, bill.remaining());
        assertEquals(TableStatus.OCCUPIED, table.getStatus());

        bill.pay(null, PaymentMethod.CASH, 200_000, shift, waiter, NOW.plusMinutes(1));
        assertEquals(35_000, bill.getPayments().get(1).getChangeAmount());
        assertEquals(BillStatus.CLOSED, bill.getStatus());
    }

    // --- BR-12: closed bills are final; FR-26 ---------------------------------------------------

    @Test
    @DisplayName("TC-BIL-17 FR-26: the last payment closes the bill and the order, and the table needs cleaning")
    void fullPaymentClosesBill() {
        CustomerOrder order = cookedOrder();
        Bill bill = Bill.generate(order, NOW);

        bill.pay(null, PaymentMethod.BANK_TRANSFER, 265_000, shift, waiter, NOW);

        assertEquals(BillStatus.CLOSED, bill.getStatus());
        assertEquals(NOW, bill.getClosedAt());
        assertEquals(shift, bill.getShift());
        assertEquals(OrderStatus.CLOSED, order.getStatus());
        assertEquals(TableStatus.NEEDS_CLEANING, table.getStatus());
    }

    @Test
    @DisplayName("TC-BIL-18 BR-12: a closed bill takes no payment, split or cancel")
    void closedBillIsFinal() {
        Bill bill = Bill.generate(cookedOrder(), NOW);
        bill.pay(null, PaymentMethod.CARD, 265_000, shift, waiter, NOW);

        assertThrows(BusinessRuleException.class, () -> bill.pay(null, PaymentMethod.CASH, 1_000, shift, waiter, NOW));
        assertThrows(BusinessRuleException.class, () -> bill.splitEvenly(2));
        assertThrows(BusinessRuleException.class, bill::cancel);
        assertEquals(1, bill.getPayments().size());
    }

    // --- Shares --------------------------------------------------------------------------------

    @Test
    @DisplayName("TC-BIL-19 FR-25: each share is paid on its own; the bill closes when every share is paid")
    void sharesArePaidSeparately() {
        Bill bill = Bill.generate(cookedOrder(), NOW);
        bill.splitEvenly(2);  // 133,000 + 132,000
        BillShare first = bill.getShares().get(0);
        BillShare second = bill.getShares().get(1);

        assertThrows(InvalidInputException.class, () -> bill.pay(null, PaymentMethod.CARD, 1_000, shift, waiter, NOW));
        assertThrows(InvalidInputException.class, () -> bill.pay(first, PaymentMethod.CARD, 133_001, shift, waiter, NOW));

        bill.pay(first, PaymentMethod.CARD, 133_000, shift, waiter, NOW);
        assertEquals(0, first.remaining());
        assertEquals(BillStatus.OPEN, bill.getStatus());
        assertThrows(BusinessRuleException.class, () -> bill.pay(first, PaymentMethod.CASH, 1_000, shift, waiter, NOW));

        Payment cash = bill.pay(second, PaymentMethod.CASH, 150_000, shift, waiter, NOW);
        assertEquals(18_000, cash.getChangeAmount());
        assertEquals(BillStatus.CLOSED, bill.getStatus());
    }

    // --- BR-13: shift control ------------------------------------------------------------------

    @Test
    @DisplayName("TC-BIL-20 BR-13: no payment without an open shift")
    void paymentNeedsAnOpenShift() {
        Bill bill = Bill.generate(cookedOrder(), NOW);
        Shift closed = new Shift(manager, NOW.minusHours(5));
        closed.close(manager, NOW.minusHours(1), null);

        assertThrows(BusinessRuleException.class, () -> bill.pay(null, PaymentMethod.CASH, 265_000, null, waiter, NOW));
        assertThrows(BusinessRuleException.class, () -> bill.pay(null, PaymentMethod.CASH, 265_000, closed, waiter, NOW));
        assertTrue(bill.getPayments().isEmpty());

        Payment payment = bill.pay(null, PaymentMethod.CASH, 265_000, shift, waiter, NOW);
        assertEquals(shift, payment.getShift());
    }

    @Test
    @DisplayName("TC-SHF-01 BR-13: a shift closes once; counted cash cannot be negative")
    void shiftClosesOnce() {
        Shift open = new Shift(manager, NOW.minusHours(4));
        assertTrue(open.isOpen());

        assertThrows(IllegalArgumentException.class, () -> open.close(manager, NOW, -1L));
        assertTrue(open.isOpen());

        open.close(manager, NOW, 1_500_000L);
        assertFalse(open.isOpen());
        assertEquals(1_500_000L, open.getCountedCash());
        assertEquals(manager, open.getClosedBy());
        assertThrows(BusinessRuleException.class, () -> open.close(manager, NOW, null));
    }

    @Test
    void moneyIsFormattedForMessages() {
        assertEquals("334,000 VND", Money.format(334_000));
        assertNull(Bill.generate(cookedOrder(), NOW).getClosedAt());
    }

    // --- helpers ---------------------------------------------------------------------------------

    private CustomerOrder newOrder() {
        if (table.getStatus() == TableStatus.AVAILABLE) {
            table.changeStatus(TableStatus.OCCUPIED);
        }
        return new CustomerOrder(table, waiter, NOW.minusHours(1));
    }

    /** 2 × pho 75,000 + 1 × rice 85,000 + 2 × tea 15,000 = 265,000 VND, every dish Ready. */
    private CustomerOrder cookedOrder() {
        CustomerOrder order = newOrder();
        order.addLine(pho, 2, null);
        order.addLine(rice, 1, null);
        order.addLine(tea, 2, null);
        order.sendToKitchen(order.unsentLines(), NOW.minusMinutes(50));
        cook(order);
        return order;
    }

    private static void cook(CustomerOrder order) {
        for (OrderLine line : order.getLines()) {
            KitchenTicketItem item = line.getTicketItem();
            if (item != null && item.getStatus() == ItemStatus.NEW) {
                item.changeStatus(ItemStatus.IN_PROGRESS, null, NOW.minusMinutes(40));
                item.changeStatus(ItemStatus.READY, null, NOW.minusMinutes(30));
            }
        }
    }

    private static long[] amounts(Bill bill) {
        return bill.getShares().stream().mapToLong(BillShare::getAmount).toArray();
    }
}
