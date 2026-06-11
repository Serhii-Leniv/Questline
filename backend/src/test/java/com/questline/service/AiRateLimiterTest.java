package com.questline.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.questline.common.ApiException;
import com.questline.repository.AiJobRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
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

    private AiRateLimiter limiter;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-11T12:00:00Z"), ZoneId.of("UTC"));
        limiter = new AiRateLimiter(aiJobRepository, clock, 2);
    }

    @Test
    void allowsUnderTheLimit() {
        when(aiJobRepository.countByUser_IdAndCreatedAtAfter(eq(USER_ID), any())).thenReturn(1L);
        assertThatCode(() -> limiter.check(USER_ID)).doesNotThrowAnyException();
    }

    @Test
    void rejectsAtTheLimit() {
        when(aiJobRepository.countByUser_IdAndCreatedAtAfter(eq(USER_ID), any())).thenReturn(2L);
        assertThatThrownBy(() -> limiter.check(USER_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> org.assertj.core.api.Assertions
                        .assertThat(((ApiException) e).code()).isEqualTo("AI_RATE_LIMIT"));
    }
}
