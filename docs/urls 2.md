# MTN Admission System - Service URLs and Endpoints
## Complete Reference Guide for Phase 1 Implementation

### Version: 1.0
### Date: 2024-01-15  
### Status: Phase 1 - Containerized Architecture

---

## 🌐 Service Access URLs

### Core Application Services
| Service | URL | Port | Status | Purpose |
|---------|-----|------|--------|---------|
| **Frontend (React)** | http://localhost:3000 | 3000 | ✅ Active | Main user interface |
| **API Gateway** | http://localhost:8080 | 8080 | ✅ Active | Request routing and JWT validation |
| **Monolith API** | http://localhost:8081 | 8081 | ✅ Active | Core business logic |
| **Eureka Server** | http://localhost:8761 | 8761 | ✅ Active | Service discovery registry |

### Authentication & Security
| Service | URL | Port | Credentials | Purpose |
|---------|-----|------|-------------|---------|
| **Keycloak Admin** | http://localhost:8090/admin | 8090 | admin/admin123 | OIDC authentication provider |
| **Keycloak Realm** | http://localhost:8090/realms/mtn-admision | 8090 | - | Public realm endpoints |

### Database Services  
| Service | URL | Port | Credentials | Purpose |
|---------|-----|------|-------------|---------|
| **PostgreSQL** | localhost:5432 | 5432 | admin/admin123 | Primary database |
| **Database Name** | Admisión_MTN_DB | - | - | Main application database |

### Message Broker
| Service | URL | Port | Credentials | Purpose |
|---------|-----|------|-------------|---------|
| **RabbitMQ Management** | http://localhost:15672 | 15672 | admin/admin123 | Message broker management |
| **RabbitMQ AMQP** | amqp://localhost:5672 | 5672 | admin/admin123 | Message publishing/consuming |

---

## 📊 Observability Stack URLs

### Monitoring Dashboards
| Service | URL | Port | Credentials | Purpose |
|---------|-----|------|-------------|---------|
| **Grafana** | http://localhost:3001 | 3001 | admin/admin123 | Unified observability dashboard |
| **Prometheus** | http://localhost:9090 | 9090 | - | Metrics collection and queries |
| **Jaeger UI** | http://localhost:16686 | 16686 | - | Distributed tracing visualization |

### Log Management
| Service | URL | Port | Purpose |
|---------|-----|------|---------|
| **Loki** | http://localhost:3100 | 3100 | Log aggregation service |
| **Promtail** | http://localhost:9080 | 9080 | Log shipping to Loki |

### Telemetry Collection
| Service | URL | Port | Purpose |
|---------|-----|------|---------|
| **OpenTelemetry Collector** | http://localhost:4317 | 4317 | OTLP gRPC endpoint |
| **OpenTelemetry HTTP** | http://localhost:4318 | 4318 | OTLP HTTP endpoint |
| **OTel Collector Health** | http://localhost:8888 | 8888 | Collector health and metrics |

---

## 🔗 Health Check Endpoints

### Service Health Monitoring
```bash
# API Gateway health
curl http://localhost:8080/actuator/health

# Monolith API health  
curl http://localhost:8081/actuator/health

# Eureka Server health
curl http://localhost:8761/actuator/health

# Frontend health (via API Gateway)
curl http://localhost:8080/health
```

