#-----------------------------------------------------------------------------
# Stage 1: Build the application JAR
#-----------------------------------------------------------------------------
FROM gradle:8.13-jdk21 AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy build files first to leverage Docker cache for dependencies
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Download dependencies. This layer is cached if build files don't change.
# Run a task that resolves dependencies. `dependencies` task or `build` with `-x` works.
# Use `--build-cache` for Gradle's build cache. `|| true` prevents failure if task doesn't exist.
RUN ./gradlew dependencies --build-cache || true

# Copy the source code into the container
COPY src ./src

# Build the application JAR. Disable daemons for CI environments.
# Use bootJar for Spring Boot applications. Skip tests.
RUN ./gradlew bootJar --no-daemon -x test

#-----------------------------------------------------------------------------
# Stage 2: Create the final runtime image
#-----------------------------------------------------------------------------
FROM eclipse-temurin:21-alpine AS runtime
# Or use: FROM eclipse-temurin:17-jre for a non-alpine base

# Set the working directory
WORKDIR /app

# Create a non-root user and group for security
# Alpine version:
RUN addgroup --system spring && adduser --system --ingroup spring spring
# Debian/Ubuntu version (if using eclipse-temurin:17-jre):
# RUN addgroup --system spring && adduser --system --disabled-password --ingroup spring spring

# Switch to the non-root user
USER spring

# Copy the executable JAR from the builder stage
# Make sure the JAR name pattern matches your build output (usually in build/libs/)
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose the port the application runs on (default for Spring Boot is 8080)
EXPOSE 8080

# Command to run the application
# ENTRYPOINT ["java", "-jar", "app.jar"]

# Optional: Add JVM arguments if needed
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom","-Xmx512m", "-jar","/app/app.jar"]
