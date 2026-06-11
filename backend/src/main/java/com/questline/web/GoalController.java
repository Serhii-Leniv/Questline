package com.questline.web;

import com.questline.domain.Goal;
import com.questline.domain.GoalStatus;
import com.questline.service.GoalService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Goal CRUD. Thin: resolves the user id from the JWT subject and delegates to {@link GoalService}.
 * The user id is never read from the request, so a caller can only ever touch their own goals.
 */
@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @GetMapping
    public List<GoalResponse> list(@AuthenticationPrincipal Jwt jwt,
                                   @RequestParam(required = false) GoalStatus status) {
        return goalService.list(userId(jwt), status).stream().map(GoalResponse::from).toList();
    }

    @PostMapping
    public GoalResponse create(@AuthenticationPrincipal Jwt jwt,
                               @Valid @RequestBody CreateGoalRequest req) {
        Goal goal = goalService.create(userId(jwt), req.title(), req.description(),
                req.context(), req.target(), req.targetDate());
        return GoalResponse.from(goal);
    }

    @GetMapping("/{id}")
    public GoalTreeResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return GoalTreeResponse.from(goalService.getTree(userId(jwt), id));
    }

    @PatchMapping("/{id}")
    public GoalResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                               @Valid @RequestBody UpdateGoalRequest req) {
        Goal goal = goalService.update(userId(jwt), id, req.title(), req.description(),
                req.context(), req.target(), req.targetDate(), req.status());
        return GoalResponse.from(goal);
    }

    @PostMapping("/{id}/archive")
    public GoalResponse archive(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return GoalResponse.from(goalService.archive(userId(jwt), id));
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
