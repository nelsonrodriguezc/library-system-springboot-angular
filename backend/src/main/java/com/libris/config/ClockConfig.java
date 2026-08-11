package com.libris.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Time is injected rather than read from static {@code now()} calls, so every rule that
 * depends on dates (due dates, reminders, the 90-day overdue window, temporary blocks)
 * can be exercised deterministically in tests with a fixed clock.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
