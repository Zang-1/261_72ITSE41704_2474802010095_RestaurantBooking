package vn.edu.vlu.restaurant.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.vlu.restaurant.domain.Reservation;
import vn.edu.vlu.restaurant.domain.ReservationStatus;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByCode(String code);

    /** Reservations of one day: from is inclusive, to is exclusive. */
    @EntityGraph(attributePaths = "table")
    List<Reservation> findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
            LocalDateTime from, LocalDateTime to);

    long countByTableIdAndStatusAndStartTimeAfter(Long tableId, ReservationStatus status, LocalDateTime after);

    List<Reservation> findByTableIdAndStatusOrderByStartTimeAsc(Long tableId, ReservationStatus status);

    /** Bookings that start strictly inside (from, to): the BR-01 overlap window around a new start time. */
    @EntityGraph(attributePaths = "table")
    List<Reservation> findByStatusAndStartTimeGreaterThanAndStartTimeLessThan(ReservationStatus status, LocalDateTime from, LocalDateTime to);

    @EntityGraph(attributePaths = "table")
    List<Reservation> findByStatusAndStartTimeBetweenOrderByStartTimeAsc(ReservationStatus status, LocalDateTime from, LocalDateTime to);
}
