package vn.edu.vlu.restaurant.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.vlu.restaurant.domain.TableSection;

public interface TableSectionRepository extends JpaRepository<TableSection, Long> {

    boolean existsByNameIgnoreCase(String name);

    List<TableSection> findAllByOrderByNameAsc();
}
