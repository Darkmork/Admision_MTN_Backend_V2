# Dockerfile for Notification Service Microservice
FROM amazoncorretto:17-alpine AS builder

WORKDIR /build

# Install dependencies
RUN apk add --no-cache curl bash maven

# Copy Maven files
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src src

# Build application
RUN mvn clean package -DskipTests -B

# Verify JAR was created
RUN ls -la target/

FROM amazoncorretto:17-alpine AS runtime

WORKDIR /app

# Install runtime dependencies
RUN apk add --no-cache curl bash tzdata && \
    cp /usr/share/zoneinfo/UTC /etc/localtime && \
    echo "UTC" > /etc/timezone

# Create non-root user
RUN addgroup -g 1000 appgroup && \
    adduser -u 1000 -G appgroup -h /app -D appuser

# Copy JAR file
COPY --from=builder /build/target/notification-service-*.jar /app/notification-service.jar

# Change ownership
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Djava.security.egd=file:/dev/./urandom" \
    SPRING_PROFILES_ACTIVE=microservices \
    TZ=UTC

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8085/actuator/health || exit 1

# Expose port
EXPOSE 8085

# Entry point
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar notification-service.jar"]

# Labels
LABEL maintainer="MTN Development Team <dev@mtn.cl>" \
      version="1.0.0" \
      description="MTN Notification Service - Microservicio de notificaciones" \
      org.opencontainers.image.source="https://github.com/mtn/admision-system" \
      org.opencontainers.image.title="MTN Notification Service" \
      org.opencontainers.image.description="Microservicio de notificaciones para sistema de admisión MTN" \
      org.opencontainers.image.version="1.0.0"