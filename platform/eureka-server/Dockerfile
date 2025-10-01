# Eureka Server Dockerfile
FROM openjdk:17-jdk-slim

LABEL maintainer="MTN Admission System <admin@mtn.cl>"
LABEL description="Eureka Service Discovery Server for MTN Admission Microservices"

# Install necessary packages
RUN apt-get update && apt-get install -y \
    curl \
    netcat-traditional \
    && rm -rf /var/lib/apt/lists/*

# Create app directory
WORKDIR /app

# Copy Maven wrapper and pom.xml for dependency caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (for Docker layer caching)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN ./mvnw clean package -DskipTests

# Expose port
EXPOSE 8761

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8761/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "target/eureka-server.jar"]