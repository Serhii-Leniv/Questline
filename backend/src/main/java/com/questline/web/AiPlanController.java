package com.questline.web;

import com.questline.service.AiPlanService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI plan endpoints (Flow A). Generation runs as a JobRunr job; the client polls {@code /jobs}
 * and then accepts the plan to persist it. The user id always comes from the JWT.
 */
@RestController
@RequestMapping("/api/ai")
public class AiPlanController {

    private final AiPlanService aiPlanService;

    public AiPlanController(AiPlanService aiPlanService) {
        this.aiPlanService = aiPlanService;
    }

    @PostMapping("/plans")
    public PlanJobResponse generate(@AuthenticationPrincipal Jwt jwt,
                                    @Valid @RequestBody GoalInput input) {
        return PlanJobResponse.from(aiPlanService.startPlan(userId(jwt), input.context(),
                input.target(), input.targetDate(), input.weeklyCapacityMinutes()));
    }

    @GetMapping("/jobs/{jobId}")
    public AiJobResponse job(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID jobId) {
        return AiJobResponse.from(aiPlanService.getJob(userId(jwt), jobId));
    }

    @PostMapping("/plans/{goalId}/accept")
    public GoalTreeResponse accept(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID goalId) {
        return GoalTreeResponse.from(aiPlanService.accept(userId(jwt), goalId));
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
