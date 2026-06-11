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
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deterministic gamification rules (SPEC §10). On task completion it records the day's activity,
 * advances the streak, and awards XP — all in the user's IANA timezone (local midnight boundary).
 *
 * <p>MVP scope: ActivityDay + Streak + XP. Streak freezes, achievements, and the recurring
 * "streak broken" job are Phase 2.
 */
@Service
public class GamificationService {

    private static final int XP_PER_TASK = 10;
    /** Streak bonus caps at +60% (streak of 30+). */
    private static final int STREAK_BONUS_CAP_DAYS = 30;
    private static final double STREAK_BONUS_PER_DAY = 0.02;

    private final ActivityDayRepository activityDayRepository;
    private final StreakRepository streakRepository;
    private final Clock clock;

    public GamificationService(ActivityDayRepository activityDayRepository,
                               StreakRepository streakRepository, Clock clock) {
        this.activityDayRepository = activityDayRepository;
        this.streakRepository = streakRepository;
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
    }

    private static void advanceStreak(Streak streak, LocalDate localDate) {
        LocalDate last = streak.getLastActiveDate();
        if (last != null && last.equals(localDate)) {
            return; // already advanced today
        }
        if (last != null && last.equals(localDate.minusDays(1))) {
            streak.setCurrent(streak.getCurrent() + 1);
        } else {
            // First active day ever, or a gap broke the chain (MVP has no freezes).
            streak.setCurrent(1);
        }
        streak.setLastActiveDate(localDate);
        streak.setLongest(Math.max(streak.getLongest(), streak.getCurrent()));
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
