package vn.edu.vlu.restaurant.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import vn.edu.vlu.restaurant.domain.CustomerOrder;
import vn.edu.vlu.restaurant.domain.DiningTable;
import vn.edu.vlu.restaurant.domain.ItemStatus;
import vn.edu.vlu.restaurant.domain.KitchenTicketItem;
import vn.edu.vlu.restaurant.domain.MenuItem;
import vn.edu.vlu.restaurant.domain.OrderLine;
import vn.edu.vlu.restaurant.domain.Shift;
import vn.edu.vlu.restaurant.domain.TableStatus;
import vn.edu.vlu.restaurant.domain.User;
import vn.edu.vlu.restaurant.repository.CustomerOrderRepository;
import vn.edu.vlu.restaurant.repository.DiningTableRepository;
import vn.edu.vlu.restaurant.repository.MenuItemRepository;
import vn.edu.vlu.restaurant.repository.ShiftRepository;
import vn.edu.vlu.restaurant.repository.UserRepository;

/** Builds the state the billing API needs: an occupied table with a cooked order, and an open shift. */
abstract class BillingTestSupport extends ApiTestSupport {

    protected static final RequestPostProcessor WAITER = user("waiter").roles("WAITER_HOST");
    protected static final RequestPostProcessor MANAGER = user("manager").roles("MANAGER");
    protected static final RequestPostProcessor KITCHEN = user("kitchen").roles("KITCHEN_STAFF");

    @Autowired
    protected DiningTableRepository tableRepository;
    @Autowired
    protected MenuItemRepository itemRepository;
    @Autowired
    protected CustomerOrderRepository orderRepository;
    @Autowired
    protected ShiftRepository shiftRepository;
    @Autowired
    protected UserRepository userRepository;

    private int nextTable = 0;

    protected Shift openShift() {
        return shiftRepository.saveAndFlush(new Shift(staffUser("manager"), LocalDateTime.now().minusHours(2)));
    }

    /** An Occupied table whose order has the given dishes (quantity 1 each, unless repeated), all Ready. */
    protected CustomerOrder cookedOrder(int dishes) {
        CustomerOrder order = orderWithSentDishes(dishes);
        for (OrderLine line : order.getLines()) {
            KitchenTicketItem item = line.getTicketItem();
            item.changeStatus(ItemStatus.IN_PROGRESS, null, LocalDateTime.now().minusMinutes(20));
            item.changeStatus(ItemStatus.READY, null, LocalDateTime.now().minusMinutes(10));
        }
        return orderRepository.saveAndFlush(order);
    }

    /** The dishes are sent but still New in the kitchen. */
    protected CustomerOrder orderWithSentDishes(int dishes) {
        List<DiningTable> free = tableRepository.findByActiveTrueOrderByNumberAsc().stream()
                .filter(t -> t.getStatus() == TableStatus.AVAILABLE).toList();
        DiningTable table = free.get(nextTable++);
        table.changeStatus(TableStatus.OCCUPIED);
        List<MenuItem> items = itemRepository.findAll().stream().filter(MenuItem::isOrderable).toList();
        CustomerOrder order = new CustomerOrder(table, staffUser("waiter"), LocalDateTime.now().minusHours(1));
        for (int i = 0; i < dishes; i++) {
            order.addLine(items.get(i % items.size()), 1, null);
        }
        order.sendToKitchen(order.unsentLines(), LocalDateTime.now().minusMinutes(30));
        return orderRepository.saveAndFlush(order);
    }

    protected long generateBill(CustomerOrder order) throws Exception {
        String body = mockMvc.perform(post("/api/bills").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("orderId", order.getId()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    protected String pay(long billId, String method, long amount) throws Exception {
        return mockMvc.perform(post("/api/bills/" + billId + "/payments").with(WAITER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("method", method, "amount", amount))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    protected User staffUser(String username) {
        return userRepository.findByUsername(username).orElseThrow();
    }
}
