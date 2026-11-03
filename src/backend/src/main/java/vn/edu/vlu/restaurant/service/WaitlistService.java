package vn.edu.vlu.restaurant.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.vlu.restaurant.domain.DiningTable;
import vn.edu.vlu.restaurant.domain.WaitlistEntry;
import vn.edu.vlu.restaurant.dto.SeatedPartyResponse;
import vn.edu.vlu.restaurant.dto.WaitlistEntryResponse;
import vn.edu.vlu.restaurant.dto.WaitlistRequest;
import vn.edu.vlu.restaurant.exception.BusinessRuleException;
import vn.edu.vlu.restaurant.exception.NotFoundException;
import vn.edu.vlu.restaurant.repository.WaitlistEntryRepository;

/** Walk-in waiting list (FR-07, FR-08, UC-05) and seating in arrival order (BR-04). */
@Service
@Transactional
public class WaitlistService {

    private final WaitlistEntryRepository waitlistRepository;
    private final TableService tableService;
    private final Clock clock;

    public WaitlistService(WaitlistEntryRepository waitlistRepository, TableService tableService, Clock clock) {
        this.waitlistRepository = waitlistRepository;
        this.tableService = tableService;
        this.clock = clock;
    }

    /** BR-04: seats the earliest waiting party that fits the table; a later party goes first only if no earlier one fits. */
    public SeatedPartyResponse seatNext(Long tableId) {
        DiningTable table = tableService.lockTable(tableId);
        WaitlistEntry entry = waitlistRepository.findBySeatedTableIsNullOrderByArrivedAtAscIdAsc().stream()
                .filter(e -> table.canSeat(e.getPartySize()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("No waiting party fits table " + table.getNumber()));
        tableService.seatParty(table, entry.getPartySize());
        entry.seat(table);
        return new SeatedPartyResponse(entry.getPartyName(), entry.getPartySize(), table.getNumber());
    }

    /** Parties still waiting, numbered 1, 2, 3 ... in arrival order. */
    @Transactional(readOnly = true)
    public List<WaitlistEntryResponse> listWaiting() {
        List<WaitlistEntry> waiting = waitlistRepository.findBySeatedTableIsNullOrderByArrivedAtAscIdAsc();
        List<WaitlistEntryResponse> result = new ArrayList<>();
        for (int i = 0; i < waiting.size(); i++) {
            result.add(WaitlistEntryResponse.from(waiting.get(i), i + 1));
        }
        return result;
    }

    /** Adds the party and returns it with its position in the queue. */
    public WaitlistEntryResponse add(WaitlistRequest request) {
        WaitlistEntry entry = waitlistRepository.save(new WaitlistEntry(
                request.partyName().trim(), request.phone(), request.partySize(), LocalDateTime.now(clock)));
        List<WaitlistEntryResponse> waiting = listWaiting();
        return waiting.stream()
                .filter(e -> e.id().equals(entry.getId()))
                .findFirst()
                .orElseThrow();
    }

    /** The party left before being seated (UC-05 alternative 5a); later parties move up. */
    public void remove(Long id) {
        WaitlistEntry entry = waitlistRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Waiting-list entry not found"));
        if (!entry.isWaiting()) {
            throw new BusinessRuleException("This party has already been seated");
        }
        waitlistRepository.delete(entry);
    }
}
