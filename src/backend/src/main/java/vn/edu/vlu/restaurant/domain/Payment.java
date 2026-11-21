package vn.edu.vlu.restaurant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Money received for a bill or one of its shares (FR-25). amount is what the guest handed over and
 * changeAmount what was given back, so the part that pays the bill is amount - changeAmount.
 * Created only by {@link Bill#pay}, which applies BR-11 to BR-13.
 */
@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    /** Null when the payment is for the whole bill. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "share_id")
    private BillShare share;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Column(nullable = false)
    private long amount;

    @Column(name = "change_amount", nullable = false)
    private long changeAmount;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by_id", nullable = false)
    private User recordedBy;

    protected Payment() {
    }

    Payment(Bill bill, BillShare share, Shift shift, PaymentMethod method, long amount, long changeAmount,
            LocalDateTime paidAt, User recordedBy) {
        this.bill = bill;
        this.share = share;
        this.shift = shift;
        this.method = method;
        this.amount = amount;
        this.changeAmount = changeAmount;
        this.paidAt = paidAt;
        this.recordedBy = recordedBy;
    }

    /** The part of the payment that counts towards the bill and the shift's revenue. */
    public long appliedAmount() {
        return amount - changeAmount;
    }

    public Long getId() {
        return id;
    }

    public Bill getBill() {
        return bill;
    }

    public BillShare getShare() {
        return share;
    }

    public Shift getShift() {
        return shift;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public long getAmount() {
        return amount;
    }

    public long getChangeAmount() {
        return changeAmount;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public User getRecordedBy() {
        return recordedBy;
    }
}
