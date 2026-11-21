package vn.edu.vlu.restaurant.dto;

import vn.edu.vlu.restaurant.domain.BillShare;

public record BillShareResponse(Long id, int number, long amount, long paid, long remaining) {

    public static BillShareResponse from(BillShare share) {
        long paid = share.paidAmount();
        return new BillShareResponse(share.getId(), share.getNumber(), share.getAmount(), paid, share.getAmount() - paid);
    }
}
