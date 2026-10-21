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
import java.time.LocalDateTime;

/** A walk-in party waiting for a table. seatedTable stays null while the party is waiting. */
@Entity
@Table(name = "waitlist_entry")
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "party_name", nullable = false, length = 100)
    private String partyName;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(name = "party_size", nullable = false)
    private int partySize;

    @Column(name = "arrived_at", nullable = false)
    private LocalDateTime arrivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seated_table_id")
    private DiningTable seatedTable;

    protected WaitlistEntry() {
    }

    public WaitlistEntry(String partyName, String phone, int partySize, LocalDateTime arrivedAt) {
        this.partyName = partyName;
        this.phone = phone;
        this.partySize = partySize;
        this.arrivedAt = arrivedAt;
    }

    public boolean isWaiting() {
        return seatedTable == null;
    }

    public Long getId() {
        return id;
    }

    public String getPartyName() {
        return partyName;
    }

    public String getPhone() {
        return phone;
    }

    public int getPartySize() {
        return partySize;
    }

    public LocalDateTime getArrivedAt() {
        return arrivedAt;
    }

    public DiningTable getSeatedTable() {
        return seatedTable;
    }
}
