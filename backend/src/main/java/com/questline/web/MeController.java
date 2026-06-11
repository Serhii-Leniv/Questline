package com.questline.web;

import com.questline.domain.User;
import com.questline.service.AiSettingsService;
import com.questline.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The authenticated user's profile, settings, and BYOK AI provider settings. The user id is read
 * from the JWT subject — never from the request — so a caller only ever touches their own data.
 */
@RestController
public class MeController {

    private final UserService userService;
    private final AiSettingsService aiSettingsService;

    public MeController(UserService userService, AiSettingsService aiSettingsService) {
        this.userService = userService;
        this.aiSettingsService = aiSettingsService;
    }

    @GetMapping("/api/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.getById(userId(jwt));
        return MeResponse.from(user);
    }

    @PatchMapping("/api/me/settings")
    public MeResponse updateSettings(@AuthenticationPrincipal Jwt jwt,
                                     @Valid @RequestBody UpdateSettingsRequest req) {
        User user = userService.updateSettings(userId(jwt),
                req.timezone(), req.dailyCapacityMinutes(), req.dailyTaskGoal());
        return MeResponse.from(user);
    }

    @GetMapping("/api/me/ai-settings")
    public AiSettingsResponse aiSettings(@AuthenticationPrincipal Jwt jwt) {
        return AiSettingsResponse.from(aiSettingsService.status(userId(jwt)));
    }

    @PutMapping("/api/me/ai-settings")
    public AiSettingsResponse updateAiSettings(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody AiSettingsRequest req) {
        UUID userId = userId(jwt);
        aiSettingsService.update(userId, req.baseUrl(), req.model(), req.apiKey());
        return AiSettingsResponse.from(aiSettingsService.status(userId));
    }

    @DeleteMapping("/api/me/ai-settings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearAiSettings(@AuthenticationPrincipal Jwt jwt) {
        aiSettingsService.clear(userId(jwt));
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
