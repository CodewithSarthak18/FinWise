# ==============================================================================
# Multi-Stage Dockerfile for FinWise Backend (Java 21 + Spring Boot 3)
# ==============================================================================

# Stage 1: Build Application with Maven
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy Maven POM and pre-fetch dependencies for efficient Docker layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and build production executable JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal Distroless/Alpine Production Runtime
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Create non-root system user and group for security
RUN addgroup -S finwise && adduser -S finwise -G finwise && \
    mkdir -p /var/log/finwise && \
    chown -R finwise:finwise /app /var/log/finwise

USER finwise:finwise

# Copy compiled JAR artifact from builder
COPY --from=builder --chown=finwise:finwise /build/target/finwise-backend-*.jar app.jar

# Expose backend service port
EXPOSE 8080

# Configure production container healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/readiness || exit 1

# Optimized JVM memory and GC flags for container environments
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:InitialRAMPercentage=40.0", \
  "-XX:+UseG1GC", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=prod", \
  "-jar", "app.jar"]
