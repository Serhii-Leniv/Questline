package com.questline.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;

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

/** End-to-end: completing a task unlocks FIRST_TASK and it appears on /api/stats/achievements. */
class AchievementsIntegrationTest extends AbstractIntegrationTest {

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
        user.setEmail("ach-test-" + UUID.randomUUID() + "@questline.test");
        user.setName("Achiever");
        token = tokenService.issue(userRepository.save(user));
    }

    @Test
    void firstCompletionUnlocksFirstTaskAchievement() {
        String goalId = given().auth().oauth2(token).contentType(ContentType.JSON)
                .body("{ \"title\": \"Goal\" }")
                .when().post("/api/goals")
                .then().statusCode(200).extract().path("id");

        String taskId = given().auth().oauth2(token).contentType(ContentType.JSON)
                .body("{ \"goalId\": \"" + goalId + "\", \"title\": \"Task\" }")
                .when().post("/api/tasks")
                .then().statusCode(200).extract().path("id");

        // No achievements before any completion.
        given().auth().oauth2(token)
                .when().get("/api/stats/achievements")
                .then().statusCode(200)
                .body("code", org.hamcrest.Matchers.not(hasItem("FIRST_TASK")));

        given().auth().oauth2(token).contentType(ContentType.JSON)
                .body("{ \"status\": \"DONE\" }")
                .when().patch("/api/tasks/" + taskId + "/status")
                .then().statusCode(200);

        given().auth().oauth2(token)
                .when().get("/api/stats/achievements")
                .then().statusCode(200)
                .body("code", hasItem("FIRST_TASK"));
    }

    @Test
    void requiresAuthentication() {
        given().when().get("/api/stats/achievements").then().statusCode(401);
    }
}
