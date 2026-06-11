package com.questline.common;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides a single {@link Clock} so time-dependent logic (e.g. "today" in the user's timezone,
 * task completion timestamps) is injected rather than read from {@code Instant.now()} directly.
 * Tests can replace this bean with a fixed clock to pin "now".
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
