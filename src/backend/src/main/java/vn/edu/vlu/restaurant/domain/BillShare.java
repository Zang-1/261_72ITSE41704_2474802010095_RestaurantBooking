package vn.edu.vlu.restaurant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** The part of a split bill that one guest pays (FR-23, FR-24). Shares are numbered from 1. */
@Entity
@Table(name = "bill_share")
public class BillShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @Column(nullable = false)
    private int number;

    @Column(nullable = false)
    private long amount;

    protected BillShare() {
    }

    BillShare(Bill bill, int number, long amount) {
        this.bill = bill;
        this.number = number;
        this.amount = amount;
    }

    /** What has been paid towards this share, without the change given back. */
    public long paidAmount() {
        return bill.getPayments().stream()
                .filter(payment -> payment.getShare() == this)
                .mapToLong(Payment::appliedAmount)
                .sum();
    }

    public long remaining() {
        return amount - paidAmount();
    }

    public Long getId() {
        return id;
    }

    public Bill getBill() {
        return bill;
    }

    public int getNumber() {
        return number;
    }

    public long getAmount() {
        return amount;
    }
}
