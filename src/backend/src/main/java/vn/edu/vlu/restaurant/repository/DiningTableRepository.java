package vn.edu.vlu.restaurant.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.edu.vlu.restaurant.domain.DiningTable;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {

    boolean existsByNumber(int number);

    boolean existsByNumberAndIdNot(int number, Long id);

    @EntityGraph(attributePaths = "section")
    List<DiningTable> findAllByOrderByNumberAsc();

    @EntityGraph(attributePaths = "section")
    List<DiningTable> findByActiveTrueOrderByNumberAsc();

    /** Active tables with enough seats, smallest suitable table first (BR-02). */
    @EntityGraph(attributePaths = "section")
    List<DiningTable> findByActiveTrueAndCapacityGreaterThanEqualOrderByCapacityAscNumberAsc(int partySize);

    @Query("select max(t.capacity) from DiningTable t where t.active = true")
    Optional<Integer> findLargestActiveCapacity();
}
