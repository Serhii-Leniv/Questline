package com.questline.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

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

/** The week view returns tasks scheduled in the current week and excludes ones outside it. */
class WeekViewIntegrationTest extends AbstractIntegrationTest {

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
        user.setEmail("week-test-" + UUID.randomUUID() + "@questline.test");
        user.setName("Weekly");
        token = tokenService.issue(userRepository.save(user));
    }

    @Test
    void returnsThisWeeksTasksOnly() {
        String goalId = createGoal();
        String inWeek = createTask(goalId, "In week", LocalDate.now(USER_ZONE).toString());
        String outOfWeek = createTask(goalId, "Next week", LocalDate.now(USER_ZONE).plusDays(10).toString());

        given().auth().oauth2(token)
                .when().get("/api/tasks/week")
                .then().statusCode(200)
                .body("id", hasItem(inWeek))
                .body("id", not(hasItem(outOfWeek)));
    }

    @Test
    void requiresAuthentication() {
        given().when().get("/api/tasks/week").then().statusCode(401);
    }

    private String createGoal() {
        return given().auth().oauth2(token).contentType(ContentType.JSON)
                .body("{ \"title\": \"Goal\" }")
                .when().post("/api/goals")
                .then().statusCode(200).extract().path("id");
    }

    private String createTask(String goalId, String title, String scheduledFor) {
        return given().auth().oauth2(token).contentType(ContentType.JSON)
                .body("{ \"goalId\": \"" + goalId + "\", \"title\": \"" + title
                        + "\", \"scheduledFor\": \"" + scheduledFor + "\" }")
                .when().post("/api/tasks")
                .then().statusCode(200).extract().path("id");
    }
}
