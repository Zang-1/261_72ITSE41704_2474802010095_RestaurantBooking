package vn.edu.vlu.restaurant.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /** SELECT … FOR UPDATE: two users booking or seating the same table are served one after the other (NFR-06). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from DiningTable t where t.id = :id")
    Optional<DiningTable> findByIdForUpdate(@Param("id") Long id);
}
