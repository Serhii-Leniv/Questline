package com.questline.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

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
 * Covers task CRUD, the user-timezone "today" view, status/schedule/reorder, and the
 * multi-tenancy rule: one user can never touch another user's task.
 */
class TaskControllerIntegrationTest extends AbstractIntegrationTest {

    /** Matches the {@code User.timezone} default; "today" is computed in this zone. */
    private static final ZoneId USER_ZONE = ZoneId.of("Europe/Kyiv");

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
    void createsAndListsTodayInUserTimezone() {
        String goalId = createGoal(ownerToken);
        String today = LocalDate.now(USER_ZONE).toString();
        String tomorrow = LocalDate.now(USER_ZONE).plusDays(1).toString();

        String todayTaskId = createTask(goalId, "Do today", today);
        createTask(goalId, "Do tomorrow", tomorrow);

        given().auth().oauth2(ownerToken)
                .when().get("/api/tasks/today")
                .then().statusCode(200)
                .body("id", contains(todayTaskId))
                .body("title", contains("Do today"));
    }

    @Test
    void statusDoneSetsCompletedAt_andKeepsItWhenReopened() {
        String goalId = createGoal(ownerToken);
        String taskId = createTask(goalId, "Finish", null);

        given().auth().oauth2(ownerToken).contentType(ContentType.JSON)
                .body("{ \"status\": \"DONE\" }")
                .when().patch("/api/tasks/" + taskId + "/status")
                .then().statusCode(200)
                .body("status", equalTo("DONE"))
                .body("completedAt", notNullValue());

        // completedAt is the first-completion marker: re-opening the task keeps it set, so a
        // later re-completion cannot count twice.
        given().auth().oauth2(ownerToken).contentType(ContentType.JSON)
                .body("{ \"status\": \"TODO\" }")
                .when().patch("/api/tasks/" + taskId + "/status")
                .then().statusCode(200)
                .body("status", equalTo("TODO"))
                .body("completedAt", notNullValue());
    }

    @Test
    void reorderAppliesNewOrder() {
        String goalId = createGoal(ownerToken);
        String today = LocalDate.now(USER_ZONE).toString();
        String first = createTask(goalId, "First", today);
        String second = createTask(goalId, "Second", today);

        given().auth().oauth2(ownerToken).contentType(ContentType.JSON)
                .body("{ \"ids\": [\"" + second + "\", \"" + first + "\"] }")
                .when().post("/api/tasks/reorder")
                .then().statusCode(204);

        given().auth().oauth2(ownerToken)
                .when().get("/api/tasks/today")
                .then().statusCode(200)
                .body("id", contains(second, first));
    }

    @Test
    void tasksAreScopedToOwner() {
        String goalId = createGoal(ownerToken);
        String taskId = createTask(goalId, "Mine", null);

        given().auth().oauth2(otherToken).contentType(ContentType.JSON)
                .body("{ \"status\": \"DONE\" }")
                .when().patch("/api/tasks/" + taskId + "/status")
                .then().statusCode(404)
                .body("code", equalTo("NOT_FOUND"));

        given().auth().oauth2(otherToken)
                .when().delete("/api/tasks/" + taskId)
                .then().statusCode(404);
    }

    @Test
    void cannotCreateTaskUnderAnotherUsersGoal() {
        String goalId = createGoal(ownerToken);

        given().auth().oauth2(otherToken).contentType(ContentType.JSON)
                .body("{ \"goalId\": \"" + goalId + "\", \"title\": \"Sneaky\" }")
                .when().post("/api/tasks")
                .then().statusCode(404)
                .body("code", equalTo("NOT_FOUND"));
    }

    @Test
    void rejectsMissingTitle() {
        String goalId = createGoal(ownerToken);
        given().auth().oauth2(ownerToken).contentType(ContentType.JSON)
                .body("{ \"goalId\": \"" + goalId + "\" }")
                .when().post("/api/tasks")
                .then().statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void requiresAuthentication() {
        given().when().get("/api/tasks/today").then().statusCode(401);
    }

    private String createGoal(String token) {
        return given().auth().oauth2(token).contentType(ContentType.JSON)
                .body("{ \"title\": \"Goal\" }")
                .when().post("/api/goals")
                .then().statusCode(200)
                .extract().path("id");
    }

    private String createTask(String goalId, String title, String scheduledFor) {
        String schedule = scheduledFor == null ? "" : ", \"scheduledFor\": \"" + scheduledFor + "\"";
        return given().auth().oauth2(ownerToken).contentType(ContentType.JSON)
                .body("{ \"goalId\": \"" + goalId + "\", \"title\": \"" + title + "\"" + schedule + " }")
                .when().post("/api/tasks")
                .then().statusCode(200)
                .body("id", notNullValue())
                .extract().path("id");
    }

    private String tokenForNewUser() {
        User user = new User();
        user.setEmail("task-test-" + UUID.randomUUID() + "@questline.test");
        user.setName("Task Tester");
        return tokenService.issue(userRepository.save(user));
    }
}
