# Guía de Despliegue - Sistema MTN Microservicios

## Arquitectura de Despliegue

### Componentes del Sistema
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  API Gateway    │    │ Application     │    │ Evaluation      │
│  (Port 8081)    │    │ Service         │    │ Service         │
│                 │    │ (Port 8080)     │    │ (Port 8082)     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
    │ Notification    │    │ Schema Registry │    │ Event Inbox     │
    │ Service         │    │ (Port 8084)     │    │ Library         │
    │ (Port 8083)     │    │                 │    │                 │
    └─────────────────┘    └─────────────────┘    └─────────────────┘
                                 │
    ┌─────────────────────────────┼─────────────────────────────────┐
    │                    Infrastructure                            │
    │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
    │  │ PostgreSQL  │  │ RabbitMQ    │  │ Keycloak    │          │
    │  │ (5432)      │  │ (5672)      │  │ (8180)      │          │
    │  └─────────────┘  └─────────────┘  └─────────────┘          │
    │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
    │  │ Prometheus  │  │ Grafana     │  │ Jaeger      │          │
    │  │ (9090)      │  │ (3000)      │  │ (16686)     │          │
    │  └─────────────┘  └─────────────┘  └─────────────┘          │
    └─────────────────────────────────────────────────────────────┘
```

## Prerequisitos del Sistema

### Software Requerido
- **Java 17 LTS** (OpenJDK o Oracle JDK)
- **Maven 3.8+** para build y gestión de dependencias
- **PostgreSQL 15+** para persistencia de datos
- **RabbitMQ 3.11+** para messaging asíncrono
- **Docker & Docker Compose** para containerización
- **Git** para control de versiones

### Configuración del Entorno
```bash
# Verificar versiones
java -version
mvn -version
psql --version
docker --version

# Variables de entorno base
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export MAVEN_HOME=/usr/share/maven
export PATH=$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH
```

## Configuración de Base de Datos

### PostgreSQL Setup
```bash
# Crear usuario y bases de datos
sudo -u postgres psql

-- Crear usuario MTN
CREATE USER admin WITH PASSWORD 'admin123';
ALTER USER admin CREATEDB;

-- Crear bases de datos por servicio
CREATE DATABASE "Admisión_MTN_DB" OWNER admin;
CREATE DATABASE "evaluations_db" OWNER admin;
CREATE DATABASE "notifications_db" OWNER admin;
CREATE DATABASE "schema_registry_db" OWNER admin;

-- Otorgar permisos
GRANT ALL PRIVILEGES ON DATABASE "Admisión_MTN_DB" TO admin;
GRANT ALL PRIVILEGES ON DATABASE "evaluations_db" TO admin;
GRANT ALL PRIVILEGES ON DATABASE "notifications_db" TO admin;
GRANT ALL PRIVILEGES ON DATABASE "schema_registry_db" TO admin;
```

### Schema Initialization
```bash
# Ejecutar scripts de inicialización
cd Admision_MTN_backend

# Application Service Schema
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -f db/migration/V1__create_base_schema.sql

# Evaluation Service Schema  
PGPASSWORD=admin123 psql -h localhost -U admin -d "evaluations_db" -f evaluation-service/src/main/resources/db/migration/V1__create_evaluation_schema.sql

# Notification Service Schema
PGPASSWORD=admin123 psql -h localhost -U admin -d "notifications_db" -f notification-service/src/main/resources/db/migration/V1__create_notification_schema.sql

# Schema Registry
PGPASSWORD=admin123 psql -h localhost -U admin -d "schema_registry_db" -f shared-libs/event-schema-registry/src/main/resources/db/migration/V1__create_schema_registry.sql
```

## Configuración de RabbitMQ

### Instalación y Configuración
```bash
# Instalar RabbitMQ
sudo apt-get install rabbitmq-server

# Habilitar management plugin
sudo rabbitmq-plugins enable rabbitmq_management

# Crear usuario MTN
sudo rabbitmqctl add_user mtn_user mtn_password
sudo rabbitmqctl set_user_tags mtn_user administrator
sudo rabbitmqctl set_permissions -p / mtn_user ".*" ".*" ".*"
```

### Topología AMQP
```bash
# Crear exchanges y queues
rabbitmqadmin declare exchange name=mtn.domain.events type=topic durable=true
rabbitmqadmin declare exchange name=mtn.saga.events type=direct durable=true
rabbitmqadmin declare exchange name=mtn.notification.events type=fanout durable=true

# Application Service Queues
rabbitmqadmin declare queue name=application.domain.events.queue durable=true
rabbitmqadmin declare binding source=mtn.domain.events destination=application.domain.events.queue routing_key="application.*"

