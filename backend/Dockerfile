# --- Build stage ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Cache dependencies first.
COPY gradle ./gradle
COPY gradlew settings.gradle.kts build.gradle.kts ./
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# Build the boot jar.
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# --- Runtime stage ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/questline.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
