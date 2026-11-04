package vn.edu.vlu.restaurant.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.vlu.restaurant.domain.Course;
import vn.edu.vlu.restaurant.domain.MenuCategory;
import vn.edu.vlu.restaurant.domain.MenuItem;
import vn.edu.vlu.restaurant.repository.MenuCategoryRepository;
import vn.edu.vlu.restaurant.repository.MenuItemRepository;

/**
 * Creates a small sample menu on an empty database so the order screens have something to show.
 * The dishes and prices are examples (VND), not the real menu of the interviewed restaurant.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class MenuSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MenuSeeder.class);

    private final MenuCategoryRepository categoryRepository;
    private final MenuItemRepository itemRepository;

    public MenuSeeder(MenuCategoryRepository categoryRepository, MenuItemRepository itemRepository) {
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() > 0) {
            return;
        }
        MenuCategory appetizers = categoryRepository.save(new MenuCategory("Appetizers"));
        MenuCategory soups = categoryRepository.save(new MenuCategory("Soups"));
        MenuCategory mains = categoryRepository.save(new MenuCategory("Main dishes"));
        MenuCategory desserts = categoryRepository.save(new MenuCategory("Desserts"));
        MenuCategory drinks = categoryRepository.save(new MenuCategory("Drinks"));

        add(appetizers, "Fresh spring rolls", "Shrimp, herbs and rice noodles", 55_000, Course.STARTER);
        add(appetizers, "Crispy fried spring rolls", "Pork and mushroom filling", 60_000, Course.STARTER);
        add(appetizers, "Green papaya salad", null, 65_000, Course.STARTER);
        add(appetizers, "Banana flower salad with chicken", null, 75_000, Course.STARTER);
        add(appetizers, "Grilled beef in betel leaves", "Six rolls with peanut sauce", 85_000, Course.STARTER);
        add(appetizers, "Steamed rice rolls", "Minced pork and wood ear mushroom", 50_000, Course.STARTER);
        add(soups, "Sour fish soup", "Pineapple, tomato and tamarind", 95_000, Course.STARTER);
        add(soups, "Crab and asparagus soup", null, 70_000, Course.STARTER);
        add(soups, "Pumpkin soup with shrimp", null, 60_000, Course.STARTER);
        add(mains, "Grilled pork with rice", "Lemongrass pork, fried egg, pickles", 85_000, Course.MAIN);
        add(mains, "Beef pho", null, 75_000, Course.MAIN);
        add(mains, "Caramelized fish in clay pot", "Served with steamed rice", 140_000, Course.MAIN);
        add(mains, "Stir-fried morning glory with garlic", null, 60_000, Course.MAIN);
        add(mains, "Shaking beef", "Cubed beef tenderloin with watercress", 165_000, Course.MAIN);
        add(mains, "Lemongrass chicken", null, 120_000, Course.MAIN);
        add(mains, "Grilled squid with chili salt", null, 135_000, Course.MAIN);
        add(mains, "Grilled pork with rice noodles", "Hanoi style, with herbs and dipping sauce", 90_000, Course.MAIN);
        add(mains, "Tofu in tomato sauce", "Vegetarian", 70_000, Course.MAIN);
        add(mains, "Seafood fried rice", null, 95_000, Course.MAIN);
        add(desserts, "Sweet corn pudding", null, 35_000, Course.DESSERT);
        add(desserts, "Coconut ice cream", "Two scoops with roasted peanuts", 45_000, Course.DESSERT);
        add(desserts, "Mung bean sweet soup", null, 30_000, Course.DESSERT);
        add(desserts, "Caramel flan", null, 35_000, Course.DESSERT);
        add(desserts, "Fresh fruit platter", "Seasonal fruit for two", 70_000, Course.DESSERT);
        add(drinks, "Vietnamese iced coffee", null, 35_000, Course.STARTER);
        add(drinks, "Fresh orange juice", null, 40_000, Course.STARTER);
        add(drinks, "Sparkling water", null, 25_000, Course.STARTER);
        add(drinks, "Lotus tea", null, 30_000, Course.STARTER);
        add(drinks, "Fresh coconut", null, 45_000, Course.STARTER);
        add(drinks, "Salted lemonade", null, 30_000, Course.STARTER);
        log.info("Seeded 5 menu categories and {} menu items", itemRepository.count());
    }

    private void add(MenuCategory category, String name, String description, long price, Course course) {
        itemRepository.save(new MenuItem(category, name, description, price, course));
    }
}
