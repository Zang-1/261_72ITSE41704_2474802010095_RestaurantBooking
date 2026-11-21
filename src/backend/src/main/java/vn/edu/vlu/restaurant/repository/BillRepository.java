package vn.edu.vlu.restaurant.repository;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.vlu.restaurant.domain.Bill;
import vn.edu.vlu.restaurant.domain.BillStatus;

public interface BillRepository extends JpaRepository<Bill, Long> {

    /** SELECT … FOR UPDATE: two payments for the same bill are recorded one after the other (NFR-08). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bill b where b.id = :id")
    Optional<Bill> findByIdForUpdate(@Param("id") Long id);

    List<Bill> findByOrderTableIdAndStatusOrderByCreatedAtDesc(Long tableId, BillStatus status);

    boolean existsByOrderIdAndStatusIn(Long orderId, Collection<BillStatus> statuses);

    /** Unpaid bills with their tables, for the BR-13 check when the shift is closed. */
    @Query("select b from Bill b join fetch b.order o join fetch o.table where b.status = :status order by o.table.number")
    List<Bill> findWithTableByStatus(@Param("status") BillStatus status);

    /** Bills closed in a shift with their tables in one query, for the shift report (FR-28, NFR-04). */
    @Query("select b from Bill b join fetch b.order o join fetch o.table "
            + "where b.shift.id = :shiftId and b.status = vn.edu.vlu.restaurant.domain.BillStatus.CLOSED "
            + "order by b.closedAt asc, b.id asc")
    List<Bill> findClosedInShift(@Param("shiftId") Long shiftId);
}
