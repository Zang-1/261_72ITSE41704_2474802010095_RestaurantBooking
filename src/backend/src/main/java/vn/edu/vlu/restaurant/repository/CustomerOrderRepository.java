package vn.edu.vlu.restaurant.repository;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.vlu.restaurant.domain.CustomerOrder;
import vn.edu.vlu.restaurant.domain.OrderStatus;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    List<CustomerOrder> findByTableIdAndStatus(Long tableId, OrderStatus status);

    List<CustomerOrder> findByTableIdAndStatusInOrderByCreatedAtAsc(Long tableId, Collection<OrderStatus> statuses);

    boolean existsByTableIdAndStatusIn(Long tableId, Collection<OrderStatus> statuses);

    /** SELECT … FOR UPDATE: a double click on Send is handled one request after the other (NFR-07). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.id = :id")
    Optional<CustomerOrder> findByIdForUpdate(@Param("id") Long id);
}
