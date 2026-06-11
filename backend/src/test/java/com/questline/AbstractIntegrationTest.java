package com.questline;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Boots the full Spring context against a real Postgres in Testcontainers, with Flyway running
 * the migrations. Subclasses get a ready application on a random port.
 *
 * <p>The container is a <b>singleton</b> started once in a static initializer and shared across
 * every test class — Ryuk stops it at JVM exit. A per-class {@code @Container} would start (and
 * tear down) a fresh Postgres for each IT class, which is slow and, on some Docker hosts, flaky
 * when two containers' lifecycles overlap.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Dummy AI key so the provider autoconfig wires up; no network call is ever made.
        registry.add("spring.ai.google.genai.api-key", () -> "test-key");
        // No background worker needed for context/API tests.
        registry.add("org.jobrunr.background-job-server.enabled", () -> "false");
    }
}
