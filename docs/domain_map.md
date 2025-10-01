# Mapeo de Dominios - Sistema de Admisión MTN

## Arquitectura de Dominios para Migración a Microservicios

### 🏗️ Estructura Actual → Microservicios Target

| Dominio | Controllers | Packages | Entidades Principales | Servicios | Futuro Microservicio |
|---------|-------------|----------|----------------------|-----------|---------------------|
| **Autenticación** | AuthController | auth, security | User, JwtToken | UserService, JwtService, AuthenticationManager | **Auth Service** |
| **Usuarios** | UserController, SchoolUserController | user, admin | User, UserRole, UserProfile | AdminUserService, UserService | **User Service** |
| **Postulaciones** | ApplicationController, StateTransitionValidationController | application, workflow | Application, ApplicationStatus, Workflow | ApplicationService, WorkflowService | **Application Service** |
| **Evaluaciones** | EvaluationController, EvaluationScheduleController | evaluation, schedule | Evaluation, EvaluationType, Schedule | EvaluationService, ScheduleService | **Evaluation Service** |
| **Entrevistas** | InterviewController, InterviewWorkflowController, InterviewAvailabilityController, InterviewResponseController | interview, workflow | Interview, InterviewSchedule, Availability | InterviewService, WorkflowService | **Interview Service** |
| **Documentos** | DocumentController | document, storage | Document, DocumentType, FileMetadata | DocumentService, FileStorageService | **File Service** |
| **Notificaciones** | EmailController, InstitutionalEmailController, EmailManagementController, NotificationController | email, notification | EmailTemplate, Notification, EmailLog | EmailService, InstitutionalEmailService, NotificationService | **Notification Service** |
| **Monitoreo** | MonitoringController, AnalyticsController, DashboardController | monitoring, analytics | SystemMetrics, AuditLog, Performance | MonitoringService, AnalyticsService | **Monitoring Service** |

---

## Mapeo Detallado por Dominio

### 🔐 Dominio: Autenticación (Auth Service)
**Package:** `com.desafios.admision_mtn.auth`
```
├── controller/
│   └── AuthController.java               # /api/auth/*
├── service/
│   ├── UserService.java                  # Autenticación de usuarios
│   ├── JwtService.java                   # Generación y validación JWT
│   └── RateLimitingService.java          # Control de intentos de login
├── entity/
│   ├── User.java                         # Usuario principal
│   └── RefreshToken.java                 # Tokens de actualización
├── dto/
│   ├── LoginRequest.java
│   ├── RegisterRequest.java
│   ├── AuthResponse.java
│   └── EmailVerificationRequest.java
└── security/
    ├── SecurityConfig.java               # Configuración Spring Security
    ├── SecurityValidationService.java    # Validaciones de seguridad
    └── JwtAuthenticationFilter.java      # Filtro JWT
```

### 👥 Dominio: Usuarios (User Service)
**Package:** `com.desafios.admision_mtn.user`
```
├── controller/
│   ├── UserController.java               # /api/users/*
│   └── SchoolUserController.java         # /api/school-users/*
├── service/
│   ├── AdminUserService.java             # CRUD usuarios del sistema
│   └── UserService.java                  # Operaciones generales
├── entity/
│   ├── User.java                         # Entidad principal
│   ├── UserRole.java                     # Enum de roles
│   ├── EducationalLevel.java             # Niveles educativos
│   └── Subject.java                      # Materias de especialización
├── dto/
│   ├── CreateUserRequest.java
│   ├── UpdateUserRequest.java
│   ├── UserResponse.java
│   ├── SchoolUserResponse.java
│   └── EvaluatorResponse.java
└── repository/
    └── UserRepository.java               # Acceso a datos
```

### 📝 Dominio: Postulaciones (Application Service)
**Package:** `com.desafios.admision_mtn.application`
```
├── controller/
│   ├── ApplicationController.java        # /api/applications/*
│   └── StateTransitionValidationController.java # /api/state-transitions/*
├── service/
│   ├── ApplicationService.java           # Lógica de postulaciones
│   └── WorkflowService.java              # Flujos de estados
├── entity/
│   ├── Application.java                  # Postulación principal
│   ├── ApplicationStatus.java            # Estados de postulación
│   ├── Student.java                      # Datos del estudiante
│   ├── Parent.java                       # Datos de padres
│   ├── Guardian.java                     # Datos de apoderados
│   └── Supporter.java                    # Datos de sostenedores
├── dto/
│   ├── CreateApplicationRequest.java
│   ├── ApplicationResponse.java
│   ├── UpdateStatusRequest.java
│   ├── WorkflowResponse.java
│   └── PublicApplicationResponse.java
└── repository/
    ├── ApplicationRepository.java
    ├── StudentRepository.java
    ├── ParentRepository.java
    └── GuardianRepository.java
```

