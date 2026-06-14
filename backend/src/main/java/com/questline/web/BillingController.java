package com.questline.web;

import com.questline.domain.User;
import com.questline.service.AiRateLimiter;
import com.questline.service.BillingService;
import com.questline.service.UserService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Subscription plan: view it, and (mock) upgrade/downgrade. Real Stripe checkout would replace the
 * upgrade flow behind {@link BillingService}.
 */
@RestController
@RequestMapping("/api/me/plan")
public class BillingController {

    private final UserService userService;
    private final BillingService billingService;
    private final AiRateLimiter rateLimiter;

    public BillingController(UserService userService, BillingService billingService,
                            AiRateLimiter rateLimiter) {
        this.userService = userService;
        this.billingService = billingService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping
    public PlanResponse plan(@AuthenticationPrincipal Jwt jwt) {
        return planOf(userId(jwt));
    }

    @PostMapping("/upgrade")
    public PlanResponse upgrade(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = userId(jwt);
        billingService.upgradeToPro(userId);
        return planOf(userId);
    }

    @PostMapping("/downgrade")
    public PlanResponse downgrade(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = userId(jwt);
        billingService.downgradeToFree(userId);
        return planOf(userId);
    }

    private PlanResponse planOf(UUID userId) {
        User user = userService.getById(userId);
        return PlanResponse.of(user, rateLimiter.limitFor(user.getPlan()));
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
