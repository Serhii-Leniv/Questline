package com.questline.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;

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

/** "Plan my day" fills today up to the user's daily capacity, highest-priority tasks first. */
class PlanDayIntegrationTest extends AbstractIntegrationTest {

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
        user.setEmail("plan-test-" + UUID.randomUUID() + "@questline.test");
        user.setName("Planner");
        user.setDailyCapacityMinutes(120); // explicit for the test
        token = tokenService.issue(userRepository.save(user));
    }

    @Test
    void schedulesTasksUpToCapacity() {
        String goalId = createGoal();
        createTask(goalId, "A", 60);
        createTask(goalId, "B", 60);
        createTask(goalId, "C", 60); // 180 total > 120 capacity → only two fit

        given().auth().oauth2(token)
                .when().post("/api/tasks/plan-day")
                .then().statusCode(200)
                .body("$", hasSize(2));

        given().auth().oauth2(token)
                .when().get("/api/tasks/today")
                .then().statusCode(200)
                .body("$", hasSize(2));
    }

    @Test
    void requiresAuthentication() {
        given().when().post("/api/tasks/plan-day").then().statusCode(401);
    }

    private String createGoal() {
        return given().auth().oauth2(token).contentType(ContentType.JSON)
                .body("{ \"title\": \"Goal\" }")
                .when().post("/api/goals")
                .then().statusCode(200).extract().path("id");
    }

    private void createTask(String goalId, String title, int estimateMinutes) {
        given().auth().oauth2(token).contentType(ContentType.JSON)
                .body("{ \"goalId\": \"" + goalId + "\", \"title\": \"" + title
                        + "\", \"estimateMinutes\": " + estimateMinutes + " }")
                .when().post("/api/tasks")
                .then().statusCode(200);
    }
}