### 📊 Dominio: Evaluaciones (Evaluation Service)
**Package:** `com.desafios.admision_mtn.evaluation`
```
├── controller/
│   ├── EvaluationController.java         # /api/evaluations/*
│   └── EvaluationScheduleController.java # /api/evaluation-schedules/*
├── service/
│   ├── EvaluationService.java            # Lógica de evaluaciones
│   └── ScheduleService.java              # Programación de evaluaciones
├── entity/
│   ├── Evaluation.java                   # Evaluación principal
│   ├── EvaluationType.java               # Tipos de evaluación
│   ├── EvaluationCriteria.java           # Criterios de evaluación
│   └── EvaluationSchedule.java           # Programación
├── dto/
│   ├── CreateEvaluationRequest.java
│   ├── EvaluationResponse.java
│   ├── UpdateEvaluationRequest.java
│   ├── AssignEvaluationRequest.java
│   ├── CreateScheduleRequest.java
│   └── ScheduleResponse.java
└── repository/
    ├── EvaluationRepository.java
    └── EvaluationScheduleRepository.java
```

### 🎯 Dominio: Entrevistas (Interview Service)
**Package:** `com.desafios.admision_mtn.interview`
```
├── controller/
│   ├── InterviewController.java          # /api/interviews/*
│   ├── InterviewWorkflowController.java  # /api/interview-workflow/*
│   ├── InterviewAvailabilityController.java # /api/interview-availability/*
│   ├── InterviewResponseController.java  # /api/interview-responses/*
│   └── InterviewerScheduleController.java # /api/interviewer-schedules/*
├── service/
│   ├── InterviewService.java             # Lógica de entrevistas
│   ├── InterviewWorkflowService.java     # Flujos de entrevistas
│   └── AvailabilityService.java          # Disponibilidad de entrevistadores
├── entity/
│   ├── Interview.java                    # Entrevista principal
│   ├── InterviewSchedule.java            # Programación de entrevistas
│   ├── InterviewAvailability.java        # Disponibilidad
│   └── InterviewResponse.java            # Respuestas de familias
├── dto/
│   ├── CreateInterviewRequest.java
│   ├── InterviewResponse.java
│   ├── ScheduleInterviewRequest.java
│   ├── CompleteInterviewRequest.java
│   ├── ScheduleWorkflowRequest.java
│   ├── NotifyWorkflowRequest.java
│   ├── CreateAvailabilityRequest.java
│   ├── ConfirmInterviewRequest.java
│   └── RescheduleInterviewRequest.java
└── repository/
    ├── InterviewRepository.java
    ├── InterviewScheduleRepository.java
    └── InterviewAvailabilityRepository.java
```

### 📎 Dominio: Documentos (File Service)
**Package:** `com.desafios.admision_mtn.document`
```
├── controller/
│   └── DocumentController.java           # /api/documents/*
├── service/
│   ├── DocumentService.java              # Lógica de documentos
│   └── FileStorageService.java           # Almacenamiento de archivos
├── entity/
│   ├── Document.java                     # Documento principal
│   ├── DocumentType.java                 # Tipos de documento
│   └── DocumentMetadata.java             # Metadatos de archivos
├── dto/
│   ├── DocumentResponse.java
│   ├── DocumentTypeResponse.java
│   └── DocumentStatistics.java
└── repository/
    └── DocumentRepository.java
```

### 📧 Dominio: Notificaciones (Notification Service)
**Package:** `com.desafios.admision_mtn.notification`
```
├── controller/
│   ├── EmailController.java              # /api/emails/*
│   ├── InstitutionalEmailController.java # /api/institutional-emails/*
│   ├── EmailManagementController.java    # /api/admin/email-management/*
│   └── NotificationController.java       # /api/notifications/*
├── service/
│   ├── EmailService.java                 # Envío de emails básicos
│   ├── InstitutionalEmailService.java    # Emails institucionales
│   └── NotificationService.java          # Sistema de notificaciones
├── entity/
│   ├── EmailTemplate.java                # Plantillas de email
│   ├── EmailLog.java                     # Log de emails enviados
│   └── Notification.java                 # Notificaciones internas
├── dto/
│   ├── SendEmailRequest.java
│   ├── EmailVerificationRequest.java
│   ├── ApplicationReceivedRequest.java
│   ├── InterviewInvitationRequest.java
│   ├── StatusUpdateRequest.java
│   ├── DocumentReminderRequest.java
│   ├── AdmissionResultRequest.java
│   ├── TestEmailRequest.java
│   └── NotificationResponse.java
└── repository/
    ├── EmailTemplateRepository.java
    ├── EmailLogRepository.java
    └── NotificationRepository.java
```

