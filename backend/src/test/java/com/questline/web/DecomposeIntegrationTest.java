package com.questline.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

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

/**
 * Flow C over HTTP: requesting a decompose enqueues a PENDING job for the task. The actual
 * subtask persistence runs in the (disabled-in-tests) worker, so it is unit-tested separately.
 */
class DecomposeIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TokenService tokenService;

    private String token;
    private String otherToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        token = tokenFor();
        otherToken = tokenFor();
    }

    @Test
    void decomposeEnqueuesAJobForTheTask() {
        String goalId = createGoal(token);
        String taskId = createTask(token, goalId);

        String jobId = given().auth().oauth2(token)
                .when().post("/api/ai/tasks/" + taskId + "/decompose")
                .then().statusCode(200)
                .body("jobId", notNullValue())
                .body("goalId", equalTo(goalId))
                .extract().path("jobId");

        given().auth().oauth2(token)
                .when().get("/api/ai/jobs/" + jobId)
                .then().statusCode(200)
                .body("status", equalTo("PENDING"))
                .body("type", equalTo("DECOMPOSE_TASK"));
    }

    @Test
    void cannotDecomposeAnotherUsersTask() {
        String goalId = createGoal(token);
        String taskId = createTask(token, goalId);

        given().auth().oauth2(otherToken)
                .when().post("/api/ai/tasks/" + taskId + "/decompose")
                .then().statusCode(404);
    }

    private String createGoal(String t) {
        return given().auth().oauth2(t).contentType(ContentType.JSON)
                .body("{ \"title\": \"Goal\" }")
                .when().post("/api/goals")
                .then().statusCode(200).extract().path("id");
    }

    private String createTask(String t, String goalId) {
        return given().auth().oauth2(t).contentType(ContentType.JSON)
                .body("{ \"goalId\": \"" + goalId + "\", \"title\": \"Build the thing\" }")
                .when().post("/api/tasks")
                .then().statusCode(200).extract().path("id");
    }

    private String tokenFor() {
        User user = new User();
        user.setEmail("decompose-" + UUID.randomUUID() + "@questline.test");
        user.setName("Decomposer");
        return tokenService.issue(userRepository.save(user));
    }
}
