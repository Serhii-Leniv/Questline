package com.questline.jobs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.questline.common.Notifier;
import com.questline.domain.ActivityDay;
import com.questline.domain.Streak;
import com.questline.domain.User;
import com.questline.repository.ActivityDayRepository;
import com.questline.repository.StreakRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    StreakRepository streakRepository;

    @Mock
    ActivityDayRepository activityDayRepository;

    @Mock
    Notifier notifier;

    private final User user = user();

    /** 17:00 UTC = 20:00 in Kyiv (UTC+3) — the reminder hour. */
    private ReminderService serviceAtEvening() {
        return new ReminderService(streakRepository, activityDayRepository, notifier,
                Clock.fixed(Instant.parse("2026-06-11T17:00:00Z"), ZoneId.of("UTC")));
    }

    @Test
    void remindsWhenStreakAtRiskInTheEvening() {
        when(streakRepository.findByCurrentGreaterThan(0)).thenReturn(List.of(streak(5)));
        when(activityDayRepository.findByUser_IdAndDate(user.getId(), LocalDate.of(2026, 6, 11)))
                .thenReturn(Optional.empty()); // no activity today → goal not met

        serviceAtEvening().sendEveningReminders();

        verify(notifier).send(eq(user.getEmail()), anyString(), anyString());
    }

    @Test
    void doesNotRemindWhenTodaysGoalIsMet() {
        ActivityDay day = new ActivityDay();
        day.setGoalMet(true);
        when(streakRepository.findByCurrentGreaterThan(0)).thenReturn(List.of(streak(5)));
        when(activityDayRepository.findByUser_IdAndDate(user.getId(), LocalDate.of(2026, 6, 11)))
                .thenReturn(Optional.of(day));

        serviceAtEvening().sendEveningReminders();

        verify(notifier, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void doesNotRemindOutsideTheEveningHour() {
        // 09:00 UTC = 12:00 in Kyiv — not the reminder hour.
        ReminderService service = new ReminderService(streakRepository, activityDayRepository, notifier,
                Clock.fixed(Instant.parse("2026-06-11T09:00:00Z"), ZoneId.of("UTC")));
        when(streakRepository.findByCurrentGreaterThan(0)).thenReturn(List.of(streak(5)));

        service.sendEveningReminders();

        verify(notifier, never()).send(anyString(), any(), any());
    }

    private Streak streak(int current) {
        Streak streak = new Streak();
        streak.setUser(user);
        streak.setCurrent(current);
        return streak;
    }

    private static User user() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("streaker@questline.test");
        u.setTimezone("Europe/Kyiv");
        return u;
    }
}
