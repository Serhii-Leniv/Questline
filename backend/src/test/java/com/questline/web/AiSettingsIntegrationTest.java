package com.questline.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;

import com.questline.AbstractIntegrationTest;
import com.questline.domain.User;
import com.questline.repository.UserRepository;
import com.questline.security.TokenService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

/** BYOK settings round-trip: store / read / clear, and the API key is never returned. */
class AiSettingsIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TokenService tokenService;

    private String token;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        User user = new User();
        user.setEmail("byok-test-" + UUID.randomUUID() + "@questline.test");
        user.setName("BYOK");
        token = tokenService.issue(userRepository.save(user));
    }

    @Test
    void storesReadsAndClearsByokSettings_withoutEverReturningTheKey() {
        // Initially unconfigured.
        given().auth().oauth2(token).when().get("/api/me/ai-settings")
                .then().statusCode(200).body("configured", equalTo(false));

        // Store provider settings.
        given().auth().oauth2(token).contentType(ContentType.JSON)
                .body("{ \"baseUrl\": \"https://openrouter.ai/api/v1\","
                        + " \"model\": \"openai/gpt-4o-mini\", \"apiKey\": \"sk-or-secret\" }")
                .when().put("/api/me/ai-settings")
                .then().statusCode(200)
                .body("configured", equalTo(true))
                .body("baseUrl", equalTo("https://openrouter.ai/api/v1"))
                .body("model", equalTo("openai/gpt-4o-mini"))
                .body("$", not(hasKey("apiKey")));   // key never returned

        given().auth().oauth2(token).when().get("/api/me/ai-settings")
                .then().statusCode(200)
                .body("configured", equalTo(true))
                .body("$", not(hasKey("apiKey")));

        // Clear.
        given().auth().oauth2(token).when().delete("/api/me/ai-settings")
                .then().statusCode(204);
        given().auth().oauth2(token).when().get("/api/me/ai-settings")
                .then().statusCode(200).body("configured", equalTo(false));
    }

    @Test
    void rejectsMissingBaseUrlOrModel() {
        given().auth().oauth2(token).contentType(ContentType.JSON)
                .body("{ \"apiKey\": \"sk-x\" }")
                .when().put("/api/me/ai-settings")
                .then().statusCode(400).body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void requiresAuthentication() {
        given().when().get("/api/me/ai-settings").then().statusCode(401);
    }
}
