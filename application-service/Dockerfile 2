# Multi-stage build para application-service
FROM amazoncorretto:17-alpine AS builder

# Instalar herramientas necesarias
RUN apk add --no-cache curl tar maven

# Crear directorio de trabajo
WORKDIR /workspace/app

# Copiar archivos de configuración Maven
COPY pom.xml .

# Descargar dependencias (optimización de caché Docker)
RUN mvn dependency:go-offline -B

# Copiar código fuente
COPY src src

# Construir aplicación
RUN mvn clean package -DskipTests
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

# Imagen de producción
FROM amazoncorretto:17-alpine

# Configurar zona horaria
RUN apk add --no-cache tzdata
ENV TZ=America/Santiago

# Crear usuario no-root para seguridad
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Configurar directorio de trabajo
WORKDIR /app

# Copiar aplicación desde stage builder
ARG DEPENDENCY=/workspace/app/target/dependency
COPY --from=builder ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=builder ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=builder ${DEPENDENCY}/BOOT-INF/classes /app

# Cambiar propietario de archivos
RUN chown -R appuser:appgroup /app

# Cambiar a usuario no-root
USER appuser

# Exponer puerto
EXPOSE 8083

# Configurar JVM optimizada para contenedor
ENV JAVA_OPTS="-server \
    -Xms256m \
    -Xmx512m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -Djava.awt.headless=true \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=America/Santiago \
    -Djava.security.egd=file:/dev/./urandom"

# Variables de entorno para observabilidad
ENV OTEL_SERVICE_NAME=application-service
ENV OTEL_RESOURCE_ATTRIBUTES="service.name=application-service,deployment.environment=docker"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8083/actuator/health/readiness || exit 1

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -cp app:app/lib/* com.desafios.mtn.applicationservice.ApplicationServiceApplication"]

# Metadata
LABEL maintainer="Equipo MTN <dev@mtn.cl>"
LABEL description="Microservicio de Dominio de Admisiones - Sistema MTN"
LABEL version="1.0.0"
LABEL org.opencontainers.image.source="https://github.com/mtn-colegio/admission-system"
LABEL org.opencontainers.image.title="Application Service"
LABEL org.opencontainers.image.description="Microservicio de gestión de postulaciones y estados de admisión"