import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.questline"
version = "0.0.1-SNAPSHOT"
description = "Questline — AI-powered goal decomposition + streak tracker"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    // Spring AI 2.0 is still a release candidate; RC artifacts are published to Maven Central,
    // but keep the milestone repo available in case a dependency resolves only there.
    maven { url = uri("https://repo.spring.io/milestone") }
}

// Pinned, verified versions (see README / SPEC §6). Spring AI is pinned to the exact RC so
// moving to GA is a one-line bump.
val springAiVersion = "2.0.0-RC2"
val jobrunrVersion = "8.6.1"
val testcontainersVersion = "2.0.5"
val restAssuredVersion = "6.0.0"

extra["spring-ai.version"] = springAiVersion

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:$springAiVersion")
        // Boot 4's BOM does not manage these test libraries — pin them explicitly.
        mavenBom("org.testcontainers:testcontainers-bom:$testcontainersVersion")
    }
}

dependencies {
    // Web / REST
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Persistence
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // Boot 4 modularized autoconfigurations: the Flyway autoconfig now ships in its own
    // starter (plain flyway-core does NOT trigger migrations on Boot 4).
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // Security: Google OAuth2 login + JWT (resource server validates our self-issued tokens)
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // AI — Spring AI Google GenAI (Google AI Studio / Gemini Developer API, NOT Vertex AI)
    implementation("org.springframework.ai:spring-ai-starter-model-google-genai")

    // Async background jobs (DB-backed)
    implementation("org.jobrunr:jobrunr-spring-boot-4-starter:$jobrunrVersion")

    // Observability
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Lombok (entities)
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Test — JUnit 5 only (no JUnit 4 on Boot 4)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    // Testcontainers 2.0 renamed modules to the testcontainers-* prefix.
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("io.rest-assured:rest-assured:$restAssuredVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<BootJar> {
    archiveFileName.set("questline.jar")
}
