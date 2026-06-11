package com.questline.web;

import com.questline.service.AchievementService;
import com.questline.service.GoalService;
import com.questline.service.StatsService;
import com.questline.service.TopicService;
import com.questline.service.UserService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Account-level operations: export all data (GDPR) and delete the account. */
@RestController
public class AccountController {

    private final UserService userService;
    private final GoalService goalService;
    private final StatsService statsService;
    private final AchievementService achievementService;
    private final TopicService topicService;

    public AccountController(UserService userService, GoalService goalService,
                            StatsService statsService, AchievementService achievementService,
                            TopicService topicService) {
        this.userService = userService;
        this.goalService = goalService;
        this.statsService = statsService;
        this.achievementService = achievementService;
        this.topicService = topicService;
    }

    @GetMapping("/api/me/export")
    public ExportResponse export(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = userId(jwt);
        List<GoalTreeResponse> goals = goalService.list(userId, null).stream()
                .map(goal -> GoalTreeResponse.from(goalService.getTree(userId, goal.getId())))
                .toList();
        return new ExportResponse(
                MeResponse.from(userService.getById(userId)),
                goals,
                StreakResponse.from(statsService.streak(userId)),
                achievementService.unlocked(userId).stream().map(AchievementResponse::from).toList(),
                topicService.progress(userId).stream().map(TopicProgressResponse::from).toList(),
                statsService.heatmap(userId, null, null).stream().map(HeatmapEntry::from).toList());
    }

    @DeleteMapping("/api/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@AuthenticationPrincipal Jwt jwt) {
        userService.deleteAccount(userId(jwt));
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
