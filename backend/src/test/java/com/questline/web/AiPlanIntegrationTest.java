package com.questline.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import com.questline.AbstractIntegrationTest;
import com.questline.ai.GeneratedPlan;
import com.questline.ai.PlannedMilestone;
import com.questline.ai.PlannedTask;
import com.questline.domain.AiJob;
import com.questline.domain.AiJobStatus;
import com.questline.domain.AiJobType;
import com.questline.domain.Goal;
import com.questline.domain.Milestone;
import com.questline.domain.MilestoneStatus;
import com.questline.domain.User;
import com.questline.repository.AiJobRepository;
import com.questline.repository.GoalRepository;
import com.questline.repository.UserRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.ObjectMapper;

/**
 * AI plan flow over HTTP. The JobRunr worker is disabled in tests (see AbstractIntegrationTest),
 * so generation jobs stay PENDING; the accept path is exercised by seeding a SUCCEEDED job
 * directly, which verifies the tree-persistence + cascade end to end.
 */
class AiPlanIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    UserRepository userRepository;

    @Autowired
    GoalRepository goalRepository;

    @Autowired
    AiJobRepository aiJobRepository;

    @Autowired
    com.questline.security.TokenService tokenService;

    @Autowired
    ObjectMapper objectMapper;

    private User owner;
    private String ownerToken;
    private String otherToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        owner = newUser();
        ownerToken = tokenService.issue(owner);
        otherToken = tokenService.issue(newUser());
    }

    @Test
    void generateCreatesDraftGoalAndPendingJob() {
        var ids = given().auth().oauth2(ownerToken).contentType(ContentType.JSON)
                .body("""
                        { "context": "mid-level dev", "target": "Become a senior engineer",
                          "weeklyCapacityMinutes": 300 }
                        """)
                .when().post("/api/ai/plans")
                .then().statusCode(200)
                .body("jobId", notNullValue())
                .body("goalId", notNullValue())
                .extract().jsonPath();

        String jobId = ids.getString("jobId");
        String goalId = ids.getString("goalId");

        given().auth().oauth2(ownerToken)
                .when().get("/api/ai/jobs/" + jobId)
                .then().statusCode(200)
                .body("status", equalTo("PENDING"))
                .body("type", equalTo("GENERATE_PLAN"));

        // The draft goal exists immediately, with no tree until the plan is accepted.
        given().auth().oauth2(ownerToken)
                .when().get("/api/goals/" + goalId)
                .then().statusCode(200)
                .body("milestones", hasSize(0));
    }

    @Test
    void parseCreatesImportedDraftGoalAndPendingJob() {
        var ids = given().auth().oauth2(ownerToken).contentType(ContentType.JSON)
                .body("{ \"text\": \"Phase 1: Basics\\n- learn X\\n- practice Y\" }")
                .when().post("/api/ai/roadmaps/parse")
                .then().statusCode(200)
                .body("jobId", notNullValue())
                .body("goalId", notNullValue())
                .extract().jsonPath();

        given().auth().oauth2(ownerToken)
                .when().get("/api/ai/jobs/" + ids.getString("jobId"))
                .then().statusCode(200)
                .body("status", equalTo("PENDING"))
                .body("type", equalTo("PARSE_ROADMAP"));
    }

    @Test
    void acceptPersistsGeneratedTree() {
        Goal goal = seedDraftGoal(owner);
        seedSucceededJob(owner, goal, samplePlan());

        given().auth().oauth2(ownerToken)
                .when().post("/api/ai/plans/" + goal.getId() + "/accept")
                .then().statusCode(200)
                .body("milestones", hasSize(1))
                .body("milestones[0].title", equalTo("Foundations"))
                .body("milestones[0].tasks", hasSize(2))
                .body("milestones[0].tasks[0].title", equalTo("Learn concurrency"))
                .body("milestones[0].tasks[0].estimateMinutes", equalTo(45));
    }

    @Test
    void acceptTwiceIsRejected() {
        Goal goal = seedDraftGoal(owner);
        seedSucceededJob(owner, goal, samplePlan());

        given().auth().oauth2(ownerToken)
                .when().post("/api/ai/plans/" + goal.getId() + "/accept")
                .then().statusCode(200);

        given().auth().oauth2(ownerToken)
                .when().post("/api/ai/plans/" + goal.getId() + "/accept")
                .then().statusCode(409)
                .body("code", equalTo("PLAN_ALREADY_ACCEPTED"));
    }

    @Test
    void refineRecordsMessageAndStartsNewJob() {
        Goal goal = seedDraftGoal(owner);
        seedSucceededJob(owner, goal, samplePlan());

        String jobId = given().auth().oauth2(ownerToken).contentType(ContentType.JSON)
                .body("{ \"message\": \"Make it three months instead of six\" }")
                .when().post("/api/ai/plans/" + goal.getId() + "/refine")
                .then().statusCode(200)
                .body("goalId", equalTo(goal.getId().toString()))
                .extract().path("jobId");

        given().auth().oauth2(ownerToken)
                .when().get("/api/ai/jobs/" + jobId)
                .then().statusCode(200)
                .body("status", equalTo("PENDING"));
    }

    @Test
    void refineIsScopedToOwner() {
        Goal goal = seedDraftGoal(owner);

        given().auth().oauth2(otherToken).contentType(ContentType.JSON)
                .body("{ \"message\": \"change it\" }")
                .when().post("/api/ai/plans/" + goal.getId() + "/refine")
                .then().statusCode(404);
    }

    @Test
    void acceptWithoutAPlanReturnsConflict() {
        Goal goal = seedDraftGoal(owner);

        given().auth().oauth2(ownerToken)
                .when().post("/api/ai/plans/" + goal.getId() + "/accept")
                .then().statusCode(409)
                .body("code", equalTo("NO_PLAN"));
    }

    @Test
    void planEndpointsAreScopedToOwner() {
        Goal goal = seedDraftGoal(owner);
        seedSucceededJob(owner, goal, samplePlan());

        given().auth().oauth2(otherToken)
                .when().post("/api/ai/plans/" + goal.getId() + "/accept")
                .then().statusCode(404);
    }

    @Test
    void replanStartsAJob() {
        Goal goal = seedDraftGoal(owner);

        given().auth().oauth2(ownerToken)
                .when().post("/api/ai/goals/" + goal.getId() + "/replan")
                .then().statusCode(200)
                .body("jobId", notNullValue())
                .body("goalId", equalTo(goal.getId().toString()));
    }

    @Test
    void acceptReplanKeepsDoneMilestonesAndReplacesTheRest() {
        Goal goal = seedGoalWithMilestones(owner);
        seedSucceededJob(owner, goal, samplePlan()); // new plan has milestone "Foundations"

        given().auth().oauth2(ownerToken)
                .when().post("/api/ai/goals/" + goal.getId() + "/replan/accept")
                .then().statusCode(200)
                .body("milestones.title", hasItem("Done part"))       // completed milestone kept
                .body("milestones.title", hasItem("Foundations"))     // regenerated milestone added
                .body("milestones.title", not(hasItem("Unfinished part"))); // replaced
    }

    @Test
    void requiresAuthentication() {
        given().contentType(ContentType.JSON).body("{ \"context\": \"a\", \"target\": \"b\" }")
                .when().post("/api/ai/plans")
                .then().statusCode(401);
    }

    private Goal seedGoalWithMilestones(User user) {
        Goal goal = new Goal();
        goal.setUser(user);
        goal.setTitle("Goal");
        goal.getMilestones().add(milestone(goal, "Done part", MilestoneStatus.DONE, 0));
        goal.getMilestones().add(milestone(goal, "Unfinished part", MilestoneStatus.IN_PROGRESS, 1));
        return goalRepository.save(goal);
    }

    private Milestone milestone(Goal goal, String title, MilestoneStatus status, int order) {
        Milestone milestone = new Milestone();
        milestone.setGoal(goal);
        milestone.setTitle(title);
        milestone.setStatus(status);
        milestone.setOrderIndex(order);
        return milestone;
    }

    private GeneratedPlan samplePlan() {
        return new GeneratedPlan("A focused 6-month roadmap", List.of(
                new PlannedMilestone("Foundations", "Core skills", List.of(
                        new PlannedTask("Learn concurrency", "threads & locks", 45),
                        new PlannedTask("Practice algorithms", "daily kata", 30)))));
    }

    private Goal seedDraftGoal(User user) {
        Goal goal = new Goal();
        goal.setUser(user);
        goal.setTitle("Become a senior engineer");
        goal.setTarget("Become a senior engineer");
        return goalRepository.save(goal);
    }

    @SuppressWarnings("unchecked")
    private void seedSucceededJob(User user, Goal goal, GeneratedPlan plan) {
        AiJob job = new AiJob();
        job.setUser(user);
        job.setGoal(goal);
        job.setType(AiJobType.GENERATE_PLAN);
        job.setStatus(AiJobStatus.SUCCEEDED);
        job.setOutput(objectMapper.convertValue(plan, Map.class));
        job.setFinishedAt(Instant.now());
        aiJobRepository.save(job);
    }

    private User newUser() {
        User user = new User();
        user.setEmail("ai-test-" + UUID.randomUUID() + "@questline.test");
        user.setName("AI Tester");
        return userRepository.save(user);
    }
}
