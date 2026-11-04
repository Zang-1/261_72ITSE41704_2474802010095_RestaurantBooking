package vn.edu.vlu.restaurant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.vlu.restaurant.domain.KitchenTicketItem;

public interface KitchenTicketItemRepository extends JpaRepository<KitchenTicketItem, Long> {
}
