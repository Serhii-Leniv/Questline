package com.questline.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.questline.common.ApiException;
import com.questline.domain.PlanType;
import com.questline.domain.User;
import com.questline.repository.AiJobRepository;
import com.questline.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiRateLimiterTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    AiJobRepository aiJobRepository;

    @Mock
    UserRepository userRepository;

    private AiRateLimiter limiter;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-11T12:00:00Z"), ZoneId.of("UTC"));
        limiter = new AiRateLimiter(aiJobRepository, userRepository, clock, 2, 5); // free=2, pro=5
    }

    @Test
    void allowsUnderTheFreeLimit() {
        stubPlan(PlanType.FREE);
        when(aiJobRepository.countByUser_IdAndCreatedAtAfter(eq(USER_ID), any())).thenReturn(1L);
        assertThatCode(() -> limiter.check(USER_ID)).doesNotThrowAnyException();
    }

    @Test
    void rejectsAtTheFreeLimit() {
        stubPlan(PlanType.FREE);
        when(aiJobRepository.countByUser_IdAndCreatedAtAfter(eq(USER_ID), any())).thenReturn(2L);
        assertThatThrownBy(() -> limiter.check(USER_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("AI_RATE_LIMIT"));
    }

    @Test
    void proPlanAllowsBeyondTheFreeLimit() {
        stubPlan(PlanType.PRO);
        when(aiJobRepository.countByUser_IdAndCreatedAtAfter(eq(USER_ID), any())).thenReturn(3L);
        assertThatCode(() -> limiter.check(USER_ID)).doesNotThrowAnyException();
    }

    @Test
    void limitForReflectsThePlan() {
        assertThat(limiter.limitFor(PlanType.FREE)).isEqualTo(2);
        assertThat(limiter.limitFor(PlanType.PRO)).isEqualTo(5);
    }

    private void stubPlan(PlanType plan) {
        User user = new User();
        user.setId(USER_ID);
        user.setPlan(plan);
        lenient().when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    }
}
