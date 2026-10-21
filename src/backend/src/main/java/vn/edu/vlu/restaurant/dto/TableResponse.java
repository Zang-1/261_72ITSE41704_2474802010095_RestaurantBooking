package vn.edu.vlu.restaurant.dto;

import vn.edu.vlu.restaurant.domain.DiningTable;
import vn.edu.vlu.restaurant.domain.TableStatus;

public record TableResponse(Long id, int number, int capacity, Long sectionId, String sectionName,
                            TableStatus status, boolean active) {

    public static TableResponse from(DiningTable table) {
        return new TableResponse(table.getId(), table.getNumber(), table.getCapacity(),
                table.getSection().getId(), table.getSection().getName(), table.getStatus(), table.isActive());
    }
}
