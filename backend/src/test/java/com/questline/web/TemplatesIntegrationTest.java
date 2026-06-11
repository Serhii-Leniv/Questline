package com.questline.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import com.questline.AbstractIntegrationTest;
import com.questline.domain.Goal;
import com.questline.domain.Milestone;
import com.questline.domain.Task;
import com.questline.domain.User;
import com.questline.repository.GoalRepository;
import com.questline.repository.UserRepository;
import com.questline.security.TokenService;
import io.restassured.RestAssured;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

/** Publish a goal as a public template, browse it, and import it into another user's account. */
class TemplatesIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    UserRepository userRepository;

    @Autowired
    GoalRepository goalRepository;

    @Autowired
    TokenService tokenService;

    private User author;
    private String authorToken;
    private String importerToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        author = newUser();
        authorToken = tokenService.issue(author);
        importerToken = tokenService.issue(newUser());
    }

    @Test
    void publishBrowseAndImport() {
        Goal goal = seedGoalWithTree(author);

        String templateId = given().auth().oauth2(authorToken)
                .when().post("/api/goals/" + goal.getId() + "/publish")
                .then().statusCode(200)
                .body("title", equalTo("Learn Go"))
                .body("taskCount", equalTo(2))
                .extract().path("id");

        given().auth().oauth2(importerToken)
                .when().get("/api/templates")
                .then().statusCode(200)
                .body("id", hasItem(templateId));

        given().auth().oauth2(importerToken)
                .when().get("/api/templates/" + templateId)
                .then().statusCode(200)
                .body("plan.milestones[0].title", equalTo("Basics"));

        // Importer gets a fresh goal recreated from the template.
        String newGoalId = given().auth().oauth2(importerToken)
                .when().post("/api/templates/" + templateId + "/import")
                .then().statusCode(200)
                .body("title", equalTo("Learn Go"))
                .body("milestones", hasSize(1))
                .body("milestones[0].tasks", hasSize(2))
                .extract().path("id");

        // It belongs to the importer.
        given().auth().oauth2(importerToken)
                .when().get("/api/goals")
                .then().statusCode(200)
                .body("id", hasItem(newGoalId));
    }

    @Test
    void browsingTemplatesRequiresAuth() {
        given().when().get("/api/templates").then().statusCode(401);
    }

    private Goal seedGoalWithTree(User user) {
        Goal goal = new Goal();
        goal.setUser(user);
        goal.setTitle("Learn Go");
        Milestone milestone = new Milestone();
        milestone.setGoal(goal);
        milestone.setTitle("Basics");
        milestone.setOrderIndex(0);
        milestone.getTasks().add(task(user, goal, milestone, "Syntax", 30, 0));
        milestone.getTasks().add(task(user, goal, milestone, "Goroutines", 45, 1));
        goal.getMilestones().add(milestone);
        return goalRepository.save(goal);
    }

    private Task task(User user, Goal goal, Milestone milestone, String title, int estimate, int order) {
        Task task = new Task();
        task.setUser(user);
        task.setGoal(goal);
        task.setMilestone(milestone);
        task.setTitle(title);
        task.setEstimateMinutes(estimate);
        task.setOrderIndex(order);
        return task;
    }

    private User newUser() {
        User user = new User();
        user.setEmail("tpl-test-" + UUID.randomUUID() + "@questline.test");
        user.setName("Templater");
        return userRepository.save(user);
    }
}
