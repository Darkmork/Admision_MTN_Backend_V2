# API Gateway Dockerfile
FROM openjdk:17-jdk-slim

LABEL maintainer="MTN Admission System <admin@mtn.cl>"
LABEL description="API Gateway for MTN Admission Microservices"

# Install dependencies
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
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN ./mvnw clean package -DskipTests

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "target/api-gateway.jar"]