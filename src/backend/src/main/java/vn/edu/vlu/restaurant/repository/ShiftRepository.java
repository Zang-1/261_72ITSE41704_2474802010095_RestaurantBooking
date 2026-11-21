package vn.edu.vlu.restaurant.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import vn.edu.vlu.restaurant.domain.Shift;

public interface ShiftRepository extends JpaRepository<Shift, Long> {

    Optional<Shift> findFirstByClosedAtIsNull();

    /**
     * The open shift, locked until the transaction ends. Payments, bill generation and closing the
     * shift all take this lock first, so a shift cannot close while a payment is being recorded (BR-13).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shift s where s.closedAt is null")
    Optional<Shift> findOpenForUpdate();

    List<Shift> findAllByOrderByOpenedAtDesc();
}
