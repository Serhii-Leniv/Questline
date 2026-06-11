package com.questline.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.questline.domain.Streak;
import com.questline.domain.User;
import com.questline.repository.StreakRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StreakMaintenanceServiceTest {

    @Mock
    StreakRepository streakRepository;

    private StreakMaintenanceService service;
    private final User user = new User();

    @BeforeEach
    void setUp() {
        // 2026-06-11 09:00 UTC → same date in Kyiv.
        Clock clock = Clock.fixed(Instant.parse("2026-06-11T09:00:00Z"), ZoneId.of("UTC"));
        service = new StreakMaintenanceService(streakRepository, clock);
        user.setTimezone("Europe/Kyiv");
    }

    @Test
    void breaksStreakWhenGapExceedsFreezes() {
        Streak streak = streak(5, LocalDate.of(2026, 6, 8), 0); // missed 09 and 10 → 2 missed days
        when(streakRepository.findByCurrentGreaterThan(0)).thenReturn(List.of(streak));

        service.resetStaleStreaks();

        assertThat(streak.getCurrent()).isZero();
    }

    @Test
    void keepsStreakWhenFreezesCoverTheGap() {
        Streak streak = streak(5, LocalDate.of(2026, 6, 9), 2); // 1 missed day, 2 freezes
        when(streakRepository.findByCurrentGreaterThan(0)).thenReturn(List.of(streak));

        service.resetStaleStreaks();

        assertThat(streak.getCurrent()).isEqualTo(5);
    }

    @Test
    void keepsStreakWhenActiveYesterday() {
        Streak streak = streak(5, LocalDate.of(2026, 6, 10), 0); // can still act today
        when(streakRepository.findByCurrentGreaterThan(0)).thenReturn(List.of(streak));

        service.resetStaleStreaks();

        assertThat(streak.getCurrent()).isEqualTo(5);
    }

    @Test
    void keepsStreakWhenActiveToday() {
        Streak streak = streak(5, LocalDate.of(2026, 6, 11), 0);
        when(streakRepository.findByCurrentGreaterThan(0)).thenReturn(List.of(streak));

        service.resetStaleStreaks();

        assertThat(streak.getCurrent()).isEqualTo(5);
    }

    private Streak streak(int current, LocalDate lastActiveDate, int freezes) {
        Streak streak = new Streak();
        streak.setUser(user);
        streak.setCurrent(current);
        streak.setLastActiveDate(lastActiveDate);
        streak.setFreezesAvailable(freezes);
        return streak;
    }
}
