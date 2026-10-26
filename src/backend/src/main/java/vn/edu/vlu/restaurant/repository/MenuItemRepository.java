package vn.edu.vlu.restaurant.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.vlu.restaurant.domain.MenuItem;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    /**
     * Menu listing. The Manager sees inactive items too; the order screens ask for availableOnly,
     * which hides items the kitchen has marked out of stock (BR-07).
     */
    @Query("select i from MenuItem i join fetch i.category c "
            + "where (:includeInactive = true or i.active = true) "
            + "and (:availableOnly = false or i.inStock = true) "
            + "order by c.name, i.name")
    List<MenuItem> search(@Param("includeInactive") boolean includeInactive,
                          @Param("availableOnly") boolean availableOnly);

    boolean existsByCategoryIdAndNameIgnoreCase(Long categoryId, String name);

    boolean existsByCategoryIdAndNameIgnoreCaseAndIdNot(Long categoryId, String name, Long id);
}
