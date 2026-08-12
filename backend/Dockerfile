# --- Build ------------------------------------------------------------------
# The jar is built inside the image so a clone plus Docker is all anyone needs;
# no local JDK or Maven required.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Dependencies resolve in their own layer, so editing source does not re-download
# the world on every rebuild.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Tests run in `mvn verify` on the developer machine and in CI. Repeating them here
# would need a Docker socket inside the build for Testcontainers.
RUN mvn -B -q clean package -DskipTests

# --- Runtime ------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# curl is only here for the compose healthcheck.
RUN apk add --no-cache curl tzdata \
    && addgroup -S libris && adduser -S libris -G libris

COPY --from=build --chown=libris:libris /build/target/*.jar app.jar

USER libris
EXPOSE 8080

# Let the JVM size its heap from the container limits rather than the host's memory.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=10 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
