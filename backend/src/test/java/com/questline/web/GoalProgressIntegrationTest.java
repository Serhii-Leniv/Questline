package com.questline.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
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

/** Completing every task of a goal drives its progress to 100%, marks it COMPLETED, unlocks GOAL_DONE. */
class GoalProgressIntegrationTest extends AbstractIntegrationTest {

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
        user.setEmail("progress-test-" + UUID.randomUUID() + "@questline.test");
        user.setName("Progress Tester");
        token = tokenService.issue(userRepository.save(user));
    }

    @Test
    void completingAllTasksCompletesTheGoal() {
        String goalId = createGoal();
        String t1 = createTask(goalId, "A");
        String t2 = createTask(goalId, "B");

        complete(t1);
        // Halfway: progress 0.5, still active.
        given().auth().oauth2(token).when().get("/api/goals/" + goalId)
                .then().statusCode(200)
                .body("progress", equalTo(0.5f))
                .body("status", equalTo("ACTIVE"));

        complete(t2);
        given().auth().oauth2(token).when().get("/api/goals/" + goalId)
                .then().statusCode(200)
                .body("progress", equalTo(1.0f))
                .body("status", equalTo("COMPLETED"));

        given().auth().oauth2(token).when().get("/api/stats/achievements")
                .then().statusCode(200)
                .body("code", hasItem("GOAL_DONE"));
    }

    private String createGoal() {
        return given().auth().oauth2(token).contentType(ContentType.JSON)
                .body("{ \"title\": \"Goal\" }")
                .when().post("/api/goals")
                .then().statusCode(200).extract().path("id");
    }

    private String createTask(String goalId, String title) {
        return given().auth().oauth2(token).contentType(ContentType.JSON)
                .body("{ \"goalId\": \"" + goalId + "\", \"title\": \"" + title + "\" }")
                .when().post("/api/tasks")
                .then().statusCode(200).extract().path("id");
    }

    private void complete(String taskId) {
        given().auth().oauth2(token).contentType(ContentType.JSON)
                .body("{ \"status\": \"DONE\" }")
                .when().patch("/api/tasks/" + taskId + "/status")
                .then().statusCode(200);
    }
}
