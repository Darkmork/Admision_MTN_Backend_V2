# user-service/Dockerfile - Simplified for testing

FROM amazoncorretto:17-alpine

WORKDIR /app

# Install dependencies
RUN apk add --no-cache curl bash maven

# Copy Maven files and source
COPY pom.xml .
COPY src ./src

# Build application
RUN mvn clean package -DskipTests -B

# Environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m" \
    SPRING_PROFILES_ACTIVE=microservices

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8082/api/users/health || exit 1

# Expose port
EXPOSE 8082

# Entry point
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar target/user-service-*.jar"]