### Health Check Response Examples
```json
// API Gateway Health
{
  "status": "UP",
  "components": {
    "discoveryComposite": {
      "status": "UP",
      "components": {
        "eureka": { "status": "UP" }
      }
    },
    "gateway": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}

// Monolith API Health
{
  "status": "UP", 
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

---

## 📋 Management and Actuator Endpoints

### API Gateway Management
| Endpoint | URL | Purpose |
|----------|-----|---------|
| Health | http://localhost:8080/actuator/health | Service health status |
| Info | http://localhost:8080/actuator/info | Application information |
| Metrics | http://localhost:8080/actuator/metrics | Prometheus metrics |
| Gateway Routes | http://localhost:8080/actuator/gateway/routes | Current routing configuration |
| Circuit Breakers | http://localhost:8080/actuator/circuitbreakers | Circuit breaker status |

### Monolith API Management  
| Endpoint | URL | Purpose |
|----------|-----|---------|
| Health | http://localhost:8081/actuator/health | Service health status |
| Info | http://localhost:8081/actuator/info | Application information |
| Metrics | http://localhost:8081/actuator/metrics | Prometheus metrics |
| OpenAPI Spec | http://localhost:8081/v3/api-docs | OpenAPI JSON specification |
| Swagger UI | http://localhost:8081/swagger-ui.html | Interactive API documentation |

### Eureka Server Management
| Endpoint | URL | Purpose |
|----------|-----|---------|
| Dashboard | http://localhost:8761 | Service registry web UI |
| Apps Registry | http://localhost:8761/eureka/apps | Service registry (XML) |
| Apps JSON | http://localhost:8761/eureka/apps/accept/application/json | Service registry (JSON) |
| Health | http://localhost:8761/actuator/health | Eureka server health |
| Metrics | http://localhost:8761/actuator/metrics | Server metrics |

---

## 🔐 Authentication Endpoints

### Keycloak OIDC Endpoints
| Endpoint | URL | Purpose |
|----------|-----|---------|
| **Token Endpoint** | http://localhost:8090/realms/mtn-admision/protocol/openid-connect/token | JWT token issuance |
| **Authorization** | http://localhost:8090/realms/mtn-admision/protocol/openid-connect/auth | OAuth2 authorization |
| **User Info** | http://localhost:8090/realms/mtn-admision/protocol/openid-connect/userinfo | User profile information |
| **JWK Set** | http://localhost:8090/realms/mtn-admision/protocol/openid-connect/certs | JWT signature verification keys |
| **OpenID Config** | http://localhost:8090/realms/mtn-admision/.well-known/openid_configuration | OIDC discovery document |

### Authentication Flow Examples
```bash
# Get JWT token
curl -X POST http://localhost:8090/realms/mtn-admision/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=admision-frontend&username=admin@mtn.cl&password=admin123"

# Validate token
curl -H "Authorization: Bearer <jwt-token>" \
  http://localhost:8090/realms/mtn-admision/protocol/openid-connect/userinfo

# Get JWK Set (for signature validation)
curl http://localhost:8090/realms/mtn-admision/protocol/openid-connect/certs
```

---

## 📊 Metrics and Monitoring URLs

### Prometheus Metrics
| Service | Metrics URL | Key Metrics |
|---------|-------------|-------------|
| **API Gateway** | http://localhost:8080/actuator/prometheus | gateway.requests, http.server.requests |
| **Monolith API** | http://localhost:8081/actuator/prometheus | http.server.requests, jvm.memory.used, database.connections |
| **Eureka Server** | http://localhost:8761/actuator/prometheus | eureka.server.registry.size, eureka.server.renewals |

### Grafana Dashboard URLs
| Dashboard | URL | Purpose |
|-----------|-----|---------|
| **Home** | http://localhost:3001 | Main Grafana dashboard |
| **Spring Boot** | http://localhost:3001/d/spring-boot | Application metrics dashboard |
| **JVM Metrics** | http://localhost:3001/d/jvm-overview | JVM performance monitoring |
| **API Gateway** | http://localhost:3001/d/api-gateway | Gateway routing and performance |

### Jaeger Tracing URLs
| Feature | URL | Purpose |
|---------|-----|---------|
| **Search Traces** | http://localhost:16686/search | Find and analyze request traces |
| **Service Map** | http://localhost:16686/dependencies | Service dependency visualization |
| **Operations** | http://localhost:16686/trace/{trace-id} | Individual trace analysis |

---

## 🗄️ Database Connection Details

### PostgreSQL Connection
```bash
# Command line connection
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB"

# Connection string
postgresql://admin:admin123@localhost:5432/Admisión_MTN_DB

# JDBC URL
jdbc:postgresql://localhost:5432/Admisión_MTN_DB
```

### Database Management URLs
```bash
# Database size
curl http://localhost:8081/actuator/metrics/hikaricp.connections

