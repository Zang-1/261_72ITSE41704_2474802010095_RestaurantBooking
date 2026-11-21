package vn.edu.vlu.restaurant.dto;

import vn.edu.vlu.restaurant.domain.BillLine;

/** shareNumber is set only when the bill is split by item. */
public record BillLineResponse(Long id, String itemName, int quantity, long unitPrice, long lineTotal, Integer shareNumber) {

    public static BillLineResponse from(BillLine line) {
        return new BillLineResponse(line.getId(), line.getItemName(), line.getQuantity(), line.getUnitPrice(),
                line.lineTotal(), line.getShare() == null ? null : line.getShare().getNumber());
    }
}
