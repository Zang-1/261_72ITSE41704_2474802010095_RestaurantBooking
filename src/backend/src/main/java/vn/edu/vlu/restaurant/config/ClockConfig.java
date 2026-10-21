package vn.edu.vlu.restaurant.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Services read "now" from this Clock instead of calling LocalDateTime.now() directly, so tests can
 * fix the time (needed for the no-show and Reserved rules in Week 7).
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.time-zone}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
