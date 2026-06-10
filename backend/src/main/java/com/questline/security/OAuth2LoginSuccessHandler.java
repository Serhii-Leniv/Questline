package com.questline.security;

import com.questline.domain.User;
import com.questline.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * After Google sign-in: provision the user, mint a JWT, and redirect the browser back to the
 * SPA callback with the token in the URL fragment (never logged server-side, never persisted).
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final TokenService tokenService;
    private final AppProperties props;

    public OAuth2LoginSuccessHandler(UserService userService, TokenService tokenService,
                                     AppProperties props) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.props = props;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        String image = principal.getAttribute("picture");

        User user = userService.provisionFromOAuth(email, name, image);
        String token = tokenService.issue(user);

        String target = UriComponentsBuilder.fromUriString(props.frontendUrl())
                .path("/login/callback")
                .fragment("token=" + token)
                .build()
                .toUriString();
        response.sendRedirect(target);
    }
}
