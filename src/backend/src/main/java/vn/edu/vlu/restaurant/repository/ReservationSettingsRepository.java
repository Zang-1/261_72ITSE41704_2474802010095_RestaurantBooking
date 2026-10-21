package vn.edu.vlu.restaurant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.vlu.restaurant.domain.ReservationSettings;

public interface ReservationSettingsRepository extends JpaRepository<ReservationSettings, Long> {
}
