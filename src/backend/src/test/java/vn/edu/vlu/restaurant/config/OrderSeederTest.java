package vn.edu.vlu.restaurant.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.vlu.restaurant.domain.Course;
import vn.edu.vlu.restaurant.domain.CustomerOrder;
import vn.edu.vlu.restaurant.domain.ItemStatus;
import vn.edu.vlu.restaurant.domain.KitchenTicket;
import vn.edu.vlu.restaurant.domain.KitchenTicketItem;
import vn.edu.vlu.restaurant.domain.OrderLine;
import vn.edu.vlu.restaurant.domain.OrderStatus;
import vn.edu.vlu.restaurant.domain.TableStatus;
import vn.edu.vlu.restaurant.repository.CustomerOrderRepository;
import vn.edu.vlu.restaurant.repository.DiningTableRepository;
import vn.edu.vlu.restaurant.repository.KitchenTicketRepository;
import vn.edu.vlu.restaurant.repository.MenuItemRepository;

/**
 * The sample data of the Order & Kitchen module follows the module's own rules. Uses its own
 * in-memory database so the seeded orders do not show up in the other API tests.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:order-seed-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "app.seed.sample-orders=true"})
@ActiveProfiles("test")
@Transactional
class OrderSeederTest {

    @Autowired
    private CustomerOrderRepository orderRepository;

    @Autowired
    private KitchenTicketRepository ticketRepository;

    @Autowired
    private MenuItemRepository itemRepository;

    @Autowired
    private DiningTableRepository tableRepository;

    @Test
    void seedsBetween50And200RecordsOfMenuAndOrders() {
        List<CustomerOrder> orders = orderRepository.findAll();
        long lines = orders.stream().mapToLong(o -> o.getLines().size()).sum();
        long records = itemRepository.count() + orders.size() + lines + ticketRepository.count();

        assertTrue(itemRepository.count() >= 25, "menu items: " + itemRepository.count());
        assertTrue(orders.size() >= 10, "orders: " + orders.size());
        assertTrue(records >= 50 && records <= 200, "records: " + records);
    }

    @Test
    void pastOrdersAreClosedAndTheKitchenIsDone() {
        for (CustomerOrder order : orderRepository.findAll()) {
            assertEquals(OrderStatus.CLOSED, order.getStatus());
            assertTrue(order.unsentLines().isEmpty());
            for (OrderLine line : order.getLines()) {
                ItemStatus status = line.getTicketItem().getStatus();
                assertTrue(status == ItemStatus.READY || status == ItemStatus.CANCELLED, status.name());
            }
        }
        assertTrue(ticketRepository.findWithItemStatusIn(List.of(ItemStatus.NEW, ItemStatus.IN_PROGRESS)).isEmpty());
    }

    @Test
    void ticketsKeepCourseOrderAndHistory() {
        for (KitchenTicket ticket : ticketRepository.findAll()) {
            List<KitchenTicketItem> items = ticket.getItems();
            for (int i = 1; i < items.size(); i++) {
                assertTrue(course(items.get(i - 1)).compareTo(course(items.get(i))) <= 0, "BR-06 on ticket " + ticket.getId());
            }
            for (KitchenTicketItem item : items) {
                assertEquals(ItemStatus.NEW, item.getHistory().get(0).getStatus());
            }
        }
    }

    @Test
    void tablesAreLeftAsTheyWere() {
        assertTrue(tableRepository.findAll().stream().allMatch(t -> t.getStatus() == TableStatus.AVAILABLE));
    }

    private static Course course(KitchenTicketItem item) {
        return item.getOrderLine().getMenuItem().getCourse();
    }
}
