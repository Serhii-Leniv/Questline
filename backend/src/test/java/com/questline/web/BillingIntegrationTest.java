package com.questline.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

import com.questline.AbstractIntegrationTest;
import com.questline.domain.User;
import com.questline.repository.UserRepository;
import com.questline.security.TokenService;
import io.restassured.RestAssured;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

/** Plan view + (mock) upgrade/downgrade, and that the AI limit grows with the plan. */
class BillingIntegrationTest extends AbstractIntegrationTest {

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
        user.setEmail("billing-test-" + UUID.randomUUID() + "@questline.test");
        user.setName("Billing");
        token = tokenService.issue(userRepository.save(user));
    }

    @Test
    void newUsersAreFree_andCanUpgradeAndDowngrade() {
        given().auth().oauth2(token).when().get("/api/me/plan")
                .then().statusCode(200).body("plan", equalTo("FREE"));

        int freeLimit = given().auth().oauth2(token).when().get("/api/me/plan")
                .then().statusCode(200).extract().path("aiDailyLimit");

        given().auth().oauth2(token).when().post("/api/me/plan/upgrade")
                .then().statusCode(200)
                .body("plan", equalTo("PRO"))
                .body("aiDailyLimit", greaterThan(freeLimit));

        // The profile reflects the plan too.
        given().auth().oauth2(token).when().get("/api/me")
                .then().statusCode(200).body("plan", equalTo("PRO"));

        given().auth().oauth2(token).when().post("/api/me/plan/downgrade")
                .then().statusCode(200).body("plan", equalTo("FREE"));
    }

    @Test
    void planRequiresAuthentication() {
        given().when().get("/api/me/plan").then().statusCode(401);
    }
}
