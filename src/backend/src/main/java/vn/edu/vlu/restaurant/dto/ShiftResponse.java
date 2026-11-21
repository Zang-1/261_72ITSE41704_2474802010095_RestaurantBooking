package vn.edu.vlu.restaurant.dto;

import java.time.LocalDateTime;
import vn.edu.vlu.restaurant.domain.Shift;

public record ShiftResponse(Long id, LocalDateTime openedAt, LocalDateTime closedAt, String openedBy,
                            String closedBy, boolean open, Long countedCash) {

    public static ShiftResponse from(Shift shift) {
        return new ShiftResponse(shift.getId(), shift.getOpenedAt(), shift.getClosedAt(),
                shift.getOpenedBy().getUsername(),
                shift.getClosedBy() == null ? null : shift.getClosedBy().getUsername(),
                shift.isOpen(), shift.getCountedCash());
    }
}
