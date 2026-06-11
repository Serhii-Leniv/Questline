package com.questline.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.questline.domain.ActivityDay;
import com.questline.domain.Streak;
import com.questline.domain.Task;
import com.questline.domain.User;
import com.questline.repository.ActivityDayRepository;
import com.questline.repository.StreakRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pure unit tests for the deterministic gamification rules (SPEC §10): streak advancement,
 * XP with streak bonus, and the user-timezone day boundary. No Spring, no database.
 */
@ExtendWith(MockitoExtension.class)
class GamificationServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    ActivityDayRepository activityDayRepository;

    @Mock
    StreakRepository streakRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);
        user.setTimezone("Europe/Kyiv");
        user.setDailyTaskGoal(1);
        user.setXpTotal(0);
    }

    @Test
    void firstCompletion_createsDay_startsStreak_awardsXp() {
        // 2026-06-10 10:00 UTC = same date in Kyiv.
        GamificationService service = serviceAt("2026-06-10T10:00:00Z");
        when(activityDayRepository.findByUser_IdAndDate(USER_ID, LocalDate.of(2026, 6, 10)))
                .thenReturn(Optional.empty());
        when(streakRepository.findById(USER_ID)).thenReturn(Optional.empty());

        service.onTaskCompleted(taskWithEstimate(25));

        ActivityDay day = savedDay();
        assertThat(day.getDate()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(day.getTasksCompleted()).isEqualTo(1);
        assertThat(day.getMinutesSpent()).isEqualTo(25);
        assertThat(day.isGoalMet()).isTrue();

        Streak streak = savedStreak();
        assertThat(streak.getCurrent()).isEqualTo(1);
        assertThat(streak.getLongest()).isEqualTo(1);
        assertThat(streak.getLastActiveDate()).isEqualTo(LocalDate.of(2026, 6, 10));

        // Streak of 1 → multiplier 1.02 → round(10 * 1.02) = 10.
        assertThat(user.getXpTotal()).isEqualTo(10);
        assertThat(day.getXpEarned()).isEqualTo(10);
    }

    @Test
    void secondTaskSameDay_incrementsTasks_doesNotDoubleAdvanceStreak() {
        GamificationService service = serviceAt("2026-06-10T10:00:00Z");
        ActivityDay existing = day(LocalDate.of(2026, 6, 10), 1, true);
        Streak existingStreak = streak(1, 1, LocalDate.of(2026, 6, 10));
        when(activityDayRepository.findByUser_IdAndDate(USER_ID, LocalDate.of(2026, 6, 10)))
                .thenReturn(Optional.of(existing));
        when(streakRepository.findById(USER_ID)).thenReturn(Optional.of(existingStreak));

        service.onTaskCompleted(taskWithEstimate(null));

        assertThat(existing.getTasksCompleted()).isEqualTo(2);
        assertThat(existingStreak.getCurrent()).isEqualTo(1); // unchanged
    }

    @Test
    void consecutiveDay_extendsStreak() {
        GamificationService service = serviceAt("2026-06-11T08:00:00Z");
        Streak existingStreak = streak(3, 5, LocalDate.of(2026, 6, 10));
        when(activityDayRepository.findByUser_IdAndDate(USER_ID, LocalDate.of(2026, 6, 11)))
                .thenReturn(Optional.empty());
        when(streakRepository.findById(USER_ID)).thenReturn(Optional.of(existingStreak));

        service.onTaskCompleted(taskWithEstimate(null));

        assertThat(existingStreak.getCurrent()).isEqualTo(4);
        assertThat(existingStreak.getLongest()).isEqualTo(5); // 4 < previous longest
        assertThat(existingStreak.getLastActiveDate()).isEqualTo(LocalDate.of(2026, 6, 11));
    }

    @Test
    void gapDay_resetsStreakToOne() {
        GamificationService service = serviceAt("2026-06-13T08:00:00Z");
        Streak existingStreak = streak(9, 9, LocalDate.of(2026, 6, 10)); // 3-day gap
        when(activityDayRepository.findByUser_IdAndDate(USER_ID, LocalDate.of(2026, 6, 13)))
                .thenReturn(Optional.empty());
        when(streakRepository.findById(USER_ID)).thenReturn(Optional.of(existingStreak));

        service.onTaskCompleted(taskWithEstimate(null));

        assertThat(existingStreak.getCurrent()).isEqualTo(1);
        assertThat(existingStreak.getLongest()).isEqualTo(9);
    }

    @Test
    void xpUsesStreakMultiplier() {
        GamificationService service = serviceAt("2026-06-11T08:00:00Z");
        Streak existingStreak = streak(4, 4, LocalDate.of(2026, 6, 10)); // becomes 5 today
        when(activityDayRepository.findByUser_IdAndDate(USER_ID, LocalDate.of(2026, 6, 11)))
                .thenReturn(Optional.empty());
        when(streakRepository.findById(USER_ID)).thenReturn(Optional.of(existingStreak));

        service.onTaskCompleted(taskWithEstimate(null));

        // Streak 5 → multiplier 1.10 → round(10 * 1.10) = 11.
        assertThat(existingStreak.getCurrent()).isEqualTo(5);
        assertThat(user.getXpTotal()).isEqualTo(11);
    }

    @Test
    void completionAfterLocalMidnight_countsToNewLocalDay() {
        // 2026-06-10T22:30Z is 2026-06-11 01:30 in Kyiv (UTC+3) → new local day.
        GamificationService service = serviceAt("2026-06-10T22:30:00Z");
        when(activityDayRepository.findByUser_IdAndDate(USER_ID, LocalDate.of(2026, 6, 11)))
                .thenReturn(Optional.empty());
        when(streakRepository.findById(USER_ID)).thenReturn(Optional.empty());

        service.onTaskCompleted(taskWithEstimate(null));

        assertThat(savedDay().getDate()).isEqualTo(LocalDate.of(2026, 6, 11));
    }

    private GamificationService serviceAt(String instant) {
        Clock clock = Clock.fixed(Instant.parse(instant), ZoneId.of("UTC"));
        // save() returns are ignored by the service; stub leniently so unused stubs don't fail.
        lenient().when(activityDayRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(streakRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        return new GamificationService(activityDayRepository, streakRepository, clock);
    }

    private Task taskWithEstimate(Integer estimateMinutes) {
        Task task = new Task();
        task.setUser(user);
        task.setEstimateMinutes(estimateMinutes);
        return task;
    }

    private ActivityDay day(LocalDate date, int tasksCompleted, boolean goalMet) {
        ActivityDay day = new ActivityDay();
        day.setUser(user);
        day.setDate(date);
        day.setTasksCompleted(tasksCompleted);
        day.setGoalMet(goalMet);
        return day;
    }

    private Streak streak(int current, int longest, LocalDate lastActiveDate) {
        Streak streak = new Streak();
        streak.setUser(user);
        streak.setCurrent(current);
        streak.setLongest(longest);
        streak.setLastActiveDate(lastActiveDate);
        return streak;
    }

    private ActivityDay savedDay() {
        org.mockito.ArgumentCaptor<ActivityDay> captor =
                org.mockito.ArgumentCaptor.forClass(ActivityDay.class);
        org.mockito.Mockito.verify(activityDayRepository).save(captor.capture());
        return captor.getValue();
    }

    private Streak savedStreak() {
        org.mockito.ArgumentCaptor<Streak> captor = org.mockito.ArgumentCaptor.forClass(Streak.class);
        org.mockito.Mockito.verify(streakRepository).save(captor.capture());
        return captor.getValue();
    }
}
