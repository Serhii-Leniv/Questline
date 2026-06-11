package com.questline.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

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

/** Topic tagging on tasks, auto-creation (find-or-create by slug), and per-topic completion stats. */
class TopicsIntegrationTest extends AbstractIntegrationTest {

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
        user.setEmail("topic-test-" + UUID.randomUUID() + "@questline.test");
        user.setName("Tagger");
        token = tokenService.issue(userRepository.save(user));
    }

    @Test
    void taggingTasks_autoCreatesTopics_andTracksCompletion() {
        String goalId = createGoal();

        String taskId = given().auth().oauth2(token).contentType(ContentType.JSON)
                .body("{ \"goalId\": \"" + goalId + "\", \"title\": \"Study\","
                        + " \"topics\": [\"System Design\", \"Concurrency\"] }")
                .when().post("/api/tasks")
                .then().statusCode(200)
                .body("topics", hasItems("System Design", "Concurrency"))
                .extract().path("id");

        // A second task reuses the same topic regardless of case (slug match).
        given().auth().oauth2(token).contentType(ContentType.JSON)
                .body("{ \"goalId\": \"" + goalId + "\", \"title\": \"More study\","
                        + " \"topics\": [\"system design\"] }")
                .when().post("/api/tasks")
                .then().statusCode(200);

        given().auth().oauth2(token)
                .when().get("/api/stats/topics")
                .then().statusCode(200)
                .body("find { it.name == 'System Design' }.total", equalTo(2))
                .body("find { it.name == 'System Design' }.done", equalTo(0));

        given().auth().oauth2(token).contentType(ContentType.JSON)
                .body("{ \"status\": \"DONE\" }")
                .when().patch("/api/tasks/" + taskId + "/status")
                .then().statusCode(200);

        given().auth().oauth2(token)
                .when().get("/api/stats/topics")
                .then().statusCode(200)
                .body("find { it.name == 'System Design' }.done", equalTo(1));
    }

    private String createGoal() {
        return given().auth().oauth2(token).contentType(ContentType.JSON)
                .body("{ \"title\": \"Goal\" }")
                .when().post("/api/goals")
                .then().statusCode(200).extract().path("id");
    }
}
