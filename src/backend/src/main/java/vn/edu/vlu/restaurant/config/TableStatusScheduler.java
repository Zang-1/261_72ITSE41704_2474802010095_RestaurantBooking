package vn.edu.vlu.restaurant.config;

import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import vn.edu.vlu.restaurant.service.ReservationService;

/**
 * Applies the time-driven rules every minute: Reserved 30 minutes before a booking (BR-05) and
 * No-show 45 minutes after it (FR-06, CR-01). Switched off in tests with app.scheduling.enabled=false.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class TableStatusScheduler {

    private final ReservationService reservationService;
    private final Clock clock;

    public TableStatusScheduler(ReservationService reservationService, Clock clock) {
        this.reservationService = reservationService;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 60_000)
    public void refresh() {
        reservationService.refreshStatuses(LocalDateTime.now(clock));
    }
}
