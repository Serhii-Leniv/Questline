package com.questline.service;

import com.questline.common.ApiException;
import com.questline.repository.AiJobRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Per-user cap on AI requests over a rolling 24-hour window, to stay within the provider's free
 * tier. Configurable via {@code app.ai.daily-job-limit}.
 */
@Component
public class AiRateLimiter {

    private final AiJobRepository aiJobRepository;
    private final Clock clock;
    private final int dailyLimit;

    public AiRateLimiter(AiJobRepository aiJobRepository, Clock clock,
                         @Value("${app.ai.daily-job-limit:50}") int dailyLimit) {
        this.aiJobRepository = aiJobRepository;
        this.clock = clock;
        this.dailyLimit = dailyLimit;
    }

    /** Throws 429 if the user has already hit the limit in the last 24 hours. */
    public void check(UUID userId) {
        Instant since = Instant.now(clock).minus(Duration.ofDays(1));
        long used = aiJobRepository.countByUser_IdAndCreatedAtAfter(userId, since);
        if (used >= dailyLimit) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "AI_RATE_LIMIT",
                    "You've reached today's AI request limit. Please try again later.");
        }
    }
}
