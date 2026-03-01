# Stage 1: Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Cache Gradle wrapper
COPY gradle gradle
COPY gradlew build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# Build the application
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Production
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S docbrain && adduser -S docbrain -G docbrain
RUN mkdir -p /app/uploads && chown -R docbrain:docbrain /app

COPY --from=build --chown=docbrain:docbrain /app/build/libs/*.jar app.jar

USER docbrain

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/api/v1/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
