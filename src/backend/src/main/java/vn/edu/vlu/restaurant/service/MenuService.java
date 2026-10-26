package vn.edu.vlu.restaurant.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.vlu.restaurant.domain.MenuCategory;
import vn.edu.vlu.restaurant.domain.MenuItem;
import vn.edu.vlu.restaurant.dto.MenuCategoryRequest;
import vn.edu.vlu.restaurant.dto.MenuCategoryResponse;
import vn.edu.vlu.restaurant.dto.MenuItemRequest;
import vn.edu.vlu.restaurant.dto.MenuItemResponse;
import vn.edu.vlu.restaurant.exception.InvalidInputException;
import vn.edu.vlu.restaurant.exception.NotFoundException;
import vn.edu.vlu.restaurant.repository.MenuCategoryRepository;
import vn.edu.vlu.restaurant.repository.MenuItemRepository;

/** Menu categories and items (FR-08, UC-08) and the sold-out switch used by the kitchen (UC-10). */
@Service
@Transactional
public class MenuService {

    private final MenuItemRepository itemRepository;
    private final MenuCategoryRepository categoryRepository;

    public MenuService(MenuItemRepository itemRepository, MenuCategoryRepository categoryRepository) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<MenuCategoryResponse> listCategories() {
        return categoryRepository.findAllByOrderByNameAsc().stream().map(MenuCategoryResponse::from).toList();
    }

    public MenuCategoryResponse createCategory(MenuCategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new InvalidInputException("name", "Category \"" + name + "\" already exists");
        }
        return MenuCategoryResponse.from(categoryRepository.save(new MenuCategory(name)));
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> listItems(boolean includeInactive, boolean availableOnly) {
        return itemRepository.search(includeInactive, availableOnly).stream().map(MenuItemResponse::from).toList();
    }

    public MenuItemResponse createItem(MenuItemRequest request) {
        MenuCategory category = findCategory(request.categoryId());
        String name = request.name().trim();
        if (itemRepository.existsByCategoryIdAndNameIgnoreCase(category.getId(), name)) {
            throw duplicateName(name, category);
        }
        MenuItem item = new MenuItem(category, name, cleanDescription(request.description()),
                request.price(), request.course());
        return MenuItemResponse.from(itemRepository.save(item));
    }

    public MenuItemResponse updateItem(Long id, MenuItemRequest request) {
        MenuItem item = findItem(id);
        MenuCategory category = findCategory(request.categoryId());
        String name = request.name().trim();
        if (itemRepository.existsByCategoryIdAndNameIgnoreCaseAndIdNot(category.getId(), name, id)) {
            throw duplicateName(name, category);
        }
        item.updateDetails(category, name, cleanDescription(request.description()),
                request.price(), request.course());
        return MenuItemResponse.from(item);
    }

    /** Takes the item off the menu without deleting it, so old orders keep their history. */
    public MenuItemResponse deactivateItem(Long id) {
        MenuItem item = findItem(id);
        item.deactivate();
        return MenuItemResponse.from(item);
    }

    public MenuItemResponse activateItem(Long id) {
        MenuItem item = findItem(id);
        item.activate();
        return MenuItemResponse.from(item);
    }

    /** The kitchen ran out of an ingredient; the item disappears from the order screens (BR-07). */
    public MenuItemResponse markOutOfStock(Long id) {
        MenuItem item = findItem(id);
        item.markOutOfStock();
        return MenuItemResponse.from(item);
    }

    public MenuItemResponse restock(Long id) {
        MenuItem item = findItem(id);
        item.restock();
        return MenuItemResponse.from(item);
    }

    private MenuItem findItem(Long id) {
        return itemRepository.findById(id).orElseThrow(() -> new NotFoundException("Menu item not found"));
    }

    private MenuCategory findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new InvalidInputException("categoryId", "Category not found"));
    }

    private InvalidInputException duplicateName(String name, MenuCategory category) {
        return new InvalidInputException("name",
                "\"" + name + "\" already exists in " + category.getName());
    }

    /** A blank description is stored as no description. */
    private String cleanDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }
}
