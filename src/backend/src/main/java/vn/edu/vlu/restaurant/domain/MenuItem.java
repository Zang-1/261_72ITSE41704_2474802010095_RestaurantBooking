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

/**
 * A dish or drink on the menu (FR-08). The price is a whole number of VND.
 * inStock is switched by the kitchen when an ingredient runs out; active is switched by the Manager
 * when the dish leaves the menu. Only an active, in-stock item can be ordered (BR-07).
 */
@Entity
@Table(name = "menu_item")
public class MenuItem {

    public static final long MIN_PRICE = 1;
    public static final long MAX_PRICE = 10_000_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private MenuCategory category;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 300)
    private String description;

    @Column(nullable = false)
    private long price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Course course;

    @Column(name = "in_stock", nullable = false)
    private boolean inStock = true;

    @Column(nullable = false)
    private boolean active = true;

    protected MenuItem() {
    }

    public MenuItem(MenuCategory category, String name, String description, long price, Course course) {
        updateDetails(category, name, description, price, course);
    }

    public void updateDetails(MenuCategory category, String name, String description, long price, Course course) {
        if (price < MIN_PRICE || price > MAX_PRICE) {
            throw new IllegalArgumentException("Price must be between 1 and 10,000,000 VND");
        }
        this.category = category;
        this.name = name;
        this.description = description;
        this.price = price;
        this.course = course;
    }

    /** The kitchen has run out of an ingredient (UC-10); the item disappears from the order screens. */
    public void markOutOfStock() {
        this.inStock = false;
    }

    public void restock() {
        this.inStock = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    /** A waiter may add this item to an order only if it is on the menu and in stock (BR-07). */
    public boolean isOrderable() {
        return active && inStock;
    }

    public Long getId() {
        return id;
    }

    public MenuCategory getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public long getPrice() {
        return price;
    }

    public Course getCourse() {
        return course;
    }

    public boolean isInStock() {
        return inStock;
    }

    public boolean isActive() {
        return active;
    }
}
