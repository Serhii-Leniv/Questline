package com.questline.web;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.questline.AbstractIntegrationTest;
import com.questline.domain.User;
import com.questline.repository.GoalRepository;
import com.questline.repository.UserRepository;
import com.questline.security.TokenService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

/** GDPR account operations: export all data, and delete the account (cascading all owned rows). */
class AccountIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    UserRepository userRepository;

    @Autowired
    GoalRepository goalRepository;

    @Autowired
    TokenService tokenService;

    private User user;
    private String token;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        user = new User();
        user.setEmail("account-test-" + UUID.randomUUID() + "@questline.test");
        user.setName("Account Tester");
        token = tokenService.issue(userRepository.save(user));
    }

    @Test
    void exportReturnsTheUsersData() {
        String goalId = createGoal();
        createTask(goalId);

        given().auth().oauth2(token)
                .when().get("/api/me/export")
                .then().statusCode(200)
                .body("profile.email", equalTo(user.getEmail()))
                .body("goals", hasSize(1))
                .body("goals[0].id", equalTo(goalId));
    }

    @Test
    void deleteAccountRemovesTheUserAndCascadesTheirData() {
        String goalId = createGoal();
        String taskId = createTask(goalId);
        // Completing a task creates streak/activity/achievement rows for the user.
        given().auth().oauth2(token).contentType(ContentType.JSON).body("{ \"status\": \"DONE\" }")
                .when().patch("/api/tasks/" + taskId + "/status").then().statusCode(200);

        given().auth().oauth2(token).when().delete("/api/me").then().statusCode(204);

        // The user is gone (token still parses, but there's no user behind it).
        given().auth().oauth2(token).when().get("/api/me").then().statusCode(404);
        // Owned data cascaded away.
        assertThat(goalRepository.findByUser_IdOrderByCreatedAtDesc(user.getId())).isEmpty();
    }

    @Test
    void exportRequiresAuthentication() {
        given().when().get("/api/me/export").then().statusCode(401);
    }

    private String createGoal() {
        return given().auth().oauth2(token).contentType(ContentType.JSON)
                .body("{ \"title\": \"Goal\" }")
                .when().post("/api/goals")
                .then().statusCode(200).extract().path("id");
    }

    private String createTask(String goalId) {
        return given().auth().oauth2(token).contentType(ContentType.JSON)
                .body("{ \"goalId\": \"" + goalId + "\", \"title\": \"Task\" }")
                .when().post("/api/tasks")
                .then().statusCode(200).extract().path("id");
    }
}
