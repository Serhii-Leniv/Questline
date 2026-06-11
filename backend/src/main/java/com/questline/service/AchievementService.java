package com.questline.service;

import com.questline.domain.Achievement;
import com.questline.domain.Streak;
import com.questline.domain.User;
import com.questline.domain.UserAchievement;
import com.questline.repository.AchievementRepository;
import com.questline.repository.UserAchievementRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grants achievements from gamification events. Each grant is idempotent (guarded by the unique
 * (user, achievement) constraint), so {@link #evaluate} can be called on every task completion.
 *
 * <p>Current rules are streak/completion based. Goal- and milestone-completion achievements arrive
 * once goal progress tracking exists.
 */
@Service
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final Clock clock;

    public AchievementService(AchievementRepository achievementRepository,
                              UserAchievementRepository userAchievementRepository, Clock clock) {
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.clock = clock;
    }

    /** Grants any newly-earned achievements for the user given their current streak. */
    public void evaluate(User user, Streak streak) {
        grantIfAbsent(user, "FIRST_TASK");
        int current = streak.getCurrent();
        if (current >= 3) grantIfAbsent(user, "STREAK_3");
        if (current >= 7) grantIfAbsent(user, "STREAK_7");
        if (current >= 30) grantIfAbsent(user, "STREAK_30");
        if (current >= 100) grantIfAbsent(user, "STREAK_100");
    }

    /** Grants a single achievement by code if the user doesn't already hold it. */
    public void grant(User user, String code) {
        grantIfAbsent(user, code);
    }

    @Transactional(readOnly = true)
    public List<UserAchievement> unlocked(UUID userId) {
        return userAchievementRepository.findByUser_IdOrderByUnlockedAtDesc(userId);
    }

    private void grantIfAbsent(User user, String code) {
        if (userAchievementRepository.existsByUser_IdAndAchievement_Code(user.getId(), code)) {
            return;
        }
        Achievement achievement = achievementRepository.findByCode(code).orElse(null);
        if (achievement == null) {
            return; // catalog missing this code — nothing to grant
        }
        UserAchievement unlock = new UserAchievement();
        unlock.setUser(user);
        unlock.setAchievement(achievement);
        unlock.setUnlockedAt(Instant.now(clock));
        userAchievementRepository.save(unlock);
    }
}
