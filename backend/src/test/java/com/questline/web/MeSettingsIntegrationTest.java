package com.questline.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

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

/** Covers PATCH /api/me/settings, including IANA-timezone validation (drives streak math). */
class MeSettingsIntegrationTest extends AbstractIntegrationTest {

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
        user.setEmail("settings-test-" + UUID.randomUUID() + "@questline.test");
        user.setName("Settings Tester");
        token = tokenService.issue(userRepository.save(user));
    }

    @Test
    void updatesSettings() {
        given().auth().oauth2(token).contentType(ContentType.JSON)
                .body("{ \"timezone\": \"America/New_York\", \"dailyTaskGoal\": 3 }")
                .when().patch("/api/me/settings")
                .then().statusCode(200)
                .body("timezone", equalTo("America/New_York"))
                .body("dailyTaskGoal", equalTo(3));

        // Persisted: a fresh read reflects the change.
        given().auth().oauth2(token)
                .when().get("/api/me")
                .then().statusCode(200)
                .body("timezone", equalTo("America/New_York"))
                .body("dailyTaskGoal", equalTo(3));
    }

    @Test
    void rejectsUnknownTimezone() {
        given().auth().oauth2(token).contentType(ContentType.JSON)
                .body("{ \"timezone\": \"Mars/Olympus_Mons\" }")
                .when().patch("/api/me/settings")
                .then().statusCode(400)
                .body("code", equalTo("INVALID_TIMEZONE"));
    }

    @Test
    void rejectsNonPositiveDailyTaskGoal() {
        given().auth().oauth2(token).contentType(ContentType.JSON)
                .body("{ \"dailyTaskGoal\": 0 }")
                .when().patch("/api/me/settings")
                .then().statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void requiresAuthentication() {
        given().contentType(ContentType.JSON).body("{ \"dailyTaskGoal\": 2 }")
                .when().patch("/api/me/settings")
                .then().statusCode(401);
    }
}
