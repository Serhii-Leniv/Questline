package com.questline.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.questline.domain.Achievement;
import com.questline.domain.Streak;
import com.questline.domain.User;
import com.questline.repository.AchievementRepository;
import com.questline.repository.UserAchievementRepository;
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
class AchievementServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    AchievementRepository achievementRepository;

    @Mock
    UserAchievementRepository userAchievementRepository;

    private AchievementService service;
    private User user;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-11T12:00:00Z"), ZoneId.of("UTC"));
        service = new AchievementService(achievementRepository, userAchievementRepository, clock);
        user = new User();
        user.setId(USER_ID);
    }

    @Test
    void grantsFirstTaskWhenAbsent() {
        when(userAchievementRepository.existsByUser_IdAndAchievement_Code(USER_ID, "FIRST_TASK"))
                .thenReturn(false);
        when(achievementRepository.findByCode("FIRST_TASK"))
                .thenReturn(Optional.of(achievement("FIRST_TASK")));

        service.evaluate(user, streak(0));

        verify(userAchievementRepository).save(any());
    }

    @Test
    void doesNotRegrantWhenAlreadyHeld() {
        when(userAchievementRepository.existsByUser_IdAndAchievement_Code(USER_ID, "FIRST_TASK"))
                .thenReturn(true);

        service.evaluate(user, streak(0));

        verify(userAchievementRepository, never()).save(any());
    }

    @Test
    void grantsEveryStreakThresholdUpToCurrent() {
        when(userAchievementRepository.existsByUser_IdAndAchievement_Code(eq(USER_ID), anyString()))
                .thenReturn(false);
        when(achievementRepository.findByCode(anyString()))
                .thenReturn(Optional.of(achievement("X")));

        service.evaluate(user, streak(7));

        // FIRST_TASK + STREAK_3 + STREAK_7 (not STREAK_30/100).
        verify(userAchievementRepository, times(3)).save(any());
    }

    @Test
    void skipsCodesMissingFromCatalog() {
        when(userAchievementRepository.existsByUser_IdAndAchievement_Code(eq(USER_ID), anyString()))
                .thenReturn(false);
        when(achievementRepository.findByCode(anyString())).thenReturn(Optional.empty());

        service.evaluate(user, streak(0));

        verify(userAchievementRepository, never()).save(any());
    }

    private Achievement achievement(String code) {
        Achievement a = new Achievement();
        a.setCode(code);
        a.setTitle(code);
        return a;
    }

    private Streak streak(int current) {
        Streak streak = new Streak();
        streak.setUser(user);
        streak.setCurrent(current);
        return streak;
    }
}
