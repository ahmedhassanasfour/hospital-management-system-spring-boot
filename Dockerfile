# =============================================================================
# STAGE 1 — BUILD
# Uses the full Maven + JDK image to compile and package the application.
# The Maven wrapper (mvnw) is used so no separate Maven installation is needed.
# =============================================================================
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /build

# Copy Maven wrapper and POM first for layer caching.
# Dependencies are downloaded only when pom.xml changes.
COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml ./

# Download dependencies (cached layer — only invalidated when pom.xml changes)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B -q

# Copy source code and build the fat JAR, skipping tests
# (tests require a running DB/Redis which is not available at build time)
COPY src ./src
RUN ./mvnw package -DskipTests -B -q

# =============================================================================
# STAGE 2 — RUNTIME
# Uses the slim JRE image — much smaller than the full JDK.
# Only the application JAR is copied from the build stage.
# =============================================================================
FROM eclipse-temurin:21-jre AS runtime

# Security: run as a non-root user
RUN groupadd --system hospital && useradd --system --gid hospital hospital

WORKDIR /app

# Copy only the fat JAR from the build stage
COPY --from=builder /build/target/hospital-*.jar app.jar

# Create the uploads directory and set ownership
RUN mkdir -p uploads && chown -R hospital:hospital /app

# Switch to non-root user
USER hospital

# Expose the application port (matches server.port=8081)
EXPOSE 8081

# Health check — probes /actuator/health (root endpoint, always public).
# This is the external check used by Docker to determine if the container is
# alive.  Docker Compose uses /actuator/health/readiness for the orchestration
# readiness probe (verifies DB + Redis are reachable too).
# start_period allows the JVM and Spring context to fully initialize before
# the first check runs.
HEALTHCHECK \
  --interval=30s \
  --timeout=10s \
  --start-period=60s \
  --retries=3 \
  CMD wget -qO- http://localhost:8081/actuator/health | grep -q '"status":"UP"' || exit 1

# JVM tuning for containers:
#   -XX:+UseContainerSupport     — respects Docker CPU/memory limits
#   -XX:MaxRAMPercentage=75.0    — use up to 75% of container memory for heap
#   -XX:+ExitOnOutOfMemoryError  — fail-fast on OOM (Docker will restart)
#
# Spring profile:
#   The active profile is controlled by the SPRING_PROFILES_ACTIVE environment
#   variable injected at runtime (e.g., by Docker Compose).  Not baked into
#   the image so the same image can be used across environments.
#   Default (if not set): dev  — see application.properties: spring.profiles.default=dev
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "app.jar"]
