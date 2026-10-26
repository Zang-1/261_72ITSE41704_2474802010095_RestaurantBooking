package vn.edu.vlu.restaurant.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.vlu.restaurant.domain.MenuCategory;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

    boolean existsByNameIgnoreCase(String name);

    List<MenuCategory> findAllByOrderByNameAsc();
}
