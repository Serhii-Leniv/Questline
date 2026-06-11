package com.questline.jobs;

import com.questline.common.Notifier;
import com.questline.domain.ActivityDay;
import com.questline.domain.Streak;
import com.questline.domain.User;
import com.questline.repository.ActivityDayRepository;
import com.questline.repository.StreakRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.jobrunr.jobs.annotations.Recurring;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends an evening "streak at risk" reminder to users who have an active streak but haven't met
 * today's goal yet. Runs hourly and fires at each user's local {@value #REMINDER_HOUR}:00, so a
 * user is reminded once per day in their own timezone.
 */
@Service
public class ReminderService {

    private static final int REMINDER_HOUR = 20;

    private final StreakRepository streakRepository;
    private final ActivityDayRepository activityDayRepository;
    private final Notifier notifier;
    private final Clock clock;

    public ReminderService(StreakRepository streakRepository,
                           ActivityDayRepository activityDayRepository, Notifier notifier, Clock clock) {
        this.streakRepository = streakRepository;
        this.activityDayRepository = activityDayRepository;
        this.notifier = notifier;
        this.clock = clock;
    }

    @Recurring(id = "evening-reminders", cron = "0 * * * *")
    @Transactional(readOnly = true)
    public void sendEveningReminders() {
        for (Streak streak : streakRepository.findByCurrentGreaterThan(0)) {
            User user = streak.getUser();
            ZonedDateTime localNow = ZonedDateTime.now(clock.withZone(ZoneId.of(user.getTimezone())));
            if (localNow.getHour() != REMINDER_HOUR) {
                continue;
            }
            LocalDate today = localNow.toLocalDate();
            boolean goalMet = activityDayRepository.findByUser_IdAndDate(user.getId(), today)
                    .map(ActivityDay::isGoalMet)
                    .orElse(false);
            if (!goalMet) {
                notifier.send(user.getEmail(), "Your streak is at risk",
                        "You have a " + streak.getCurrent()
                                + "-day streak. Complete a task today to keep it going!");
            }
        }
    }
}
