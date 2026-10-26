package vn.edu.vlu.restaurant.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.vlu.restaurant.domain.CustomerOrder;
import vn.edu.vlu.restaurant.domain.DiningTable;
import vn.edu.vlu.restaurant.domain.MenuItem;
import vn.edu.vlu.restaurant.domain.OrderLine;
import vn.edu.vlu.restaurant.domain.OrderStatus;
import vn.edu.vlu.restaurant.domain.User;

/** Checks that the V2 migration and the order entities agree (docs/diagrams/lam/04-erd-2-order-menu.png). */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderPersistenceTest {

    @Autowired
    private CustomerOrderRepository orderRepository;

    @Autowired
    private DiningTableRepository tableRepository;

    @Autowired
    private MenuItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("An order with lines is saved and read back with its lines and copied prices")
    void saveOrderWithLines() {
        DiningTable table = tableRepository.findAllByOrderByNumberAsc().get(0);
        User waiter = userRepository.findByUsername("waiter").orElseThrow();
        List<MenuItem> items = itemRepository.search(false, false);

        CustomerOrder order = new CustomerOrder(table, waiter, LocalDateTime.of(2026, 10, 26, 19, 0));
        order.addLine(items.get(0), 2, "extra spicy");
        order.addLine(items.get(1), 1, null);
        orderRepository.saveAndFlush(order);

        CustomerOrder saved = orderRepository.findByTableIdAndStatus(table.getId(), OrderStatus.OPEN).get(0);
        assertEquals(2, saved.getLines().size());
        OrderLine first = saved.getLines().get(0);
        assertNotNull(first.getId());
        assertEquals(items.get(0).getPrice(), first.getUnitPrice());
        assertEquals("extra spicy", first.getNote());
    }
}
