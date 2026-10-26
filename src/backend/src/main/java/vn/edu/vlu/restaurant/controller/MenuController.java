package vn.edu.vlu.restaurant.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.vlu.restaurant.dto.MenuCategoryRequest;
import vn.edu.vlu.restaurant.dto.MenuCategoryResponse;
import vn.edu.vlu.restaurant.dto.MenuItemRequest;
import vn.edu.vlu.restaurant.dto.MenuItemResponse;
import vn.edu.vlu.restaurant.service.MenuService;

/**
 * Menu management (UC-08). Every staff role can read the menu; only the Manager changes it, and the
 * kitchen can mark an item sold out (UC-10).
 */
@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/categories")
    @PreAuthorize(AccessRules.ANY_STAFF)
    public List<MenuCategoryResponse> listCategories() {
        return menuService.listCategories();
    }

    @PostMapping("/categories")
    @PreAuthorize(AccessRules.MANAGER)
    @ResponseStatus(HttpStatus.CREATED)
    public MenuCategoryResponse createCategory(@Valid @RequestBody MenuCategoryRequest request) {
        return menuService.createCategory(request);
    }

    /**
     * includeInactive=true is for the Manager's menu screen. availableOnly=true is for the order
     * screens and hides items that are out of stock (BR-07).
     */
    @GetMapping("/items")
    @PreAuthorize(AccessRules.ANY_STAFF)
    public List<MenuItemResponse> listItems(@RequestParam(defaultValue = "false") boolean includeInactive,
                                            @RequestParam(defaultValue = "false") boolean availableOnly,
                                            Authentication authentication) {
        if (includeInactive && !isManager(authentication)) {
            throw new AccessDeniedException("Only a Manager can see items that are off the menu");
        }
        return menuService.listItems(includeInactive, availableOnly);
    }

    @PostMapping("/items")
    @PreAuthorize(AccessRules.MANAGER)
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItemResponse createItem(@Valid @RequestBody MenuItemRequest request) {
        return menuService.createItem(request);
    }

    @PutMapping("/items/{id}")
    @PreAuthorize(AccessRules.MANAGER)
    public MenuItemResponse updateItem(@PathVariable Long id, @Valid @RequestBody MenuItemRequest request) {
        return menuService.updateItem(id, request);
    }

    @PostMapping("/items/{id}/deactivate")
    @PreAuthorize(AccessRules.MANAGER)
    public MenuItemResponse deactivateItem(@PathVariable Long id) {
        return menuService.deactivateItem(id);
    }

    @PostMapping("/items/{id}/activate")
    @PreAuthorize(AccessRules.MANAGER)
    public MenuItemResponse activateItem(@PathVariable Long id) {
        return menuService.activateItem(id);
    }

    @PostMapping("/items/{id}/out-of-stock")
    @PreAuthorize(AccessRules.KITCHEN)
    public MenuItemResponse markOutOfStock(@PathVariable Long id) {
        return menuService.markOutOfStock(id);
    }

    @PostMapping("/items/{id}/restock")
    @PreAuthorize(AccessRules.KITCHEN)
    public MenuItemResponse restock(@PathVariable Long id) {
        return menuService.restock(id);
    }

    private boolean isManager(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_MANAGER".equals(authority.getAuthority()));
    }
}
