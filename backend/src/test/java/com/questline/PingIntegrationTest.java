package com.questline;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

class PingIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void ping_isPublic_andReturnsOk() {
        given()
                .when().get("/api/ping")
                .then().statusCode(200).body("status", equalTo("ok"));
    }

    @Test
    void me_requiresAuthentication() {
        given()
                .when().get("/api/me")
                .then().statusCode(401);
    }
}