# Connection pool metrics  
curl http://localhost:8081/actuator/metrics/hikaricp.connections.active

# Query statistics
curl http://localhost:8081/actuator/metrics | grep database
```

---

## 🔍 Service Discovery URLs

### Eureka Registry API
```bash
# Get all registered applications
curl http://localhost:8761/eureka/apps \
  -H "Accept: application/json"

# Get specific application instances
curl http://localhost:8761/eureka/apps/API-GATEWAY \
  -H "Accept: application/json"

# Service instance details
curl http://localhost:8761/eureka/apps/API-GATEWAY/{instance-id}

# Instance status
curl http://localhost:8761/eureka/apps/API-GATEWAY/{instance-id}/status
```

### Gateway Routing Information
```bash
# Current gateway routes
curl http://localhost:8080/actuator/gateway/routes

# Specific route details
curl http://localhost:8080/actuator/gateway/routes/{route-id}

# Route filters
curl http://localhost:8080/actuator/gateway/globalfilters
```

---

## 📚 Documentation URLs

### API Documentation
| Documentation | URL | Description |
|---------------|-----|-------------|
| **OpenAPI Spec** | http://localhost:8081/v3/api-docs | Complete API specification (JSON) |
| **Swagger UI** | http://localhost:8081/swagger-ui.html | Interactive API documentation |
| **API Endpoints** | http://localhost:8081/actuator/mappings | All available endpoints |

### Technical Documentation Links
| Document | Location | Description |
|----------|----------|-------------|
| **API Documentation** | [./api_documentation.md](./api_documentation.md) | Complete REST API reference |
| **Domain Mapping** | [./domain_map.md](./domain_map.md) | Service boundaries and dependencies |
| **Data Strategy** | [./data_strategy.md](./data_strategy.md) | Migration strategy and patterns |
| **Domain Events** | [./domain_events.md](./domain_events.md) | Event schemas and versioning |
| **Endpoint Inventory** | [./endpoints_inventory.md](./endpoints_inventory.md) | Complete endpoint catalog |

### Platform Documentation
| Service | Documentation | Description |
|---------|---------------|-------------|
| **API Gateway** | [../platform/api-gateway/README.md](../platform/api-gateway/README.md) | Gateway configuration and routing |
| **Eureka Server** | [../platform/eureka-server/README.md](../platform/eureka-server/README.md) | Service discovery setup |

---

## 🚀 Development and Testing URLs

### Development Endpoints
```bash
# Development health check
curl http://localhost:8081/actuator/health/db

# Application info
curl http://localhost:8081/actuator/info

# Environment variables
curl http://localhost:8081/actuator/env

# Configuration properties  
curl http://localhost:8081/actuator/configprops
```

### Testing and Debugging
| Purpose | URL | Description |
|---------|-----|-------------|
| **API Testing** | http://localhost:8081/swagger-ui.html | Interactive API testing |
| **JWT Debugging** | http://jwt.io | JWT token decoder |
| **Health Checks** | http://localhost:8080/actuator/health | Service health validation |
| **Metrics Testing** | http://localhost:9090/targets | Prometheus target status |

---

## 🔧 Configuration and Management URLs

### Spring Boot Actuator Endpoints
| Endpoint | Gateway URL | Monolith URL | Purpose |
|----------|-------------|--------------|---------|
| **/health** | :8080/actuator/health | :8081/actuator/health | Health status |
| **/info** | :8080/actuator/info | :8081/actuator/info | Application info |
| **/metrics** | :8080/actuator/metrics | :8081/actuator/metrics | Micrometer metrics |
| **/prometheus** | :8080/actuator/prometheus | :8081/actuator/prometheus | Prometheus format |
| **/env** | :8080/actuator/env | :8081/actuator/env | Environment properties |
| **/loggers** | :8080/actuator/loggers | :8081/actuator/loggers | Logging configuration |

### Configuration Validation
```bash
# Validate gateway configuration
curl http://localhost:8080/actuator/configprops