### 📊 Dominio: Monitoreo (Monitoring Service)
**Package:** `com.desafios.admision_mtn.monitoring`
```
├── controller/
│   ├── MonitoringController.java         # /api/monitoring/*
│   ├── AnalyticsController.java          # /api/analytics/*
│   └── DashboardController.java          # /api/dashboard/*
├── service/
│   ├── MonitoringService.java            # Monitoreo del sistema
│   ├── AnalyticsService.java             # Análisis y métricas
│   └── DashboardService.java             # Datos para dashboards
├── entity/
│   ├── SystemMetrics.java                # Métricas del sistema
│   ├── AuditLog.java                     # Log de auditoría
│   └── PerformanceMetrics.java           # Métricas de rendimiento
├── dto/
│   ├── SystemStatusResponse.java
│   ├── HealthResponse.java
│   ├── MetricsResponse.java
│   ├── ApplicationAnalytics.java
│   ├── UserAnalytics.java
│   ├── EvaluationAnalytics.java
│   ├── PerformanceAnalytics.java
│   ├── AdminDashboardResponse.java
│   ├── TeacherDashboardResponse.java
│   └── ApoderadoDashboardResponse.java
└── repository/
    ├── SystemMetricsRepository.java
    ├── AuditLogRepository.java
    └── PerformanceMetricsRepository.java
```

---

## Dependencias Entre Dominios

### 🔗 Comunicación Cross-Domain

| Dominio Origen | Dominio Destino | Tipo Comunicación | Endpoints/Eventos |
|----------------|-----------------|-------------------|-------------------|
| **Postulaciones** | **Usuarios** | Síncrona | GET /api/users/{id} (validación apoderado) |
| **Postulaciones** | **Documentos** | Síncrona | GET /api/documents/application/{id} |
| **Postulaciones** | **Notificaciones** | Asíncrona | ApplicationCreated, StatusChanged events |
| **Evaluaciones** | **Postulaciones** | Síncrona | GET /api/applications/{id} |
| **Evaluaciones** | **Usuarios** | Síncrona | GET /api/users/evaluators (asignación) |
| **Entrevistas** | **Postulaciones** | Síncrona | GET /api/applications/{id} |
| **Entrevistas** | **Usuarios** | Síncrona | GET /api/users/interviewers |
| **Entrevistas** | **Notificaciones** | Asíncrona | InterviewScheduled, InterviewCompleted events |
| **Documentos** | **Postulaciones** | Síncrona | Validación de documentos requeridos |
| **Notificaciones** | **Usuarios** | Síncrona | GET /api/users/{id} (datos para emails) |
| **Monitoreo** | **Todos** | Síncrona | Health checks y métricas |

### 📊 Datos Compartidos

| Entidad | Dominios que la Usan | Estrategia de Datos |
|---------|---------------------|---------------------|
| **User** | Auth, Users, Applications, Evaluations, Interviews | Database per Service + Event Sourcing |
| **Application** | Applications, Evaluations, Interviews, Documents | Shared Database → Event Sourcing |
| **Student** | Applications, Evaluations, Interviews | Event-driven replication |
| **EmailTemplate** | Notifications, Interviews | Shared Configuration Service |

---

## Patrones de Migración Recomendados

### 🔄 Strangler Fig Pattern
1. **Fase 1**: Extraer User Service (más independiente)
2. **Fase 2**: Notification Service (event-driven)
3. **Fase 3**: Document Service (stateless)
4. **Fase 4**: Evaluation Service
5. **Fase 5**: Interview Service  
6. **Fase 6**: Application Service (core business)

### 📡 Event-Driven Communication
```yaml
Events:
  - ApplicationCreated
  - ApplicationStatusChanged
  - EvaluationCompleted
  - InterviewScheduled
  - DocumentUploaded
  - UserRegistered
  - EmailSent
```

### 🗄️ Database Strategy
- **Auth Service**: PostgreSQL (users, tokens, sessions)
- **User Service**: PostgreSQL (users, roles, profiles)
- **Application Service**: PostgreSQL (applications, workflow, family data)
- **Evaluation Service**: PostgreSQL (evaluations, schedules, criteria)
- **Interview Service**: PostgreSQL (interviews, availability, responses)
- **Document Service**: PostgreSQL (metadata) + Object Storage (files)
- **Notification Service**: PostgreSQL (templates, logs) + Message Queue
- **Monitoring Service**: Time-series DB (InfluxDB) + PostgreSQL

---

**Generado automáticamente en Fase 0 Pre-flight**  
**Fecha:** $(date '+%Y-%m-%d %H:%M:%S')  
**Versión del Sistema:** 1.0.0