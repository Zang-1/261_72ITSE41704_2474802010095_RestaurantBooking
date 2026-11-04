package vn.edu.vlu.restaurant.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.vlu.restaurant.domain.ItemStatus;
import vn.edu.vlu.restaurant.domain.KitchenTicketItem;
import vn.edu.vlu.restaurant.dto.KitchenTicketItemResponse;
import vn.edu.vlu.restaurant.dto.KitchenTicketResponse;
import vn.edu.vlu.restaurant.exception.NotFoundException;
import vn.edu.vlu.restaurant.repository.KitchenTicketItemRepository;
import vn.edu.vlu.restaurant.repository.KitchenTicketRepository;

/** UC-09 Process kitchen ticket: the kitchen display and the item status changes (FR-15). */
@Service
@Transactional
public class KitchenService {

    private final KitchenTicketRepository ticketRepository;
    private final KitchenTicketItemRepository itemRepository;
    private final Clock clock;

    public KitchenService(KitchenTicketRepository ticketRepository, KitchenTicketItemRepository itemRepository,
                          Clock clock) {
        this.ticketRepository = ticketRepository;
        this.itemRepository = itemRepository;
        this.clock = clock;
    }

    /** Tickets with a dish still New or In Progress, oldest first; items in course order (BR-06). */
    @Transactional(readOnly = true)
    public List<KitchenTicketResponse> listActiveTickets() {
        return ticketRepository.findWithItemStatusIn(EnumSet.of(ItemStatus.NEW, ItemStatus.IN_PROGRESS)).stream()
                .map(KitchenTicketResponse::from).toList();
    }

    /** Cooking starts; from now on the waiter can no longer change or cancel the dish (BR-08). */
    public KitchenTicketItemResponse start(Long itemId) {
        return changeStatus(itemId, ItemStatus.IN_PROGRESS, null);
    }

    public KitchenTicketItemResponse markReady(Long itemId) {
        return changeStatus(itemId, ItemStatus.READY, null);
    }

    /** The guest sent the dish back: Ready → In Progress, with the reason kept in the history. */
    public KitchenTicketItemResponse sendBack(Long itemId, String reason) {
        return changeStatus(itemId, ItemStatus.IN_PROGRESS, reason);
    }

    private KitchenTicketItemResponse changeStatus(Long itemId, ItemStatus target, String reason) {
        KitchenTicketItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Ticket item not found"));
        item.changeStatus(target, reason, LocalDateTime.now(clock));
        itemRepository.flush();  // a waiter cancelling the dish at the same moment fails here (optimistic lock)
        return KitchenTicketItemResponse.from(item);
    }
}
