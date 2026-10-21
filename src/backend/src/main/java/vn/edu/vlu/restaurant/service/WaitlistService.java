package vn.edu.vlu.restaurant.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.vlu.restaurant.domain.WaitlistEntry;
import vn.edu.vlu.restaurant.dto.WaitlistEntryResponse;
import vn.edu.vlu.restaurant.dto.WaitlistRequest;
import vn.edu.vlu.restaurant.exception.BusinessRuleException;
import vn.edu.vlu.restaurant.exception.NotFoundException;
import vn.edu.vlu.restaurant.repository.WaitlistEntryRepository;

/**
 * Walk-in waiting list (FR-07, UC-05). Seating the first party that fits (FR-08, BR-04) is added in
 * Week 7 together with the table lifecycle.
 */
@Service
@Transactional
public class WaitlistService {

    private final WaitlistEntryRepository waitlistRepository;
    private final Clock clock;

    public WaitlistService(WaitlistEntryRepository waitlistRepository, Clock clock) {
        this.waitlistRepository = waitlistRepository;
        this.clock = clock;
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
