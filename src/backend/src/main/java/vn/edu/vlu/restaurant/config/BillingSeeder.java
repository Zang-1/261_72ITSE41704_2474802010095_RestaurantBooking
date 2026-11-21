package vn.edu.vlu.restaurant.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;
import vn.edu.vlu.restaurant.domain.Bill;
import vn.edu.vlu.restaurant.domain.BillLine;
import vn.edu.vlu.restaurant.domain.BillShare;
import vn.edu.vlu.restaurant.domain.CustomerOrder;
import vn.edu.vlu.restaurant.domain.ItemStatusChange;
import vn.edu.vlu.restaurant.domain.OrderLine;
import vn.edu.vlu.restaurant.domain.PaymentMethod;
import vn.edu.vlu.restaurant.domain.Shift;
import vn.edu.vlu.restaurant.domain.TableStatus;
import vn.edu.vlu.restaurant.domain.User;
import vn.edu.vlu.restaurant.repository.BillRepository;
import vn.edu.vlu.restaurant.repository.ShiftRepository;

/**
 * Pays the sample orders of {@link OrderSeeder} through the real Billing domain methods, so the shift
 * reports have data: one lunch and one dinner shift per day, bills paid in cash (with change), card,
 * bank transfer and e-wallet, some split evenly or by item. Each table is replayed through its lifecycle
 * (Available → Occupied → paid, so Needs cleaning → Available), so the tables end as they started.
 */
@Component
public class BillingSeeder {

    private static final PaymentMethod[] METHODS = PaymentMethod.values();
    private static final LocalTime LUNCH_OPENS = LocalTime.of(10, 0);
    private static final LocalTime LUNCH_CLOSES = LocalTime.of(14, 30);
    private static final LocalTime DINNER_OPENS = LocalTime.of(17, 0);
    private static final LocalTime DINNER_CLOSES = LocalTime.of(22, 30);

    private final BillRepository billRepository;
    private final ShiftRepository shiftRepository;

    public BillingSeeder(BillRepository billRepository, ShiftRepository shiftRepository) {
        this.billRepository = billRepository;
        this.shiftRepository = shiftRepository;
    }

    /** The orders must be finished in the kitchen and still open, i.e. not billed yet. */
    public void settle(List<CustomerOrder> orders, User waiter, User manager) {
        Map<LocalDateTime, List<CustomerOrder>> byShift = new TreeMap<>();
        for (CustomerOrder order : orders) {
            byShift.computeIfAbsent(shiftStart(order.getCreatedAt()), k -> new ArrayList<>()).add(order);
        }
        int count = 0;
        int shiftNumber = 0;
        for (var entry : byShift.entrySet()) {
            Shift shift = shiftRepository.saveAndFlush(new Shift(manager, entry.getKey()));
            long cash = 0;
            List<CustomerOrder> shiftOrders = entry.getValue().stream()
                    .sorted(Comparator.comparing(CustomerOrder::getCreatedAt)).toList();
            for (CustomerOrder order : shiftOrders) {
                cash += settle(order, shift, waiter, count++);
            }
            LocalDateTime closes = entry.getKey().toLocalDate()
                    .atTime(entry.getKey().toLocalTime().equals(LUNCH_OPENS) ? LUNCH_CLOSES : DINNER_CLOSES);
            // One shift was 20,000 VND short in the till, to show the difference on the report (FR-29)
            long counted = shiftNumber++ == 1 ? Math.max(0, cash - 20_000) : cash;
            shift.close(manager, closes, counted);
            shiftRepository.saveAndFlush(shift);
        }
    }

    /** Generates and pays one bill; returns the cash kept in the till. */
    private long settle(CustomerOrder order, Shift shift, User waiter, int index) {
        LocalDateTime lastReady = order.getLines().stream()
                .map(OrderLine::getTicketItem)
                .flatMap(item -> item.getHistory().stream())
                .map(ItemStatusChange::getChangedAt)
                .max(Comparator.naturalOrder())
                .orElse(order.getCreatedAt());
        LocalDateTime billed = lastReady.plusMinutes(25);
        order.getTable().changeStatus(TableStatus.OCCUPIED);
        Bill bill = Bill.generate(order, billed);

        long cash = 0;
        LocalDateTime paidAt = billed.plusMinutes(3);
        if (index % 4 == 1) {
            bill.splitEvenly(2 + index % 3);
        } else if (index % 4 == 2) {
            List<BillLine> lines = bill.getLines();
            bill.splitByItem(List.of(lines.subList(0, 1), lines.subList(1, lines.size())));
        }
        if (bill.isSplit()) {
            for (BillShare share : bill.getShares()) {
                PaymentMethod method = METHODS[(index + share.getNumber()) % METHODS.length];
                cash += pay(bill, share, method, share.remaining(), shift, waiter, paidAt);
                paidAt = paidAt.plusMinutes(1);
            }
        } else {
            cash += pay(bill, null, METHODS[index % METHODS.length], bill.remaining(), shift, waiter, paidAt);
        }
        order.getTable().changeStatus(TableStatus.AVAILABLE);
        billRepository.save(bill);
        return cash;
    }

    /** Cash is handed over rounded up to the next 50,000 VND, so the change is recorded (BR-11). */
    private long pay(Bill bill, BillShare share, PaymentMethod method, long due, Shift shift, User waiter,
                     LocalDateTime at) {
        long handed = method == PaymentMethod.CASH ? (due + 49_999) / 50_000 * 50_000 : due;
        bill.pay(share, method, handed, shift, waiter, at);
        return method == PaymentMethod.CASH ? due : 0;
    }

    private static LocalDateTime shiftStart(LocalDateTime orderTime) {
        LocalDate day = orderTime.toLocalDate();
        return day.atTime(orderTime.toLocalTime().isBefore(DINNER_OPENS) ? LUNCH_OPENS : DINNER_OPENS);
    }
}
