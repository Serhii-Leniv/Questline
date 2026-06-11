package com.questline.web;

import com.questline.domain.Streak;
import com.questline.domain.User;
import com.questline.service.AchievementService;
import com.questline.service.StatsService;
import com.questline.service.TopicService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only gamification stats for the dashboard. All data is scoped to the authenticated user.
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;
    private final AchievementService achievementService;
    private final TopicService topicService;

    public StatsController(StatsService statsService, AchievementService achievementService,
                          TopicService topicService) {
        this.statsService = statsService;
        this.achievementService = achievementService;
        this.topicService = topicService;
    }

    @GetMapping("/streak")
    public StreakResponse streak(@AuthenticationPrincipal Jwt jwt) {
        return StreakResponse.from(statsService.streak(userId(jwt)));
    }

    @GetMapping("/heatmap")
    public List<HeatmapEntry> heatmap(@AuthenticationPrincipal Jwt jwt,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return statsService.heatmap(userId(jwt), from, to).stream().map(HeatmapEntry::from).toList();
    }

    @GetMapping("/overview")
    public OverviewResponse overview(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = userId(jwt);
        User user = statsService.getUser(userId);
        Streak streak = statsService.streak(userId);
        return new OverviewResponse(user.getXpTotal(), StatsService.levelFor(user.getXpTotal()),
                streak.getCurrent(), streak.getLongest(), streak.getFreezesAvailable());
    }

    @GetMapping("/achievements")
    public List<AchievementResponse> achievements(@AuthenticationPrincipal Jwt jwt) {
        return achievementService.unlocked(userId(jwt)).stream().map(AchievementResponse::from).toList();
    }

    @GetMapping("/topics")
    public List<TopicProgressResponse> topics(@AuthenticationPrincipal Jwt jwt) {
        return topicService.progress(userId(jwt)).stream().map(TopicProgressResponse::from).toList();
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
