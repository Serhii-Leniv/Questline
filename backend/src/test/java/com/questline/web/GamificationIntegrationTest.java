package com.questline.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

import com.questline.AbstractIntegrationTest;
import com.questline.domain.User;
import com.questline.repository.UserRepository;
import com.questline.security.TokenService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * End-to-end gamification: completing a task persists a streak, XP, and a heatmap day, and the
 * stats endpoints reflect it. Exercises the full controller → service → repository wiring,
 * including the {@code @MapsId} Streak insert.
 */
class GamificationIntegrationTest extends AbstractIntegrationTest {

    private static final ZoneId USER_ZONE = ZoneId.of("Europe/Kyiv");

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
        user.setEmail("gam-test-" + UUID.randomUUID() + "@questline.test");
        user.setName("Gamer");
        token = tokenService.issue(userRepository.save(user));
    }

    @Test
    void completingTaskBuildsStreakXpAndHeatmap() {
        String goalId = given().auth().oauth2(token).contentType(ContentType.JSON)
                .body("{ \"title\": \"Goal\" }")
                .when().post("/api/goals")
                .then().statusCode(200).extract().path("id");

        String taskId = given().auth().oauth2(token).contentType(ContentType.JSON)
                .body("{ \"goalId\": \"" + goalId + "\", \"title\": \"Task\", \"estimateMinutes\": 30 }")
                .when().post("/api/tasks")
                .then().statusCode(200).extract().path("id");

        given().auth().oauth2(token).contentType(ContentType.JSON)
                .body("{ \"status\": \"DONE\" }")
                .when().patch("/api/tasks/" + taskId + "/status")
                .then().statusCode(200).body("status", equalTo("DONE"));

        given().auth().oauth2(token)
                .when().get("/api/stats/streak")
                .then().statusCode(200)
                .body("current", equalTo(1))
                .body("longest", equalTo(1));

        given().auth().oauth2(token)
                .when().get("/api/stats/overview")
                .then().statusCode(200)
                .body("xpTotal", greaterThan(0))
                .body("currentStreak", equalTo(1));

        String today = LocalDate.now(USER_ZONE).toString();
        given().auth().oauth2(token)
                .when().get("/api/stats/heatmap")
                .then().statusCode(200)
                .body("find { it.date == '" + today + "' }.count", equalTo(1));
    }

    @Test
    void togglingStatusDoesNotDoubleCountStreak() {
        String goalId = given().auth().oauth2(token).contentType(ContentType.JSON)
                .body("{ \"title\": \"Goal\" }")
                .when().post("/api/goals")
                .then().statusCode(200).extract().path("id");

        String taskId = given().auth().oauth2(token).contentType(ContentType.JSON)
                .body("{ \"goalId\": \"" + goalId + "\", \"title\": \"Task\" }")
                .when().post("/api/tasks")
                .then().statusCode(200).extract().path("id");

        // Re-opening and re-completing the same task must not count twice.
        for (String status : new String[] {"DONE", "TODO", "DONE"}) {
            given().auth().oauth2(token).contentType(ContentType.JSON)
                    .body("{ \"status\": \"" + status + "\" }")
                    .when().patch("/api/tasks/" + taskId + "/status")
                    .then().statusCode(200);
        }

        String today = LocalDate.now(USER_ZONE).toString();
        given().auth().oauth2(token)
                .when().get("/api/stats/heatmap")
                .then().statusCode(200)
                .body("find { it.date == '" + today + "' }.count", equalTo(1));
    }

    @Test
    void streakRequiresAuthentication() {
        given().when().get("/api/stats/streak").then().statusCode(401);
    }
}
