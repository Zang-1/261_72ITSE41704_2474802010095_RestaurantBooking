package vn.edu.vlu.restaurant.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.vlu.restaurant.domain.CustomerOrder;
import vn.edu.vlu.restaurant.domain.OrderStatus;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    List<CustomerOrder> findByTableIdAndStatus(Long tableId, OrderStatus status);
}
