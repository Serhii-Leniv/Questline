package com.questline.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed {@code app.*} configuration.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, String frontendUrl) {

    public record Jwt(String secret, String issuer, Duration ttl) {
    }
}
