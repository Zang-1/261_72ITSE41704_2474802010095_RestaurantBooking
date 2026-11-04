package vn.edu.vlu.restaurant.config;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.vlu.restaurant.domain.Course;
import vn.edu.vlu.restaurant.domain.CustomerOrder;
import vn.edu.vlu.restaurant.domain.DiningTable;
import vn.edu.vlu.restaurant.domain.ItemStatus;
import vn.edu.vlu.restaurant.domain.KitchenTicket;
import vn.edu.vlu.restaurant.domain.KitchenTicketItem;
import vn.edu.vlu.restaurant.domain.MenuItem;
import vn.edu.vlu.restaurant.domain.OrderLine;
import vn.edu.vlu.restaurant.domain.User;
import vn.edu.vlu.restaurant.repository.CustomerOrderRepository;
import vn.edu.vlu.restaurant.repository.DiningTableRepository;
import vn.edu.vlu.restaurant.repository.MenuItemRepository;
import vn.edu.vlu.restaurant.repository.UserRepository;

/**
 * Creates 12 finished orders from the past week (lunch and dinner) so the kitchen history and
 * reports have data. Every order goes through the real domain methods, so the sample data follows
 * BR-06 (course order), BR-07 and BR-08. Runs after the other seeders, only when there are no orders.
 * The orders are closed without a bill because Billing (Khoa) is built later.
 */
@Component
@ConditionalOnProperty(name = "app.seed.sample-orders", havingValue = "true")
public class OrderSeeder {

    private static final Logger log = LoggerFactory.getLogger(OrderSeeder.class);
    private static final int ORDERS = 12;
    private static final String[] NOTES = {"no onion", "less spicy", "no ice", "extra herbs", "sauce on the side"};

    private final CustomerOrderRepository orderRepository;
    private final DiningTableRepository tableRepository;
    private final MenuItemRepository itemRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    /** Fixed seed: every fresh database gets the same sample orders. */
    private final Random random = new Random(2026);

    public OrderSeeder(CustomerOrderRepository orderRepository, DiningTableRepository tableRepository,
                       MenuItemRepository itemRepository, UserRepository userRepository, Clock clock) {
        this.orderRepository = orderRepository;
        this.tableRepository = tableRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        List<DiningTable> tables = tableRepository.findByActiveTrueOrderByNumberAsc();
        List<MenuItem> menu = itemRepository.search(false, true);
        User waiter = userRepository.findByUsername("waiter").orElse(null);
        if (orderRepository.count() > 0 || tables.isEmpty() || menu.isEmpty() || waiter == null) {
            return;
        }
        List<MenuItem> drinks = menu.stream().filter(i -> "Drinks".equals(i.getCategory().getName())).toList();
        List<MenuItem> starters = menu.stream()
                .filter(i -> i.getCourse() == Course.STARTER && !drinks.contains(i)).toList();
        List<MenuItem> mains = menu.stream().filter(i -> i.getCourse() == Course.MAIN).toList();
        List<MenuItem> desserts = menu.stream().filter(i -> i.getCourse() == Course.DESSERT).toList();

        for (int i = 0; i < ORDERS; i++) {
            LocalDateTime opened = LocalDate.now(clock).minusDays(1 + i % 7)
                    .atTime(i % 2 == 0 ? 12 : 19, (i * 7) % 45);
            CustomerOrder order = new CustomerOrder(tables.get((i * 5) % tables.size()), waiter, opened);

            add(order, pick(drinks));
            OrderLine starter = add(order, pick(starters));
            add(order, pick(mains));
            OrderLine secondMain = i % 3 == 1 ? add(order, pick(mains)) : null;
            if (i % 3 == 0) {
                add(order, pick(desserts));
            }

            // Half of the tables send the starters first and the rest 15 minutes later (BR-06)
            LocalDateTime sent = opened.plusMinutes(5);
            if (i % 2 == 0) {
                order.sendToKitchen(order.unsentLines().stream()
                        .filter(l -> l.getMenuItem().getCourse() == Course.STARTER).toList(), sent);
                order.sendToKitchen(order.unsentLines(), sent.plusMinutes(15));
            } else {
                order.sendToKitchen(order.unsentLines(), sent);
            }
            if (i == 7 && secondMain != null) {
                order.cancelLine(secondMain, sent.plusMinutes(1));  // cancelled before cooking (BR-08)
            }
            for (KitchenTicket ticket : order.getTickets()) {
                cook(ticket);
            }
            if (i == 3) {  // a dish sent back by the guest (UC-09 alternative 3a)
                KitchenTicketItem item = starter.getTicketItem();
                LocalDateTime last = item.getHistory().get(item.getHistory().size() - 1).getChangedAt();
                item.changeStatus(ItemStatus.IN_PROGRESS, "Guest found it too salty", last.plusMinutes(3));
                item.changeStatus(ItemStatus.READY, null, last.plusMinutes(10));
            }
            order.lock();
            order.close();
            orderRepository.save(order);
        }
        log.info("Seeded {} past orders", ORDERS);
    }

    private OrderLine add(CustomerOrder order, MenuItem item) {
        String note = random.nextInt(4) == 0 ? NOTES[random.nextInt(NOTES.length)] : null;
        return order.addLine(item, 1 + random.nextInt(3), note);
    }

    /** Every dish still New is started 2-4 minutes after sending and ready 6-15 minutes later. */
    private void cook(KitchenTicket ticket) {
        for (KitchenTicketItem item : ticket.getItems()) {
            if (item.getStatus() != ItemStatus.NEW) {
                continue;
            }
            LocalDateTime started = ticket.getSentAt().plusMinutes(2 + random.nextInt(3));
            item.changeStatus(ItemStatus.IN_PROGRESS, null, started);
            item.changeStatus(ItemStatus.READY, null, started.plusMinutes(6 + random.nextInt(10)));
        }
    }

    private MenuItem pick(List<MenuItem> items) {
        return items.get(random.nextInt(items.size()));
    }
}
