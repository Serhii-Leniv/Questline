package com.questline.service;

import com.questline.domain.ActivityDay;
import com.questline.domain.Streak;
import com.questline.domain.Task;
import com.questline.domain.User;
import com.questline.repository.ActivityDayRepository;
import com.questline.repository.StreakRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deterministic gamification rules (SPEC §10). On task completion it records the day's activity,
 * advances the streak, and awards XP — all in the user's IANA timezone (local midnight boundary).
 *
 * <p>Scope: ActivityDay + Streak (with freezes) + XP. Achievements and the recurring
 * "streak broken" job are still Phase 2.
 */
@Service
public class GamificationService {

    private static final int XP_PER_TASK = 10;
    /** Streak bonus caps at +60% (streak of 30+). */
    private static final int STREAK_BONUS_CAP_DAYS = 30;
    private static final double STREAK_BONUS_PER_DAY = 0.02;
    /** One freeze is granted every 7 days of streak, up to a cap of 3. */
    private static final int FREEZE_GRANT_INTERVAL = 7;
    private static final int FREEZE_CAP = 3;

    private final ActivityDayRepository activityDayRepository;
    private final StreakRepository streakRepository;
    private final AchievementService achievementService;
    private final Clock clock;

    public GamificationService(ActivityDayRepository activityDayRepository,
                               StreakRepository streakRepository,
                               AchievementService achievementService, Clock clock) {
        this.activityDayRepository = activityDayRepository;
        this.streakRepository = streakRepository;
        this.achievementService = achievementService;
        this.clock = clock;
    }

    /**
     * Called when a task transitions to DONE. Idempotency is the caller's responsibility: only
     * invoke this on a genuine non-DONE → DONE transition, never on a re-save of an already-done
     * task.
     */
    @Transactional
    public void onTaskCompleted(Task task) {
        User user = task.getUser();
        UUID userId = user.getId();
        LocalDate localDate = LocalDate.now(clock.withZone(ZoneId.of(user.getTimezone())));

        ActivityDay day = activityDayRepository.findByUser_IdAndDate(userId, localDate)
                .orElseGet(() -> newDay(user, localDate));
        day.setTasksCompleted(day.getTasksCompleted() + 1);
        if (task.getEstimateMinutes() != null) {
            day.setMinutesSpent(day.getMinutesSpent() + task.getEstimateMinutes());
        }

        boolean wasMet = day.isGoalMet();
        boolean nowMet = day.getTasksCompleted() >= user.getDailyTaskGoal();
        day.setGoalMet(nowMet);

        Streak streak = streakRepository.findById(userId).orElseGet(() -> newStreak(user));
        // The streak advances only the first time the day's goal is met.
        if (nowMet && !wasMet) {
            advanceStreak(streak, localDate);
        }

        int xp = (int) Math.round(XP_PER_TASK * streakMultiplier(streak.getCurrent()));
        day.setXpEarned(day.getXpEarned() + xp);
        user.setXpTotal(user.getXpTotal() + xp);

        activityDayRepository.save(day);
        streakRepository.save(streak);
        achievementService.evaluate(user, streak);
    }

    private static void advanceStreak(Streak streak, LocalDate localDate) {
        LocalDate last = streak.getLastActiveDate();
        if (last != null && !last.isBefore(localDate)) {
            return; // already advanced today (or clock skew) — never double-count
        }
        if (last == null) {
            streak.setCurrent(1);
        } else {
            long missedDays = ChronoUnit.DAYS.between(last, localDate) - 1; // 0 when consecutive
            if (missedDays <= 0) {
                streak.setCurrent(streak.getCurrent() + 1);
            } else if (missedDays <= streak.getFreezesAvailable()) {
                // Spend one freeze per missed day to keep the chain alive.
                streak.setFreezesAvailable(streak.getFreezesAvailable() - (int) missedDays);
                streak.setCurrent(streak.getCurrent() + 1);
            } else {
                streak.setCurrent(1); // gap too large to cover — chain broken
            }
        }
        streak.setLastActiveDate(localDate);
        streak.setLongest(Math.max(streak.getLongest(), streak.getCurrent()));
        grantFreezeIfThreshold(streak);
    }

    private static void grantFreezeIfThreshold(Streak streak) {
        if (streak.getCurrent() > 0 && streak.getCurrent() % FREEZE_GRANT_INTERVAL == 0) {
            streak.setFreezesAvailable(Math.min(FREEZE_CAP, streak.getFreezesAvailable() + 1));
        }
    }

    private static double streakMultiplier(int current) {
        return 1 + Math.min(current, STREAK_BONUS_CAP_DAYS) * STREAK_BONUS_PER_DAY;
    }

    private static ActivityDay newDay(User user, LocalDate date) {
        ActivityDay day = new ActivityDay();
        day.setUser(user);
        day.setDate(date);
        return day;
    }

    private static Streak newStreak(User user) {
        Streak streak = new Streak();
        streak.setUser(user); // @MapsId derives the id from the user
        return streak;
    }
}
