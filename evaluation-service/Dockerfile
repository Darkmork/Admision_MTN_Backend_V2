# Multi-stage Docker build for evaluation-service
FROM amazoncorretto:17-alpine AS build

# Install build dependencies
RUN apk add --no-cache curl maven

# Set working directory
WORKDIR /app

# Copy Maven files for dependency resolution
COPY pom.xml ./

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM amazoncorretto:17-alpine AS runtime

# Install runtime dependencies and create user
RUN apk add --no-cache \
    curl \
    tzdata \
    && addgroup -g 1001 -S appgroup \
    && adduser -u 1001 -S appuser -G appgroup

# Set timezone
ENV TZ=America/Santiago
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Create application directory
WORKDIR /app

# Copy built artifact from build stage
COPY --from=build /app/target/evaluation-service-*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8084

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8084/actuator/health || exit 1

# Environment variables
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENV SPRING_PROFILES_ACTIVE=prod
ENV TZ=America/Santiago

# JVM tuning for containers
ENV JVM_OPTS=" \
    -XX:+UnlockExperimentalVMOptions \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+ParallelRefProcEnabled \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -XX:+UseCompressedOops \
    -XX:+UseCompressedClassPointers \
    -Djava.security.egd=file:/dev/./urandom \
    -Duser.timezone=America/Santiago \
    -Dfile.encoding=UTF-8 \
    -Dsun.jnu.encoding=UTF-8 \
    -Djava.awt.headless=true \
    -Dspring.backgroundpreinitializer.ignore=true"

# Application configuration
ENV SERVER_PORT=8084
ENV MANAGEMENT_SERVER_PORT=8085

# Database configuration (will be overridden by environment)
ENV DATABASE_URL=jdbc:postgresql://evaluations-db:5432/evaluations_db
ENV DATABASE_USERNAME=evaluations_user
ENV DATABASE_PASSWORD=evaluations_pass

# RabbitMQ configuration
ENV RABBITMQ_HOST=rabbitmq
ENV RABBITMQ_PORT=5672
ENV RABBITMQ_USERNAME=guest
ENV RABBITMQ_PASSWORD=guest

# Eureka configuration
ENV EUREKA_DEFAULT_ZONE=http://eureka-server:8761/eureka/

# Security configuration
ENV KEYCLOAK_ISSUER_URI=http://keycloak:8080/realms/mtn-admission

# Tracing configuration
ENV ZIPKIN_ENDPOINT=http://zipkin:9411/api/v2/spans

# Application specific configuration
ENV AUTO_ASSIGNMENT_ENABLED=true
ENV EVENTS_ENABLED=true
ENV NOTIFICATIONS_ENABLED=true
ENV SAGA_FALLBACK_ENABLED=true

# Logging configuration
ENV LOG_LEVEL=INFO
ENV ROOT_LOG_LEVEL=WARN

# Run the application
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS $JVM_OPTS -jar app.jar"]

# Labels for metadata
LABEL maintainer="MTN Development Team"
LABEL application="evaluation-service"
LABEL version="1.0.0"
LABEL description="Microservicio de evaluaciones y entrevistas para Sistema de Admisión MTN"
LABEL org.opencontainers.image.title="MTN Evaluation Service"
LABEL org.opencontainers.image.description="Microservicio de evaluaciones y entrevistas"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.created="${BUILD_DATE:-unknown}"
LABEL org.opencontainers.image.revision="${GIT_COMMIT:-unknown}"
LABEL org.opencontainers.image.source="https://github.com/mtn-admission/evaluation-service"