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

/**
 * One dish on a bill, copied from an order line when the bill is generated (FR-21). The name and the
 * price are copied, so a later menu change does not alter the bill. Created by {@link Bill#generate}.
 */
@Entity
@Table(name = "bill_line")
public class BillLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    /** The share that pays for this line when the bill is split by item (FR-24); null otherwise. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "share_id")
    private BillShare share;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private long unitPrice;

    protected BillLine() {
    }

    BillLine(Bill bill, String itemName, int quantity, long unitPrice) {
        this.bill = bill;
        this.itemName = itemName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public long lineTotal() {
        return unitPrice * quantity;
    }

    void assignTo(BillShare share) {
        this.share = share;
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

    public String getItemName() {
        return itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getUnitPrice() {
        return unitPrice;
    }
}