# Check active profiles
curl http://localhost:8081/actuator/env/spring.profiles.active

# Validate database connection
curl http://localhost:8081/actuator/health/db
```

---

## 🚨 Troubleshooting URLs

### Common Troubleshooting Endpoints
```bash
# Check if services are registered
curl http://localhost:8761/eureka/apps | grep -A 20 "API-GATEWAY"

# Validate JWT configuration
curl http://localhost:8090/realms/mtn-admision/.well-known/openid_configuration

# Test database connectivity
curl http://localhost:8081/actuator/health/db

# Check gateway routing
curl http://localhost:8080/actuator/gateway/routes | grep -A 10 "monolith"
```

### Log Analysis URLs
```bash
# Application logs via Loki
# Query: {container_name="admision-monolith"} |= "ERROR"
http://localhost:3001/explore?left=%5B"now-1h","now","loki",%7B"query":"%7Bcontainer_name%3D%22admision-monolith%22%7D%20%7C%3D%20%22ERROR%22"%7D%5D

# Gateway logs via Loki  
# Query: {container_name="api-gateway"} |= "JWT"
http://localhost:3001/explore?left=%5B"now-1h","now","loki",%7B"query":"%7Bcontainer_name%3D%22api-gateway%22%7D%20%7C%3D%20%22JWT%22"%7D%5D
```

---

## 📞 Support and Contact Information

### Technical Contacts
- **Technical Lead**: jorge.gangale@mtn.cl
- **System Architecture**: Phase 1 Documentation Team
- **Emergency Support**: See incident response procedures

### Issue Reporting
- **GitHub Issues**: Create issue with appropriate labels
- **Documentation Issues**: Update relevant README files
- **Security Issues**: Follow security disclosure process

### Resource Links
| Resource | URL | Description |
|----------|-----|-------------|
| **Spring Boot Docs** | https://docs.spring.io/spring-boot/docs/3.5.0/reference/htmlsingle/ | Framework documentation |
| **Spring Cloud Gateway** | https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/ | Gateway configuration |
| **Keycloak Docs** | https://www.keycloak.org/documentation | Authentication setup |
| **Prometheus Docs** | https://prometheus.io/docs/ | Metrics and monitoring |
| **Grafana Docs** | https://grafana.com/docs/ | Dashboard configuration |

---

## 🎯 Quick Access Checklist

### ✅ Phase 1 Services Status Check
```bash
# 1. Frontend accessible
curl -s http://localhost:3000 > /dev/null && echo "✅ Frontend UP" || echo "❌ Frontend DOWN"

# 2. API Gateway accessible  
curl -s http://localhost:8080/actuator/health > /dev/null && echo "✅ Gateway UP" || echo "❌ Gateway DOWN"

# 3. Monolith API accessible
curl -s http://localhost:8081/actuator/health > /dev/null && echo "✅ Monolith UP" || echo "❌ Monolith DOWN"

# 4. Eureka Server accessible
curl -s http://localhost:8761/actuator/health > /dev/null && echo "✅ Eureka UP" || echo "❌ Eureka DOWN"

# 5. Keycloak accessible
curl -s http://localhost:8090/realms/mtn-admision > /dev/null && echo "✅ Keycloak UP" || echo "❌ Keycloak DOWN"

# 6. Database accessible
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" -c "SELECT 1;" > /dev/null 2>&1 && echo "✅ Database UP" || echo "❌ Database DOWN"

# 7. Grafana accessible
curl -s http://localhost:3001/api/health > /dev/null && echo "✅ Grafana UP" || echo "❌ Grafana DOWN"
```

### 🔗 Essential URLs for Daily Use
1. **Main Application**: http://localhost:3000
2. **API Testing**: http://localhost:8081/swagger-ui.html  
3. **Service Registry**: http://localhost:8761
4. **Monitoring**: http://localhost:3001
5. **Authentication**: http://localhost:8090/admin

---

**📋 Complete URL reference for MTN Admission System Phase 1 deployment!**