# Evaluation Service Queues
rabbitmqadmin declare queue name=evaluation.application.events.queue durable=true
rabbitmqadmin declare queue name=evaluation.saga.events.queue durable=true
rabbitmqadmin declare binding source=mtn.domain.events destination=evaluation.application.events.queue routing_key="application.*"
rabbitmqadmin declare binding source=mtn.saga.events destination=evaluation.saga.events.queue routing_key=""

# Notification Service Queues
rabbitmqadmin declare queue name=notification.all.events.queue durable=true
rabbitmqadmin declare binding source=mtn.notification.events destination=notification.all.events.queue routing_key=""
```

## Build y Packaging

### Maven Multi-Module Build
```bash
cd Admision_MTN_backend

# Clean y compile completo
mvn clean compile

# Ejecutar tests
mvn test

# Package todos los módulos
mvn clean package -DskipTests

# Verificar JARs generados
ls -la */target/*.jar
ls -la shared-libs/*/target/*.jar
```

### Docker Images
```dockerfile
# Dockerfile.evaluation-service
FROM openjdk:17-jre-slim

WORKDIR /app

# Copiar JAR
COPY evaluation-service/target/evaluation-service-1.0.0.jar app.jar

# Copiar shared libs si es necesario
COPY shared-libs/event-schema-registry/target/event-schema-registry-1.0.0.jar /libs/
COPY shared-libs/event-inbox/target/event-inbox-1.0.0.jar /libs/
COPY shared-libs/event-envelope/target/event-envelope-1.0.0.jar /libs/

# Configuración JVM
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8082/actuator/health || exit 1

# Exponer puerto
EXPOSE 8082

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### Docker Compose Completo
```yaml
# docker-compose.yml
version: '3.8'

services:
  # Infrastructure Services
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: admin123
      POSTGRES_MULTIPLE_DATABASES: "Admisión_MTN_DB,evaluations_db,notifications_db,schema_registry_db"
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./db/init-scripts:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U admin"]
      interval: 10s
      timeout: 5s
      retries: 5

  rabbitmq:
    image: rabbitmq:3.11-management-alpine
    environment:
      RABBITMQ_DEFAULT_USER: mtn_user
      RABBITMQ_DEFAULT_PASS: mtn_password
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
      - ./rabbitmq/definitions.json:/etc/rabbitmq/definitions.json
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "ping"]
      interval: 30s
      timeout: 10s
      retries: 5

  keycloak:
    image: quay.io/keycloak/keycloak:23.0
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin123
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak
      KC_DB_USERNAME: admin
      KC_DB_PASSWORD: admin123
      KC_HOSTNAME: localhost
    ports:
      - "8180:8080"
    depends_on:
      postgres:
        condition: service_healthy
    command: start-dev

  # Application Services
  application-service:
    build:
      context: .
      dockerfile: Dockerfile.application-service
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/Admisión_MTN_DB
      SPRING_DATASOURCE_USERNAME: admin
      SPRING_DATASOURCE_PASSWORD: admin123
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_USERNAME: mtn_user
      SPRING_RABBITMQ_PASSWORD: mtn_password
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://keycloak:8080/realms/mtn
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      keycloak:
        condition: service_started
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  evaluation-service:
    build:
      context: .
      dockerfile: Dockerfile.evaluation-service
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/evaluations_db
      SPRING_DATASOURCE_USERNAME: admin
      SPRING_DATASOURCE_PASSWORD: admin123
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_USERNAME: mtn_user
      SPRING_RABBITMQ_PASSWORD: mtn_password
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://keycloak:8080/realms/mtn
      SERVICES_APPLICATION_BASE_URL: http://application-service:8080
      SERVICES_SCHEMA_REGISTRY_BASE_URL: http://schema-registry:8084
    ports:
      - "8082:8082"
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      application-service:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8082/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  notification-service:
    build:
      context: .
      dockerfile: Dockerfile.notification-service
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/notifications_db
      SPRING_DATASOURCE_USERNAME: admin
      SPRING_DATASOURCE_PASSWORD: admin123
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_USERNAME: mtn_user
      SPRING_RABBITMQ_PASSWORD: mtn_password
      EMAIL_MOCK_MODE: true
      SMTP_HOST: smtp.gmail.com
      SMTP_PORT: 587
      SMTP_USERNAME: jorge.gangale@mtn.cl
      SMTP_PASSWORD: brye mjax brum bgux
    ports:
      - "8083:8083"
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  schema-registry:
    build:
      context: ./shared-libs/event-schema-registry
      dockerfile: Dockerfile
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/schema_registry_db
      SPRING_DATASOURCE_USERNAME: admin
      SPRING_DATASOURCE_PASSWORD: admin123
    ports:
      - "8084:8084"
    depends_on:
      postgres:
        condition: service_healthy

  api-gateway:
    build:
      context: ./api-gateway
      dockerfile: Dockerfile
    environment:
      SPRING_CLOUD_GATEWAY_ROUTES_0_ID: application-service
      SPRING_CLOUD_GATEWAY_ROUTES_0_URI: http://application-service:8080
      SPRING_CLOUD_GATEWAY_ROUTES_0_PREDICATES_0: Path=/api/applications/**,/api/users/**,/api/auth/**
      SPRING_CLOUD_GATEWAY_ROUTES_1_ID: evaluation-service
      SPRING_CLOUD_GATEWAY_ROUTES_1_URI: http://evaluation-service:8082
      SPRING_CLOUD_GATEWAY_ROUTES_1_PREDICATES_0: Path=/api/evaluations/**,/api/interviews/**
      SPRING_CLOUD_GATEWAY_ROUTES_2_ID: notification-service
      SPRING_CLOUD_GATEWAY_ROUTES_2_URI: http://notification-service:8083
      SPRING_CLOUD_GATEWAY_ROUTES_2_PREDICATES_0: Path=/api/notifications/**
    ports:
      - "8081:8081"
    depends_on:
      application-service:
        condition: service_healthy
      evaluation-service:
        condition: service_healthy

  # Monitoring Stack
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - ./monitoring/alert.rules.yml:/etc/prometheus/alert.rules.yml
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--web.enable-lifecycle'

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin123
    volumes:
      - grafana_data:/var/lib/grafana
      - ./monitoring/dashboards:/etc/grafana/provisioning/dashboards
      - ./monitoring/datasources:/etc/grafana/provisioning/datasources

  jaeger:
    image: jaegertracing/all-in-one:1.50
    ports:
      - "16686:16686"
      - "14250:14250"
    environment:
      COLLECTOR_OTLP_ENABLED: true

volumes:
  postgres_data:
  rabbitmq_data:
  grafana_data:

networks:
  default:
    name: mtn-network
```

## Comandos de Despliegue

### Desarrollo Local
```bash
# Iniciar solo infrastructure
docker-compose up -d postgres rabbitmq keycloak

# Esperar que los servicios estén listos
docker-compose logs -f postgres rabbitmq

# Ejecutar servicios con Maven
cd evaluation-service
mvn spring-boot:run -Dspring-boot.run.profiles=development

# En otra terminal - application service
cd ../
mvn spring-boot:run -Dspring-boot.run.profiles=development
```

### Despliegue Completo con Docker
```bash
# Build de todas las imágenes
docker-compose build

# Iniciar todos los servicios
docker-compose up -d

# Verificar estado
docker-compose ps
docker-compose logs -f

# Verificar health checks
curl http://localhost:8080/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

### Scaling de Servicios
```bash
# Escalar evaluation service
docker-compose up -d --scale evaluation-service=3

# Verificar load balancing
for i in {1..10}; do curl http://localhost:8081/api/evaluations/health; done
```

## Configuración por Ambiente

### Development
```yaml
# application-development.yml
spring:
  profiles:
    active: development
  datasource:
    url: jdbc:postgresql://localhost:5432/evaluations_db
    show-sql: true
  rabbitmq:
    host: localhost
logging:
  level:
    com.desafios.mtn: DEBUG
    org.springframework.amqp: DEBUG
management:
  endpoints:
    web:
      exposure:
        include: "*"
```

### Production
```yaml
# application-production.yml
spring:
  profiles:
    active: production
  datasource:
    url: ${DATABASE_URL}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  rabbitmq:
    host: ${RABBITMQ_HOST}
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}
logging:
  level:
    root: INFO
    com.desafios.mtn: INFO
management:
  endpoints:
    web:
      exposure:
        include: "health,info,metrics,prometheus"
```

## Troubleshooting

### Problemas Comunes
```bash
# Verificar logs de servicios
docker-compose logs evaluation-service
docker-compose logs application-service

# Conectividad de base de datos
docker-compose exec postgres psql -U admin -d evaluations_db -c "SELECT 1"

# Estado de RabbitMQ
docker-compose exec rabbitmq rabbitmqctl status
docker-compose exec rabbitmq rabbitmqctl list_queues

# Reiniciar servicio específico
docker-compose restart evaluation-service

# Verificar métricas
curl http://localhost:8082/actuator/metrics
curl http://localhost:9090/api/v1/query?query=up
```

### Monitoring y Alertas
```bash
# Acceder a Grafana
open http://localhost:3000
# admin / admin123

# Acceder a Prometheus
open http://localhost:9090

# Acceder a Jaeger
open http://localhost:16686

# RabbitMQ Management
open http://localhost:15672
# mtn_user / mtn_password
```

Esta guía proporciona un framework completo para desplegar el sistema MTN en diferentes ambientes, con configuración de infraestructura, monitoring, y troubleshooting.