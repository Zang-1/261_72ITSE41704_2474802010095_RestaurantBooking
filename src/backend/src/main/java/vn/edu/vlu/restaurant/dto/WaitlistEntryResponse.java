package vn.edu.vlu.restaurant.dto;

import java.time.LocalDateTime;
import vn.edu.vlu.restaurant.domain.WaitlistEntry;

/** A waiting party with its 1-based position in the queue. */
public record WaitlistEntryResponse(Long id, int position, String partyName, String phone, int partySize,
                                    LocalDateTime arrivedAt) {

    public static WaitlistEntryResponse from(WaitlistEntry entry, int position) {
        return new WaitlistEntryResponse(entry.getId(), position, entry.getPartyName(), entry.getPhone(),
                entry.getPartySize(), entry.getArrivedAt());
    }
}
