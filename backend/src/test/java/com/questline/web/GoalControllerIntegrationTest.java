package com.questline.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
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
 * Covers goal CRUD over real HTTP with a real Postgres, and the core multi-tenancy rule:
 * one user can never see or mutate another user's goal.
 */
class GoalControllerIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TokenService tokenService;

    private String ownerToken;
    private String otherToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        ownerToken = tokenForNewUser();
        otherToken = tokenForNewUser();
    }

    @Test
    void createsAndFetchesGoalTree() {
        String id = given().auth().oauth2(ownerToken)
                .contentType(ContentType.JSON)
                .body("""
                        { "title": "Become senior", "context": "mid-level", "target": "senior" }
                        """)
                .when().post("/api/goals")
                .then().statusCode(200)
                .body("id", notNullValue())
                .body("title", equalTo("Become senior"))
                .body("status", equalTo("ACTIVE"))
                .extract().path("id");

        given().auth().oauth2(ownerToken)
                .when().get("/api/goals/" + id)
                .then().statusCode(200)
                .body("id", equalTo(id))
                .body("milestones", hasSize(0));
    }

    @Test
    void goalsAreScopedToOwner() {
        String id = createGoal(ownerToken, "Mine");

        given().auth().oauth2(otherToken)
                .when().get("/api/goals/" + id)
                .then().statusCode(404)
                .body("code", equalTo("NOT_FOUND"));

        given().auth().oauth2(otherToken)
                .when().post("/api/goals/" + id + "/archive")
                .then().statusCode(404);
    }

    @Test
    void listFiltersByStatus() {
        String id = createGoal(ownerToken, "To archive");

        given().auth().oauth2(ownerToken)
                .when().post("/api/goals/" + id + "/archive")
                .then().statusCode(200)
                .body("status", equalTo("ARCHIVED"));

        given().auth().oauth2(ownerToken).queryParam("status", "ARCHIVED")
                .when().get("/api/goals")
                .then().statusCode(200)
                .body("findAll { it.id == '" + id + "' }", hasSize(1));

        given().auth().oauth2(ownerToken).queryParam("status", "ACTIVE")
                .when().get("/api/goals")
                .then().statusCode(200)
                .body("findAll { it.id == '" + id + "' }", hasSize(0));
    }

    @Test
    void rejectsBlankTitle() {
        given().auth().oauth2(ownerToken)
                .contentType(ContentType.JSON)
                .body("{ \"title\": \"\" }")
                .when().post("/api/goals")
                .then().statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void requiresAuthentication() {
        given().when().get("/api/goals").then().statusCode(401);
    }

    private String createGoal(String token, String title) {
        return given().auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body("{ \"title\": \"" + title + "\" }")
                .when().post("/api/goals")
                .then().statusCode(200)
                .extract().path("id");
    }

    private String tokenForNewUser() {
        User user = new User();
        user.setEmail("goal-test-" + UUID.randomUUID() + "@questline.test");
        user.setName("Goal Tester");
        return tokenService.issue(userRepository.save(user));
    }
}
