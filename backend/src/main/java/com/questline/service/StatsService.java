package com.questline.service;

import com.questline.common.NotFoundException;
import com.questline.domain.ActivityDay;
import com.questline.domain.Streak;
import com.questline.domain.User;
import com.questline.repository.ActivityDayRepository;
import com.questline.repository.StreakRepository;
import com.questline.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only views over gamification state: streak, heatmap, and a dashboard overview. */
@Service
public class StatsService {

    private static final int HEATMAP_DEFAULT_DAYS = 365;

    private final StreakRepository streakRepository;
    private final ActivityDayRepository activityDayRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public StatsService(StreakRepository streakRepository,
                        ActivityDayRepository activityDayRepository,
                        UserRepository userRepository, Clock clock) {
        this.streakRepository = streakRepository;
        this.activityDayRepository = activityDayRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    /** Returns the user's streak, or a zeroed transient one if none has formed yet. */
    @Transactional(readOnly = true)
    public Streak streak(UUID userId) {
        return streakRepository.findById(userId).orElseGet(Streak::new);
    }

    /**
     * Activity within {@code [from, to]} (user-local dates). Defaults to the trailing
     * {@value #HEATMAP_DEFAULT_DAYS} days ending today in the user's timezone.
     */
    @Transactional(readOnly = true)
    public List<ActivityDay> heatmap(UUID userId, LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : today(userId);
        LocalDate start = from != null ? from : end.minusDays(HEATMAP_DEFAULT_DAYS - 1L);
        return activityDayRepository.findByUser_IdAndDateBetweenOrderByDateAsc(userId, start, end);
    }

    @Transactional(readOnly = true)
    public User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    /** Level curve from SPEC §10.2: {@code floor(sqrt(xpTotal / 100))}. */
    public static int levelFor(long xpTotal) {
        return (int) Math.floor(Math.sqrt(xpTotal / 100.0));
    }

    private LocalDate today(UUID userId) {
        User user = getUser(userId);
        return LocalDate.now(clock.withZone(ZoneId.of(user.getTimezone())));
    }
}
