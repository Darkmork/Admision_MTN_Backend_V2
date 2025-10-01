# 🏗️ Arquitectura del Sistema - Sistema de Admisión MTN

**Última actualización:** 2025-10-01  
**Versión:** 2.0  
**Estado:** Arquitectura Híbrida en Transición

---

## 📋 ÍNDICE

1. [Resumen Ejecutivo](#resumen-ejecutivo)
2. [Arquitectura Actual](#arquitectura-actual)
3. [Componentes del Sistema](#componentes-del-sistema)
4. [Stack Tecnológico](#stack-tecnológico)
5. [Flujos de Datos](#flujos-de-datos)
6. [Decisiones de Arquitectura](#decisiones-de-arquitectura)
7. [Roadmap de Migración](#roadmap-de-migración)
8. [Configuración por Entorno](#configuración-por-entorno)

---

## 🎯 RESUMEN EJECUTIVO

### Estado Actual

El Sistema de Admisión MTN opera con una **arquitectura híbrida** que incluye:

- **Backend Activo:** Node.js Mock Services (desarrollo y producción actual)
- **Backend Preparado:** Spring Boot Microservices (para producción futura)
- **Frontend:** React 19 + TypeScript con Vite
- **Gateway:** NGINX como reverse proxy
- **Base de Datos:** PostgreSQL 14+

### Decisión Arquitectónica Principal

```
┌─────────────────────────────────────────────────────────────┐
│  ESTRATEGIA: Desarrollo con Node.js, Migración a Spring Boot│
│  ─────────────────────────────────────────────────────────  │
│  Fase Actual:  Node.js Mock Services (ACTIVO)               │
│  Fase Futura:  Spring Boot Microservices                    │
│  Razón:        Rapidez de desarrollo + Escalabilidad futura │
└─────────────────────────────────────────────────────────────┘
```

---

## 🏛️ ARQUITECTURA ACTUAL

### Diagrama de Alto Nivel

```
┌──────────────────────────────────────────────────────────────────┐
│                         USUARIO FINAL                             │
│                  (Apoderados, Profesores, Admin)                  │
└────────────────────────────┬─────────────────────────────────────┘
                             │
                             ▼
                    ┌────────────────┐
                    │   FRONTEND     │
                    │  React 19 + TS │
                    │  Vite (5173)   │
                    └────────┬───────┘
                             │ HTTP/REST
                             ▼
                    ┌────────────────┐
                    │  NGINX GATEWAY │
                    │   Port 8080    │
                    │  (Reverse Proxy│
                    │   + CORS)      │
                    └────────┬───────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
    ┌─────────────┐ ┌──────────────┐ ┌──────────────┐
    │ User Service│ │App. Service  │ │Eval. Service │
    │   (8082)    │ │   (8083)     │ │   (8084)     │
    │  Node.js    │ │  Node.js     │ │  Node.js     │
    └─────┬───────┘ └──────┬───────┘ └──────┬───────┘
          │                │                │
          └────────────────┼────────────────┘
                           │
                           ▼
                  ┌────────────────┐
                  │   PostgreSQL   │
                  │  Admisión_MTN  │
                  │    (5432)      │
                  └────────────────┘
```

### Arquitectura de Microservicios (Preparada para Futura Migración)

```
┌─────────────────────────────────────────────────────────────┐
│              Spring Boot Microservices (8761)                │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐            │
│  │   Eureka   │  │   Config   │  │  Gateway   │            │
│  │   Server   │  │   Server   │  │  (Spring)  │            │
│  └────────────┘  └────────────┘  └────────────┘            │
│         │               │                │                   │
│  ┌──────┴───────────────┴────────────────┴─────┐            │
│  │                                               │            │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  │            │
│  │  │  User    │  │  App.    │  │  Eval.   │  │            │
│  │  │ Service  │  │ Service  │  │ Service  │  │            │
│  │  │ (Spring) │  │ (Spring) │  │ (Spring) │  │            │
│  │  └──────────┘  └──────────┘  └──────────┘  │            │
│  │                                               │            │
│  └───────────────────┬───────────────────────────┘            │
│                      │                                        │
│                      ▼                                        │
│             ┌────────────────┐                               │
│             │   PostgreSQL   │                               │
│             └────────────────┘                               │
└─────────────────────────────────────────────────────────────┘
```

---

## 🧩 COMPONENTES DEL SISTEMA

### 1. Frontend (React + TypeScript)

**Ubicación:** `Admision_MTN_front/`

**Características:**
- Framework: React 19
- Language: TypeScript 5
- Build Tool: Vite 5
- State Management: Context API
- Routing: React Router v6
- HTTP Client: Axios
- UI Components: Custom + Tailwind CSS
- Testing: Vitest + Playwright E2E

**Puertos:**
- Desarrollo: `5173`
- Preview: `4173`

**Servicios Principales:**
```
services/
├── api.ts                    # Axios instance con JWT interceptors
├── authService.ts            # Autenticación general
├── professorAuthService.ts   # Auth profesores
├── apoderadoAuthService.ts   # Auth apoderados
├── applicationService.ts     # Gestión postulaciones
├── interviewService.ts       # Calendario entrevistas
├── evaluationService.ts      # Evaluaciones académicas
├── documentService.ts        # Upload documentos
├── notificationService.ts    # Notificaciones email/SMS
└── dashboardService.ts       # Estadísticas y métricas
```

---

### 2. Backend - Node.js Mock Services (ACTIVO)

**Ubicación:** `Admision_MTN_backend/`

**Servicios:**

| Servicio | Puerto | Archivo | Responsabilidad |
|----------|--------|---------|-----------------|
| User Service | 8082 | `mock-user-service.js` | Autenticación, CRUD usuarios, RBAC |
| Application Service | 8083 | `mock-application-service.js` | Postulaciones, documentos, estudiantes |
| Evaluation Service | 8084 | `mock-evaluation-service.js` | Entrevistas, evaluaciones, scheduling |
| Notification Service | 8085 | `mock-notification-service.js` | Emails, SMS, templates dinámicos |
| Dashboard Service | 8086 | `mock-dashboard-service.js` | Estadísticas, métricas, reportes |
| Guardian Service | 8085 | `mock-guardian-service.js` | Gestión apoderados/sostenedores |

**Tecnologías:**
- Runtime: Node.js 18+
- Framework: Express.js
- Database: `pg` (PostgreSQL client)
- Auth: JWT con `jsonwebtoken`
- Password Hashing: `bcryptjs`
- Email: `nodemailer` con SMTP
- HTTP Client: `axios`

**Ventajas:**
- ✅ Desarrollo rápido (sin compilación)
- ✅ Menor overhead de configuración
- ✅ Fácil debugging
- ✅ Prototipado rápido de APIs

**Desventajas:**
- ❌ Menos type-safety que Java/Spring
- ❌ Sin ecosistema robusto de microservicios (Eureka, Config Server)
- ❌ Escalabilidad limitada sin orquestación

---

### 3. Backend - Spring Boot Microservices (PREPARADO, NO ACTIVO)

**Ubicación:** `Admision_MTN_backend/[service-name]/`

**Servicios Implementados:**

| Servicio | Puerto | Directorio | Estado |
|----------|--------|------------|--------|
| Eureka Server | 8761 | `eureka-server/` | ✅ Configurado |
| Config Server | 8888 | `config-server/` | ⚠️ Parcial |
| API Gateway | 8080 | `gateway/` | ⚠️ Parcial |
| User Service | 8082 | `user-service/` | ✅ Configurado |
| Application Service | 8083 | `application-service/` | ✅ Configurado |
| Evaluation Service | 8084 | `evaluation-service/` | ✅ Configurado |
| Notification Service | 8085 | `notification-service/` | ✅ Configurado |

**Stack:**
- Framework: Spring Boot 3.2
- Java Version: 17
- Database: Spring Data JPA + PostgreSQL
- Discovery: Netflix Eureka
- Config: Spring Cloud Config
- Gateway: Spring Cloud Gateway
- Resilience: Resilience4j (Circuit Breaker)
- Migrations: Flyway
- Security: Spring Security + JWT
- Docs: SpringDoc OpenAPI

**Ventajas:**
- ✅ Type-safe con Java
- ✅ Ecosistema maduro de microservicios
- ✅ Escalabilidad horizontal con Kubernetes
- ✅ Circuit breakers, retry, fallback patterns
- ✅ Métricas con Actuator + Prometheus

**Desventajas:**
- ❌ Mayor complejidad de setup
- ❌ Compilación + build time más largo
- ❌ Mayor consumo de memoria

---

### 4. API Gateway (NGINX)

**Ubicación:** `Admision_MTN_backend/local-gateway.conf`

**Rol:**
- Reverse proxy para todos los servicios
- Gestión de CORS
- Balanceo de carga
- SSL termination (producción)
- Rate limiting (producción)

**Configuración Activa:**
- Archivo: `local-gateway.conf` (actualizado Oct 1, 2025)
- Puerto: `8080`
- Worker Processes: `auto`
- Client Max Body Size: `10M` (uploads)

**Rutas Configuradas:**
```nginx
/api/auth/*         → User Service (8082)
/api/users/*        → User Service (8082)
/api/applications/* → Application Service (8083)
/api/interviews/*   → Evaluation Service (8084)
/api/evaluations/*  → Evaluation Service (8084)
/api/notifications/*→ Notification Service (8085)
/api/dashboard/*    → Dashboard Service (8086)
/api/guardians/*    → Guardian Service (8085)
/api/documents/*    → Application Service (8083)
/api/email/*        → Notification Service (8085)
/gateway/status     → Health check
/health             → Health check
```

**CORS Configuration:**
```nginx
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, PATCH, OPTIONS
Access-Control-Allow-Headers: 
  Origin, X-Requested-With, Content-Type, Accept, Authorization,
  Cache-Control, x-correlation-id, x-request-time, x-timezone,
  x-client-type, x-client-version
Access-Control-Allow-Credentials: true
```

**⚠️ Nota sobre Múltiples Configuraciones:**
El proyecto tiene 8 archivos `.conf` diferentes. Solo `local-gateway.conf` está activo:

```
✅ ACTIVO:   local-gateway.conf (21 KB, Oct 1 12:48)
📦 ARCHIVO:  gateway-hybrid.conf, gateway-simple.conf, etc.
```

Ver sección [Consolidación de Configs](#consolidación-de-configuraciones) para plan de limpieza.

---

### 5. Base de Datos (PostgreSQL)

**Configuración:**
- Nombre: `Admisión_MTN_DB`
- Host: `localhost` (desarrollo), RDS (producción)
- Puerto: `5432`
- Usuario: `admin`
- Encoding: `UTF-8`

**Tablas Principales (35 total):**

| Tabla | Registros | Descripción |
|-------|-----------|-------------|
| `users` | ~50 | Usuarios del sistema (admin, profesores, apoderados) |
| `applications` | ~26 | Postulaciones de estudiantes |
| `students` | ~26 | Información de estudiantes |
| `parents` | ~52 | Padres (father_id, mother_id) |
| `supporters` | ~12 | Sostenedores económicos |
| `guardians` | ~12 | Apoderados académicos |
| `interviews` | ~15 | Entrevistas programadas |
| `evaluations` | ~20 | Evaluaciones académicas |
| `interviewer_schedules` | ~10 | Disponibilidad entrevistadores |
| `documents` | Variable | Documentos adjuntos |
| `notifications` | Variable | Log de notificaciones |

**Migraciones:**
- Método Actual: SQL scripts manuales en `reports/`
- Método Futuro: Flyway (configurado pero no activo)

**Scripts de Migración:**
```
reports/
├── 001_add_supporter_guardian_tables.sql
├── 002_add_supporter_guardian_tables.sql
└── [otros scripts]
```

---

## 🛠️ STACK TECNOLÓGICO

### Frontend

| Categoría | Tecnología | Versión | Uso |
|-----------|------------|---------|-----|
| **Core** | React | 19.x | UI Framework |
| | TypeScript | 5.x | Type safety |
| | Vite | 5.x | Build tool |
| **Routing** | React Router | 6.x | SPA routing |
| **HTTP** | Axios | 1.x | API calls |
| **Styling** | Tailwind CSS | 3.x | Utility-first CSS |
| **Forms** | React Hook Form | 7.x | Form validation |
| **Testing** | Vitest | 1.x | Unit tests |
| | Playwright | 1.x | E2E tests |

### Backend (Node.js - ACTIVO)

| Categoría | Tecnología | Versión | Uso |
|-----------|------------|---------|-----|
| **Runtime** | Node.js | 18+ | JavaScript runtime |
| **Framework** | Express | 4.x | Web framework |
| **Database** | pg | 8.x | PostgreSQL client |
| **Auth** | jsonwebtoken | 9.x | JWT tokens |
| | bcryptjs | 2.x | Password hashing |
| **Email** | nodemailer | 6.x | SMTP client |
| **HTTP** | axios | 1.x | Service-to-service |
| **Patterns** | opossum | 8.x | Circuit breaker |

### Backend (Spring Boot - PREPARADO)

| Categoría | Tecnología | Versión | Uso |
|-----------|------------|---------|-----|
| **Framework** | Spring Boot | 3.2.x | Microservices |
| **Language** | Java | 17 | Backend language |
| **Database** | Spring Data JPA | 3.2.x | ORM |
| | PostgreSQL Driver | 42.x | DB connection |
| **Security** | Spring Security | 6.x | Auth + RBAC |
| **Discovery** | Eureka Client | 4.x | Service registry |
| **Config** | Spring Cloud Config | 4.x | Centralized config |
| **Gateway** | Spring Cloud Gateway | 4.x | API Gateway |
| **Resilience** | Resilience4j | 2.x | Circuit breaker |
| **Migrations** | Flyway | 9.x | DB versioning |
| **Docs** | SpringDoc OpenAPI | 2.x | API documentation |

### Infrastructure

| Categoría | Tecnología | Uso |
|-----------|------------|-----|
| **Gateway** | NGINX | Reverse proxy, load balancer |
| **Database** | PostgreSQL 14+ | RDBMS |
| **Containerization** | Docker | Service isolation |
| **Orchestration** | Docker Compose | Local development |
| **CI/CD** | GitHub Actions | Automation (en progreso) |
| **Secrets** | AWS Secrets Manager | Production secrets (futuro) |

---

## 🔄 FLUJOS DE DATOS

### 1. Flujo de Autenticación

```
┌─────────┐        ┌──────────┐       ┌──────────┐       ┌──────────┐
│ Cliente │──POST→ │  NGINX   │──→   │   User   │──→   │PostgreSQL│
│(React)  │        │  (8080)  │       │ Service  │       │  (5432)  │
└─────────┘        └──────────┘       │  (8082)  │       └──────────┘
     │                   │             └──────────┘             │
     │                   │                   │                  │
     │                   │         1. Validate credentials      │
     │                   │         2. Query user ───────────────┤
     │                   │         3. Verify password           │
     │                   │         4. Generate JWT token        │
     │                   │                   │                  │
     │                   │         200 OK + JWT                 │
     │                   │◀──────────────────┤                  │
     │       200 OK + JWT                                       │
     │◀──────────────────┤                                      │
     │                                                           │
     │  Store in localStorage                                   │
     │  (auth_token)                                            │
     └───────────────────────────────────────────────────────────┘
```

### 2. Flujo de Creación de Postulación

```
Usuario → Frontend → NGINX → Application Service → PostgreSQL
  │          │         │              │                  │
  │ Fill form│         │              │                  │
  ├─────────▶│         │              │                  │
  │          │ POST    │              │                  │
  │          │/api/app │              │                  │
  │          ├────────▶│ Verify JWT   │                  │
  │          │         ├─────────────▶│ 1. INSERT student│
  │          │         │              ├─────────────────▶│
  │          │         │              │ 2. INSERT parents│
  │          │         │              ├─────────────────▶│
  │          │         │              │ 3. INSERT supporter
  │          │         │              ├─────────────────▶│
  │          │         │              │ 4. INSERT guardian
  │          │         │              ├─────────────────▶│
  │          │         │              │ 5. INSERT application
  │          │         │              ├─────────────────▶│
  │          │         │              │◀─────────────────┤
  │          │         │              │ Return IDs       │
  │          │         │◀─────────────┤                  │
  │          │         │ 201 Created  │                  │
  │          │◀────────┤              │                  │
  │          │ Show success            │                  │
  │◀─────────┤         │              │                  │
```

### 3. Flujo de Notificaciones

```
Application Service → Notification Service → SMTP Server → Email
        │                      │                   │          │
    Event│                      │                   │          │
  Created│                      │                   │          │
        ├──POST /notifications─▶│                   │          │
        │   {applicant_email,   │                   │          │
        │    template: "welcome"}                    │          │
        │                       │ 1. Fetch template │          │
        │                       │ 2. Render with data         │
        │                       │ 3. Send email     │          │
        │                       ├──────────────────▶│          │
        │                       │                   │ SMTP     │
        │                       │                   ├─────────▶│
        │                       │                   │          │
        │                       │◀──────────────────┤          │
        │                       │ 250 OK            │          │
        │◀──────────────────────┤                   │          │
        │ 200 OK                │                   │          │
```

---

## 🎯 DECISIONES DE ARQUITECTURA

### ADR-001: Arquitectura Híbrida (Node.js + Spring Boot)

**Contexto:**
- Necesidad de rapidez en desarrollo MVP
- Equipo con experiencia en Node.js y Java
- Requisito futuro de escalabilidad

**Decisión:**
Implementar arquitectura híbrida:
1. Fase 1 (Actual): Node.js Mock Services para desarrollo rápido
2. Fase 2 (Futuro): Migración gradual a Spring Boot

**Consecuencias:**
- ✅ Time-to-market más rápido
- ✅ Flexibilidad para cambiar stack
- ❌ Mantenimiento de dos codebases en paralelo
- ❌ Posible duplicación de lógica durante migración

**Status:** APROBADO | Fecha: 2024-09-01

---

### ADR-002: PostgreSQL como Database Principal

**Contexto:**
- Necesidad de relaciones complejas (estudiantes, padres, sostenedores, guardians)
- Requisitos de integridad referencial
- Queries complejos con múltiples JOINs

**Decisión:**
PostgreSQL como RDBMS principal en lugar de MongoDB/NoSQL.

**Razones:**
- ✅ Foreign keys para integridad referencial
- ✅ ACID transactions
- ✅ Soporte JSON para datos semiestructurados
- ✅ Experiencia del equipo

**Alternativas Consideradas:**
- MongoDB: Rechazado por falta de relaciones complejas
- MySQL: PostgreSQL preferido por mejor soporte JSON y funcionalidades avanzadas

**Status:** APROBADO | Fecha: 2024-08-15

---

### ADR-003: NGINX como API Gateway

**Contexto:**
- Necesidad de reverse proxy
- Gestión centralizada de CORS
- Posible balanceo de carga futuro

**Decisión:**
NGINX en lugar de Spring Cloud Gateway para fase actual.

**Razones:**
- ✅ Independiente del stack backend
- ✅ Alta performance
- ✅ Configuración simple para desarrollo
- ✅ Fácil transición a K8s Ingress

**Alternativas Consideradas:**
- Spring Cloud Gateway: Reservado para migración futura
- Kong: Overhead excesivo para MVP

**Status:** APROBADO | Fecha: 2024-09-02

---

### ADR-004: JWT para Autenticación

**Contexto:**
- Arquitectura stateless requerida
- Frontend SPA necesita tokens portables
- Múltiples roles (ADMIN, TEACHER, COORDINATOR, etc.)

**Decisión:**
JWT con HS512 para auth tokens.

**Configuración:**
```javascript
{
  algorithm: 'HS512',
  expiresIn: '24h',
  payload: { userId, email, role }
}
```

**Razones:**
- ✅ Stateless (no sesiones en servidor)
- ✅ Portabilidad entre servicios
- ✅ Payload con roles para RBAC

**Consideraciones de Seguridad:**
- JWT_SECRET debe ser 512+ bits
- Tokens rotados cada 24h
- Refresh tokens para sesiones largas (futuro)

**Status:** APROBADO | Fecha: 2024-08-20

---

## 🛣️ ROADMAP DE MIGRACIÓN

### Fase 1: MVP con Node.js Mock Services ✅ (COMPLETADO)

**Duración:** Sep 2024 - Oct 2024

**Objetivos:**
- ✅ Implementar APIs básicas
- ✅ Conectar frontend con backend
- ✅ Deploy en desarrollo
- ✅ Validación con usuarios

**Entregables:**
- ✅ 5 servicios Node.js funcionales
- ✅ Frontend React completo
- ✅ Base de datos con 35 tablas
- ✅ NGINX gateway configurado

---

### Fase 2: Preparación Spring Boot ✅ (COMPLETADO)

**Duración:** Sep 2024

**Objetivos:**
- ✅ Crear estructura de microservicios Spring
- ✅ Configurar Eureka Server
- ✅ Implementar servicios base (User, Application, Evaluation, Notification)
- ✅ Flyway migrations

**Estado:**
- ✅ Eureka Server configurado
- ✅ 4 microservicios con pom.xml
- ⚠️ Sin tests unitarios
- ⚠️ Sin CI/CD pipeline

---

### Fase 3: Migración Gradual (EN PROGRESO)

**Duración:** Oct 2024 - Dic 2024

**Objetivos:**
- [ ] Migrar User Service a Spring Boot
- [ ] Configurar Spring Cloud Config
- [ ] Implementar Circuit Breaker con Resilience4j
- [ ] Setup CI/CD con GitHub Actions
- [ ] Tests unitarios + E2E

**Estrategia de Migración:**
```
Servicio por servicio en orden:
1. User Service (auth crítico)
2. Application Service (core business)
3. Evaluation Service
4. Notification Service
5. Dashboard Service

Para cada servicio:
- Deploy Spring Boot en paralelo a Node.js
- Configurar feature flag en NGINX
- Testing con 10% tráfico
- Gradual rollout 10% → 50% → 100%
- Deprecar Node.js service
```

---

### Fase 4: Optimización y Escalabilidad (FUTURO)

**Duración:** 2025 Q1

**Objetivos:**
- [ ] Kubernetes deployment
- [ ] Horizontal Pod Autoscaler
- [ ] Prometheus + Grafana monitoring
- [ ] Distributed tracing (Zipkin/Jaeger)
- [ ] Secrets management con AWS Secrets Manager

---

## 🌍 CONFIGURACIÓN POR ENTORNO

### Desarrollo Local

**Backend:**
```bash
# Node.js Mock Services
cd Admision_MTN_backend
node mock-user-service.js &
node mock-application-service.js &
node mock-evaluation-service.js &
node mock-notification-service.js &
node mock-dashboard-service.js &

# NGINX
sudo nginx -c "$(pwd)/local-gateway.conf"
```

**Frontend:**
```bash
cd Admision_MTN_front
npm run dev  # Port 5173
```

**Database:**
```bash
psql -h localhost -U admin -d "Admisión_MTN_DB"
```

---

### Staging (Futuro)

**Backend:** Spring Boot Microservices en AWS ECS

**Configuración:**
- Spring Profiles: `staging`
- Database: RDS PostgreSQL
- Secrets: AWS Secrets Manager
- Logs: CloudWatch

---

### Producción (Futuro)

**Backend:** Spring Boot en Kubernetes (EKS)

**Configuración:**
- Spring Profiles: `production`
- Database: RDS Multi-AZ
- Secrets: AWS Secrets Manager con rotación
- Gateway: Istio Service Mesh
- Monitoring: Prometheus + Grafana
- Logs: ELK Stack

---

## 📊 CONSOLIDACIÓN DE CONFIGURACIONES

### Problema Actual

8 archivos `.conf` en raíz del proyecto:

| Archivo | Tamaño | Fecha | Estado |
|---------|--------|-------|--------|
| `local-gateway.conf` | 21 KB | Oct 1 | ✅ ACTIVO |
| `nginx-gateway.conf` | 20 KB | Sep 4 | 📦 ARCHIVO |
| `cors-clean-gateway.conf` | 8.4 KB | Sep 22 | 📦 ARCHIVO |
| `local-gateway-fixed.conf` | 5.7 KB | Sep 22 | 📦 ARCHIVO |
| `gateway-microservices.conf` | 13 KB | Sep 2 | 📦 ARCHIVO |
| `gateway-hybrid.conf` | 5.2 KB | Sep 2 | 📦 ARCHIVO |
| `gateway-simple.conf` | 3.1 KB | Sep 2 | 📦 ARCHIVO |
| `gateway-nginx.conf` | 3.1 KB | Sep 2 | 📦 ARCHIVO |

### Plan de Limpieza

```bash
# 1. Crear directorio de archivo
mkdir -p configs/archive

# 2. Mover configs no usadas
mv gateway-hybrid.conf configs/archive/
mv gateway-simple.conf configs/archive/
mv cors-clean-gateway.conf configs/archive/
mv local-gateway-fixed.conf configs/archive/
mv gateway-microservices.conf configs/archive/
mv gateway-nginx.conf configs/archive/
mv nginx-gateway.conf configs/archive/

# 3. Renombrar config activa para claridad
mv local-gateway.conf nginx.conf

# 4. Crear symlink (opcional)
ln -s nginx.conf local-gateway.conf

# 5. Documentar en README
echo "Active NGINX config: nginx.conf" >> README.md
```

---

## 📚 RECURSOS Y DOCUMENTACIÓN

### Documentación del Proyecto

```
Admision_MTN_backend/
├── ARCHITECTURE.md           # Este archivo
├── CLAUDE.md                 # Instrucciones para Claude Code
├── README.md                 # Setup y quick start
├── MICROSERVICES_GUIDE.md    # Guía de microservicios
├── INTEGRATION_GUIDE.md      # Integración frontend-backend
├── reports/
│   ├── CI_AUDIT_REPORT.md    # Auditoría CI/CD
│   ├── SECRETS_MANAGEMENT_GUIDE.md  # Seguridad
│   └── SECURITY_FIXES_SUMMARY.md
└── scripts/
    ├── validate_all.sh       # Validación completa
    └── verify_secrets.sh     # Verificación seguridad
```

### Diagramas

**Herramientas:**
- Draw.io: `docs/diagrams/architecture.drawio`
- PlantUML: `docs/diagrams/sequences.puml`

### Contacto

**Tech Lead:** devops@mtn.cl  
**Arquitecto:** No asignado  
**DevOps:** devops@mtn.cl

---

## 📝 CHANGELOG

**v2.0 - 2025-10-01**
- ✅ Documentada arquitectura híbrida
- ✅ Explicado estado actual vs. futuro
- ✅ ADRs para decisiones principales
- ✅ Roadmap de migración
- ✅ Plan de consolidación de configs NGINX

**v1.0 - 2024-09-01**
- ✅ Arquitectura inicial Node.js mock services

---

**Fin de Documento** | Sistema de Admisión MTN | 2025-10-01
