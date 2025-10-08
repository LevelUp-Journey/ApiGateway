# Multi-stage build for optimized production image
FROM eclipse-temurin:24-jdk AS build

# Set working directory
WORKDIR /app

# Copy Maven configuration files first (better caching)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Make Maven wrapper executable
RUN chmod +x mvnw

# Download dependencies (cached layer if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster builds)
RUN ./mvnw clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:24-jre

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Copy the JAR from build stage
COPY --from=build /app/target/microservice-api-gateway-*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appuser /app
USER appuser

# Expose port (Render uses $PORT env variable)
EXPOSE ${PORT:-8080}

# Health check removed - Render handles this externally

# Run the application with optimized JVM settings for containers
# Use shell form to allow environment variable substitution
CMD java -server \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom \
    -Dserver.port=${PORT:-8080} \
    -jar app.jar