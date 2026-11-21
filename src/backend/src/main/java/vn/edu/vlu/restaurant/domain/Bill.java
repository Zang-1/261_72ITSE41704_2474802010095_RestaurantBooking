package vn.edu.vlu.restaurant.domain;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import vn.edu.vlu.restaurant.exception.BusinessRuleException;
import vn.edu.vlu.restaurant.exception.InvalidInputException;

/**
 * The bill of one order (FR-21 to FR-26). Every business rule of the module that needs only the bill
 * itself lives here, so it can be unit tested without a database:
 * BR-09 in {@link #generate} and {@link #cancel}, BR-10 in {@link #splitEvenly}, BR-11 and BR-13 in
 * {@link #pay}, BR-12 in {@link #requireOpen}. State machine: docs/diagrams/khoa/08-state-5-bill.png.
 */
@Entity
@Table(name = "bill")
public class Bill {

    public static final int MIN_SHARES = 2;
    public static final int MAX_SHARES = 20;
    /** Even shares are rounded down to whole thousands of VND (BR-10). */
    public static final long ROUNDING_UNIT = 1_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    @Column(nullable = false)
    private long total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillStatus status = BillStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false, length = 20)
    private SplitType splitType = SplitType.NONE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    /** The shift in which the bill was closed; the shift report counts the bill there (FR-28). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    private Shift shift;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<BillLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("number ASC")
    private List<BillShare> shares = new ArrayList<>();

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL)
    @OrderBy("paidAt ASC, id ASC")
    private List<Payment> payments = new ArrayList<>();

    protected Bill() {
    }

    private Bill(CustomerOrder order, LocalDateTime createdAt) {
        this.order = order;
        this.createdAt = createdAt;
    }

    /**
     * Generates the bill of an order (FR-21) and locks the order (BR-09). Refused while the kitchen is
     * still working on a dish, while a dish has not been sent, or when nothing is charged. Cancelled
     * dishes are not copied, because they cost nothing.
     */
    public static Bill generate(CustomerOrder order, LocalDateTime at) {
        if (!order.isEditable()) {
            throw new BusinessRuleException("Table " + order.getTable().getNumber() + " already has a bill");
        }
        List<String> pending = order.getLines().stream()
                .filter(line -> line.isSent() && line.getTicketItem().isPending())
                .map(line -> line.getMenuItem().getName())
                .toList();
        if (!pending.isEmpty()) {
            throw new BusinessRuleException("The kitchen is still preparing " + String.join(", ", pending)
                    + "; the bill can be generated when every dish is ready (BR-09)");
        }
        List<String> unsent = order.unsentLines().stream().map(line -> line.getMenuItem().getName()).toList();
        if (!unsent.isEmpty()) {
            throw new BusinessRuleException(String.join(", ", unsent)
                    + " not sent to the kitchen yet; send or remove before generating the bill");
        }
        Bill bill = new Bill(order, at);
        for (OrderLine line : order.getLines()) {
            if (line.lineTotal() > 0) {
                bill.lines.add(new BillLine(bill, line.getMenuItem().getName(), line.getQuantity(), line.getUnitPrice()));
            }
        }
        if (bill.lines.isEmpty()) {
            throw new BusinessRuleException("The order has nothing to charge");
        }
        bill.total = bill.lines.stream().mapToLong(BillLine::lineTotal).sum();
        order.lock();
        return bill;
    }

    /**
     * BR-10: each of the N shares is floor(total / N / 1,000) × 1,000 VND and the remainder goes on
     * the first share, so the shares always add up to the total exactly.
     * Example: 1,000,000 in 3 shares gives 334,000 + 333,000 + 333,000.
     */
    public static long[] evenShares(long total, int count) {
        if (count < MIN_SHARES || count > MAX_SHARES) {
            throw new InvalidInputException("shares", "The number of shares must be between 2 and 20");
        }
        long each = total / count / ROUNDING_UNIT * ROUNDING_UNIT;
        if (each <= 0) {
            throw new BusinessRuleException("A bill of " + Money.format(total) + " is too small to split into "
                    + count + " shares");
        }
        long[] amounts = new long[count];
        Arrays.fill(amounts, each);
        amounts[0] = total - each * (count - 1);
        return amounts;
    }

    /** FR-23: replaces any earlier split with N even shares (BR-10). Only before the first payment. */
    public void splitEvenly(int count) {
        requireOpen();
        requireNoPayments("split");
        long[] amounts = evenShares(total, count);
        clearSplit();
        for (int i = 0; i < amounts.length; i++) {
            shares.add(new BillShare(this, i + 1, amounts[i]));
        }
        splitType = SplitType.EVEN;
    }

    /**
     * FR-24: every bill line goes to exactly one share; share i is groups.get(i). A share's amount is
     * the sum of its lines, so the shares add up to the total. Only before the first payment.
     */
    public void splitByItem(List<List<BillLine>> groups) {
        requireOpen();
        requireNoPayments("split");
        if (groups.size() < MIN_SHARES || groups.size() > MAX_SHARES) {
            throw new InvalidInputException("shares", "The number of shares must be between 2 and 20");
        }
        Set<BillLine> seen = new HashSet<>();
        for (List<BillLine> group : groups) {
            if (group.isEmpty()) {
                throw new InvalidInputException("shares", "Every share needs at least one dish");
            }
            for (BillLine line : group) {
                if (!lines.contains(line)) {
                    throw new InvalidInputException("shares", "A dish does not belong to this bill");
                }
                if (!seen.add(line)) {
                    throw new InvalidInputException("shares", "\"" + line.getItemName() + "\" is in more than one share");
                }
            }
        }
        if (seen.size() != lines.size()) {
            String missing = lines.stream().filter(line -> !seen.contains(line))
                    .map(BillLine::getItemName).collect(Collectors.joining(", "));
            throw new InvalidInputException("shares", "Every dish must be in a share; not assigned: " + missing);
        }
        clearSplit();
        for (int i = 0; i < groups.size(); i++) {
            List<BillLine> group = groups.get(i);
            BillShare share = new BillShare(this, i + 1, group.stream().mapToLong(BillLine::lineTotal).sum());
            shares.add(share);
            group.forEach(line -> line.assignTo(share));
        }
        splitType = SplitType.BY_ITEM;
    }

    /** Back to one payer for the whole bill. Only before the first payment. */
    public void removeSplit() {
        requireOpen();
        requireNoPayments("changed");
        clearSplit();
    }

    private void clearSplit() {
        lines.forEach(line -> line.assignTo(null));
        shares.clear();
        splitType = SplitType.NONE;
    }

    /**
     * Records a payment (FR-25) and closes the bill when it is paid in full (FR-26).
     * BR-11: the amount must be positive; card, bank transfer and e-wallet cannot be more than what is
     * left to pay; cash can, and the change is recorded. BR-12: a closed bill takes no more payments.
     * BR-13: a shift must be open, and the payment belongs to it.
     * When the bill is split, the payment is for one share (share must be given); otherwise share is null.
     */
    public Payment pay(BillShare share, PaymentMethod method, long amount, Shift openShift, User recordedBy,
                       LocalDateTime at) {
        requireOpen();
        if (openShift == null || !openShift.isOpen()) {
            throw new BusinessRuleException("No shift is open; ask the Manager to open a shift before taking payments (BR-13)");
        }
        if (amount <= 0) {
            throw new InvalidInputException("amount", "The amount must be greater than 0");
        }
        long due;
        String dueLabel;
        if (isSplit()) {
            if (share == null) {
                throw new InvalidInputException("shareId", "Choose which share is being paid");
            }
            if (!shares.contains(share)) {
                throw new InvalidInputException("shareId", "The share does not belong to this bill");
            }
            due = share.remaining();
            dueLabel = "share " + share.getNumber();
            if (due == 0) {
                throw new BusinessRuleException("Share " + share.getNumber() + " is already paid");
            }
        } else {
            if (share != null) {
                throw new InvalidInputException("shareId", "This bill is not split");
            }
            due = remaining();
            dueLabel = "the bill";
        }
        long change = 0;
        if (method == PaymentMethod.CASH) {
            change = Math.max(0, amount - due);
        } else if (amount > due) {
            throw new InvalidInputException("amount", "A " + methodLabel(method) + " payment cannot be more than the "
                    + Money.format(due) + " left on " + dueLabel + " (BR-11)");
        }
        Payment payment = new Payment(this, share, openShift, method, amount, change, at, recordedBy);
        payments.add(payment);
        if (remaining() == 0) {
            close(openShift, at);
        }
        return payment;
    }

    /**
     * FR-26: the bill is paid. It becomes read-only (BR-12), the order is closed and the table goes to
     * Needs cleaning through the table lifecycle (BR-05). Runs in the transaction of the last payment (NFR-08).
     */
    private void close(Shift closingShift, LocalDateTime at) {
        status = BillStatus.CLOSED;
        closedAt = at;
        shift = closingShift;
        order.close();
        order.getTable().changeStatus(TableStatus.NEEDS_CLEANING);
    }

    /**
     * UC-12 alternative 4a: the guests want to order more. Allowed only while nothing is paid; the bill
     * is kept as Cancelled and the order can be changed again (BR-09).
     */
    public void cancel() {
        requireOpen();
        requireNoPayments("cancelled");
        status = BillStatus.CANCELLED;
        order.unlock();
    }

    /** BR-12: a closed (or cancelled) bill cannot change. */
    private void requireOpen() {
        if (status == BillStatus.CLOSED) {
            throw new BusinessRuleException("This bill is closed and can no longer be changed (BR-12)");
        }
        if (status == BillStatus.CANCELLED) {
            throw new BusinessRuleException("This bill was cancelled");
        }
    }

    private void requireNoPayments(String action) {
        if (!payments.isEmpty()) {
            throw new BusinessRuleException("A payment is already recorded, so the bill cannot be " + action);
        }
    }

    private static String methodLabel(PaymentMethod method) {
        return switch (method) {
            case CASH -> "cash";
            case CARD -> "card";
            case BANK_TRANSFER -> "bank transfer";
            case E_WALLET -> "e-wallet";
        };
    }

    public long paidAmount() {
        return payments.stream().mapToLong(Payment::appliedAmount).sum();
    }

    public long remaining() {
        return total - paidAmount();
    }

    public boolean isSplit() {
        return splitType != SplitType.NONE;
    }

    public Optional<BillShare> findShare(Long shareId) {
        return shares.stream().filter(share -> shareId.equals(share.getId())).findFirst();
    }

    public Optional<BillLine> findLine(Long lineId) {
        return lines.stream().filter(line -> lineId.equals(line.getId())).findFirst();
    }

    public Long getId() {
        return id;
    }

    public CustomerOrder getOrder() {
        return order;
    }

    public long getTotal() {
        return total;
    }

    public BillStatus getStatus() {
        return status;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public Shift getShift() {
        return shift;
    }

    public List<BillLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public List<BillShare> getShares() {
        return Collections.unmodifiableList(shares);
    }

    public List<Payment> getPayments() {
        return Collections.unmodifiableList(payments);
    }
}
