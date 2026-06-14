package com.questline.service;

import com.questline.common.ApiException;
import com.questline.domain.PlanType;
import com.questline.domain.User;
import com.questline.repository.AiJobRepository;
import com.questline.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Per-user cap on AI requests over a rolling 24-hour window. The limit depends on the user's plan
 * (FREE vs PRO), configured via {@code app.plan.*-ai-daily-limit}.
 */
@Component
public class AiRateLimiter {

    private final AiJobRepository aiJobRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final int freeLimit;
    private final int proLimit;

    public AiRateLimiter(AiJobRepository aiJobRepository, UserRepository userRepository, Clock clock,
                         @Value("${app.plan.free-ai-daily-limit:10}") int freeLimit,
                         @Value("${app.plan.pro-ai-daily-limit:200}") int proLimit) {
        this.aiJobRepository = aiJobRepository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.freeLimit = freeLimit;
        this.proLimit = proLimit;
    }

    /** Throws 429 if the user has already hit their plan's limit in the last 24 hours. */
    public void check(UUID userId) {
        PlanType plan = userRepository.findById(userId).map(User::getPlan).orElse(PlanType.FREE);
        Instant since = Instant.now(clock).minus(Duration.ofDays(1));
        long used = aiJobRepository.countByUser_IdAndCreatedAtAfter(userId, since);
        if (used >= limitFor(plan)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "AI_RATE_LIMIT",
                    "You've reached today's AI request limit for your plan. Upgrade or try again later.");
        }
    }

    public int limitFor(PlanType plan) {
        return plan == PlanType.PRO ? proLimit : freeLimit;
    }
}
