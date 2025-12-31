# Multi-stage Dockerfile for Spring Boot application
# Stage 1: Build stage (not used in Docker build, JAR is built by Gradle before Docker)
# Stage 2: Runtime stage

# Use Eclipse Temurin JRE 21 as base image (multi-platform support)
FROM eclipse-temurin:21-jre-jammy

# Set working directory
WORKDIR /app

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Argument for JAR file location (provided by Docker plugin)
ARG JAR_FILE

# Copy the JAR file
COPY ${JAR_FILE} app.jar

# Change ownership to non-root user
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Health check endpoint (Spring Boot Actuator)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
