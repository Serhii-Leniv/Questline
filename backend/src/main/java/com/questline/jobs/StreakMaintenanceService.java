package com.questline.jobs;

import com.questline.domain.Streak;
import com.questline.repository.StreakRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.jobrunr.jobs.annotations.Recurring;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recurring safety net that decays a streak once its gap can no longer be covered by freezes, so
 * the dashboard reflects a broken streak at the day boundary instead of waiting for the next
 * completion. Runs hourly to approximate each user's local midnight across timezones.
 *
 * <p>Freezes are not consumed here — that happens on the next completion (see
 * {@code GamificationService}); this only zeroes streaks that are already beyond saving.
 */
@Service
public class StreakMaintenanceService {

    private final StreakRepository streakRepository;
    private final Clock clock;

    public StreakMaintenanceService(StreakRepository streakRepository, Clock clock) {
        this.streakRepository = streakRepository;
        this.clock = clock;
    }

    @Recurring(id = "streak-maintenance", cron = "0 * * * *")
    @Transactional
    public void resetStaleStreaks() {
        for (Streak streak : streakRepository.findByCurrentGreaterThan(0)) {
            LocalDate last = streak.getLastActiveDate();
            if (last == null) {
                continue;
            }
            LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(streak.getUser().getTimezone())));
            long missedDays = ChronoUnit.DAYS.between(last, today) - 1; // full days missed before today
            if (missedDays > streak.getFreezesAvailable()) {
                streak.setCurrent(0);
            }
        }
    }
}
