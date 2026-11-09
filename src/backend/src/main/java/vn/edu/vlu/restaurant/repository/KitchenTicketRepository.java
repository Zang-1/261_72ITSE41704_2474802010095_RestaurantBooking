package vn.edu.vlu.restaurant.repository;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.vlu.restaurant.domain.ItemStatus;
import vn.edu.vlu.restaurant.domain.KitchenTicket;
import vn.edu.vlu.restaurant.domain.OrderStatus;

public interface KitchenTicketRepository extends JpaRepository<KitchenTicket, Long> {

    /** Tickets that still have a dish with one of the given statuses, oldest first (UC-09 step 1). */
    @Query("select distinct t from KitchenTicket t join t.items i where i.status in :statuses "
            + "order by t.sentAt asc, t.id asc")
    List<KitchenTicket> findWithItemStatusIn(@Param("statuses") Collection<ItemStatus> statuses);

    List<KitchenTicket> findByOrderStatusOrderBySentAtDescIdDesc(OrderStatus status);
}
