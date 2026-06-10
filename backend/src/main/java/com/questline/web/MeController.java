package com.questline.web;

import com.questline.domain.User;
import com.questline.service.UserService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Returns the authenticated user. The user id is read from the JWT subject — never from the
 * request — so a caller can only ever read their own profile.
 */
@RestController
public class MeController {

    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.getById(UUID.fromString(jwt.getSubject()));
        return MeResponse.from(user);
    }
}
