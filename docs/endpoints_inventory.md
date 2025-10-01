# Inventario de Endpoints - Sistema de Admisión MTN

## Resumen por Dominios

| Dominio | Controllers | Endpoints | Descripción |
|---------|-------------|-----------|-------------|
| **Autenticación** | AuthController | 4 | Login, registro, verificación email |
| **Usuarios** | UserController, SchoolUserController | 12 | CRUD usuarios del sistema |
| **Postulaciones** | ApplicationController, StateTransitionValidationController | 15 | Gestión completa de postulaciones |
| **Evaluaciones** | EvaluationController, EvaluationScheduleController | 18 | Sistema completo de evaluaciones |
| **Entrevistas** | InterviewController, InterviewWorkflowController, InterviewAvailabilityController, InterviewResponseController | 22 | Gestión integral de entrevistas |
| **Documentos** | DocumentController | 6 | Upload y gestión de archivos |
| **Notificaciones** | EmailController, InstitutionalEmailController, EmailManagementController, NotificationController | 16 | Sistema completo de emails |
| **Monitoreo** | MonitoringController, AnalyticsController, DashboardController | 12 | Observabilidad y métricas |
| **Administración** | BackupController, CacheController, SecurityController | 8 | Funciones administrativas |
| **Otros** | TestController, DebugController, CorsTestController, DatabaseTestController | 10 | Testing y debugging |

---

## Inventario Detallado de Endpoints

### 🔐 Dominio: Autenticación (Auth)
| Método | Ruta | Roles | Request Schema | Response Schema | Códigos HTTP |
|--------|------|-------|----------------|----------------|--------------|
| POST | `/api/auth/login` | PUBLIC | LoginRequest | AuthResponse | 200, 401, 429 |
| POST | `/api/auth/register` | PUBLIC | RegisterRequest | AuthResponse | 201, 400, 409 |
| POST | `/api/auth/verify-email` | PUBLIC | EmailVerificationRequest | MessageResponse | 200, 400, 404 |
| POST | `/api/auth/refresh-token` | AUTHENTICATED | RefreshTokenRequest | AuthResponse | 200, 401 |

### 👥 Dominio: Usuarios (Users)
| Método | Ruta | Roles | Request Schema | Response Schema | Códigos HTTP |
|--------|------|-------|----------------|----------------|--------------|
| GET | `/api/users` | ADMIN | Query Params | Page<UserResponse> | 200 |
| POST | `/api/users` | ADMIN | CreateUserRequest | UserResponse | 201, 400, 409 |
| GET | `/api/users/{id}` | ADMIN | - | UserResponse | 200, 404 |
| PUT | `/api/users/{id}` | ADMIN | UpdateUserRequest | UserResponse | 200, 400, 404 |
| DELETE | `/api/users/{id}` | ADMIN | - | MessageResponse | 200, 404 |
| POST | `/api/users/{id}/reset-password` | ADMIN | - | MessageResponse | 200, 404 |
| GET | `/api/users/statistics` | ADMIN | - | UserStatistics | 200 |
| GET | `/api/users/by-role/{role}` | ADMIN | - | List<UserResponse> | 200 |
| PUT | `/api/users/{id}/activate` | ADMIN | - | MessageResponse | 200, 404 |
| PUT | `/api/users/{id}/deactivate` | ADMIN | - | MessageResponse | 200, 404 |
| GET | `/api/school-users` | TEACHER, COORDINATOR, PSYCHOLOGIST, CYCLE_DIRECTOR, ADMIN | - | List<SchoolUserResponse> | 200 |
| GET | `/api/school-users/evaluators` | TEACHER, COORDINATOR, PSYCHOLOGIST, CYCLE_DIRECTOR, ADMIN | - | List<EvaluatorResponse> | 200 |

### 📝 Dominio: Postulaciones (Applications) 
| Método | Ruta | Roles | Request Schema | Response Schema | Códigos HTTP |
|--------|------|-------|----------------|----------------|--------------|
| POST | `/api/applications` | APODERADO, ADMIN | CreateApplicationRequest | ApplicationResponse | 201, 400 |
| GET | `/api/applications` | ADMIN, TEACHER, PSYCHOLOGIST, CYCLE_DIRECTOR | Query Params | Page<ApplicationResponse> | 200 |
| GET | `/api/applications/{id}` | APODERADO (own), ADMIN, TEACHER, PSYCHOLOGIST, CYCLE_DIRECTOR | - | ApplicationResponse | 200, 404 |
| PUT | `/api/applications/{id}` | APODERADO (own), ADMIN | UpdateApplicationRequest | ApplicationResponse | 200, 400, 404 |
| DELETE | `/api/applications/{id}` | APODERADO (own), ADMIN | - | MessageResponse | 200, 404 |
| PUT | `/api/applications/{id}/status` | ADMIN, TEACHER, PSYCHOLOGIST, CYCLE_DIRECTOR | UpdateStatusRequest | ApplicationResponse | 200, 400, 404 |
| PUT | `/api/applications/{id}/archive` | ADMIN | - | ApplicationResponse | 200, 404 |
| GET | `/api/applications/my-applications` | APODERADO | - | List<ApplicationResponse> | 200 |
| GET | `/api/applications/public/all` | PUBLIC | - | List<PublicApplicationResponse> | 200 |
| GET | `/api/applications/statistics` | ADMIN | - | ApplicationStatistics | 200 |
| GET | `/api/applications/by-status/{status}` | ADMIN, TEACHER, PSYCHOLOGIST, CYCLE_DIRECTOR | - | List<ApplicationResponse> | 200 |
| POST | `/api/applications/{id}/submit` | APODERADO (own) | - | ApplicationResponse | 200, 400, 404 |
| GET | `/api/applications/{id}/workflow` | ADMIN, TEACHER, PSYCHOLOGIST, CYCLE_DIRECTOR | - | WorkflowResponse | 200, 404 |
| GET | `/api/state-transitions/validate` | ADMIN, TEACHER, PSYCHOLOGIST, CYCLE_DIRECTOR | Query Params | ValidationResponse | 200 |
| GET | `/api/state-transitions/allowed/{currentStatus}` | ADMIN, TEACHER, PSYCHOLOGIST, CYCLE_DIRECTOR | - | List<ApplicationStatus> | 200 |

### 📊 Dominio: Evaluaciones (Evaluations)
| Método | Ruta | Roles | Request Schema | Response Schema | Códigos HTTP |
|--------|------|-------|----------------|----------------|--------------|
| GET | `/api/evaluations` | TEACHER, PSYCHOLOGIST, CYCLE_DIRECTOR, ADMIN | Query Params | Page<EvaluationResponse> | 200 |
| POST | `/api/evaluations` | TEACHER, PSYCHOLOGIST, CYCLE_DIRECTOR, ADMIN | CreateEvaluationRequest | EvaluationResponse | 201, 400 |
| GET | `/api/evaluations/{id}` | TEACHER (own), PSYCHOLOGIST (own), CYCLE_DIRECTOR, ADMIN | - | EvaluationResponse | 200, 404 |
| PUT | `/api/evaluations/{id}` | TEACHER (own), PSYCHOLOGIST (own), CYCLE_DIRECTOR (own), ADMIN | UpdateEvaluationRequest | EvaluationResponse | 200, 400, 404 |
| DELETE | `/api/evaluations/{id}` | ADMIN | - | MessageResponse | 200, 404 |
| GET | `/api/evaluations/application/{applicationId}` | TEACHER, PSYCHOLOGIST, CYCLE_DIRECTOR, ADMIN | - | List<EvaluationResponse> | 200 |
| GET | `/api/evaluations/my-evaluations` | TEACHER, PSYCHOLOGIST, CYCLE_DIRECTOR | - | List<EvaluationResponse> | 200 |
| GET | `/api/evaluations/pending` | TEACHER, PSYCHOLOGIST, CYCLE_DIRECTOR, ADMIN | - | List<EvaluationResponse> | 200 |
| POST | `/api/evaluations/{id}/submit` | TEACHER (own), PSYCHOLOGIST (own), CYCLE_DIRECTOR (own) | - | EvaluationResponse | 200, 400, 404 |
| GET | `/api/evaluations/statistics` | ADMIN | - | EvaluationStatistics | 200 |
| GET | `/api/evaluations/by-type/{type}` | TEACHER, PSYCHOLOGIST, CYCLE_DIRECTOR, ADMIN | - | List<EvaluationResponse> | 200 |
| GET | `/api/evaluations/by-evaluator/{evaluatorId}` | ADMIN | - | List<EvaluationResponse> | 200 |
| POST | `/api/evaluations/assign` | ADMIN, COORDINATOR | AssignEvaluationRequest | EvaluationResponse | 201, 400 |
| GET | `/api/evaluation-schedules` | TEACHER, PSYCHOLOGIST, CYCLE_DIRECTOR, ADMIN | - | List<ScheduleResponse> | 200 |
| POST | `/api/evaluation-schedules` | ADMIN | CreateScheduleRequest | ScheduleResponse | 201, 400 |
| PUT | `/api/evaluation-schedules/{id}` | ADMIN | UpdateScheduleRequest | ScheduleResponse | 200, 400, 404 |
| DELETE | `/api/evaluation-schedules/{id}` | ADMIN | - | MessageResponse | 200, 404 |
| GET | `/api/evaluation-schedules/evaluator/{evaluatorId}` | TEACHER (own), PSYCHOLOGIST (own), CYCLE_DIRECTOR (own), ADMIN | - | List<ScheduleResponse> | 200 |
| GET | `/api/evaluation-schedules/available-slots` | TEACHER, PSYCHOLOGIST, CYCLE_DIRECTOR, ADMIN | Query Params | List<TimeSlotResponse> | 200 |

### 🎯 Dominio: Entrevistas (Interviews)
| Método | Ruta | Roles | Request Schema | Response Schema | Códigos HTTP |
|--------|------|-------|----------------|----------------|--------------|
| GET | `/api/interviews` | CYCLE_DIRECTOR, ADMIN | Query Params | Page<InterviewResponse> | 200 |
| POST | `/api/interviews` | CYCLE_DIRECTOR, ADMIN | CreateInterviewRequest | InterviewResponse | 201, 400 |
| GET | `/api/interviews/{id}` | CYCLE_DIRECTOR, ADMIN, APODERADO (own) | - | InterviewResponse | 200, 404 |
| PUT | `/api/interviews/{id}` | CYCLE_DIRECTOR (own), ADMIN | UpdateInterviewRequest | InterviewResponse | 200, 400, 404 |
| DELETE | `/api/interviews/{id}` | ADMIN | - | MessageResponse | 200, 404 |
| GET | `/api/interviews/application/{applicationId}` | CYCLE_DIRECTOR, ADMIN, APODERADO (own) | - | List<InterviewResponse> | 200 |
| GET | `/api/interviews/my-interviews` | CYCLE_DIRECTOR | - | List<InterviewResponse> | 200 |
| POST | `/api/interviews/{id}/schedule` | CYCLE_DIRECTOR (own), ADMIN | ScheduleInterviewRequest | InterviewResponse | 200, 400, 404 |
| POST | `/api/interviews/{id}/complete` | CYCLE_DIRECTOR (own), ADMIN | CompleteInterviewRequest | InterviewResponse | 200, 400, 404 |
| GET | `/api/interviews/pending` | CYCLE_DIRECTOR, ADMIN | - | List<InterviewResponse> | 200 |
| GET | `/api/interviews/statistics` | ADMIN | - | InterviewStatistics | 200 |
| POST | `/api/interview-workflow/schedule` | CYCLE_DIRECTOR, ADMIN | ScheduleWorkflowRequest | WorkflowResponse | 200, 400 |
| POST | `/api/interview-workflow/notify` | CYCLE_DIRECTOR, ADMIN | NotifyWorkflowRequest | WorkflowResponse | 200, 400 |
| GET | `/api/interview-workflow/status/{applicationId}` | CYCLE_DIRECTOR, ADMIN, APODERADO (own) | - | WorkflowStatusResponse | 200, 404 |
| GET | `/api/interview-availability` | CYCLE_DIRECTOR, ADMIN | Query Params | List<AvailabilityResponse> | 200 |
| POST | `/api/interview-availability` | CYCLE_DIRECTOR | CreateAvailabilityRequest | AvailabilityResponse | 201, 400 |
| PUT | `/api/interview-availability/{id}` | CYCLE_DIRECTOR (own), ADMIN | UpdateAvailabilityRequest | AvailabilityResponse | 200, 400, 404 |
| DELETE | `/api/interview-availability/{id}` | CYCLE_DIRECTOR (own), ADMIN | - | MessageResponse | 200, 404 |
| GET | `/api/interview-availability/interviewer/{interviewerId}` | CYCLE_DIRECTOR (own), ADMIN | - | List<AvailabilityResponse> | 200 |
| POST | `/api/interview-responses/confirm` | APODERADO | ConfirmInterviewRequest | ResponseMessage | 200, 400, 404 |
| POST | `/api/interview-responses/reschedule` | APODERADO | RescheduleInterviewRequest | ResponseMessage | 200, 400, 404 |
| GET | `/api/interview-responses/token/{token}` | PUBLIC | - | InterviewDetailsResponse | 200, 404 |
| GET | `/api/interviewer-schedules` | CYCLE_DIRECTOR, ADMIN | - | List<InterviewerScheduleResponse> | 200 |

### 📎 Dominio: Documentos (Documents)
| Método | Ruta | Roles | Request Schema | Response Schema | Códigos HTTP |
|--------|------|-------|----------------|----------------|--------------|
| POST | `/api/documents/upload` | APODERADO, ADMIN | MultipartFile | DocumentResponse | 201, 400, 413 |
| GET | `/api/documents/{id}` | APODERADO (own), ADMIN, TEACHER, PSYCHOLOGIST, CYCLE_DIRECTOR | - | File Download | 200, 404 |
| DELETE | `/api/documents/{id}` | APODERADO (own), ADMIN | - | MessageResponse | 200, 404 |
| GET | `/api/documents/application/{applicationId}` | APODERADO (own), ADMIN, TEACHER, PSYCHOLOGIST, CYCLE_DIRECTOR | - | List<DocumentResponse> | 200 |
| GET | `/api/documents/types` | AUTHENTICATED | - | List<DocumentTypeResponse> | 200 |
| GET | `/api/documents/statistics` | ADMIN | - | DocumentStatistics | 200 |

### 📧 Dominio: Notificaciones (Notifications)
| Método | Ruta | Roles | Request Schema | Response Schema | Códigos HTTP |
|--------|------|-------|----------------|----------------|--------------|
| POST | `/api/emails/send` | ADMIN | SendEmailRequest | MessageResponse | 200, 400 |
| POST | `/api/emails/verify` | PUBLIC | EmailVerificationRequest | MessageResponse | 200, 400 |
| GET | `/api/emails/templates` | ADMIN | - | List<EmailTemplateResponse> | 200 |
| POST | `/api/institutional-emails/application-received` | ADMIN, SYSTEM | ApplicationReceivedRequest | MessageResponse | 200, 400 |
| POST | `/api/institutional-emails/interview-invitation` | ADMIN, CYCLE_DIRECTOR | InterviewInvitationRequest | MessageResponse | 200, 400 |
| POST | `/api/institutional-emails/status-update` | ADMIN | StatusUpdateRequest | MessageResponse | 200, 400 |
| POST | `/api/institutional-emails/document-reminder` | ADMIN | DocumentReminderRequest | MessageResponse | 200, 400 |
| POST | `/api/institutional-emails/admission-result` | ADMIN | AdmissionResultRequest | MessageResponse | 200, 400 |
| GET | `/api/institutional-emails/templates/{type}` | ADMIN | - | EmailTemplateResponse | 200, 404 |
| GET | `/api/admin/email-management/status` | ADMIN | - | EmailSystemStatus | 200 |
| POST | `/api/admin/email-management/test` | ADMIN | TestEmailRequest | MessageResponse | 200, 400 |
| GET | `/api/admin/email-management/statistics` | ADMIN | - | EmailStatistics | 200 |
| GET | `/api/admin/email-management/configuration-help` | ADMIN | - | ConfigurationHelp | 200 |
| GET | `/api/notifications` | AUTHENTICATED | Query Params | Page<NotificationResponse> | 200 |
| PUT | `/api/notifications/{id}/read` | AUTHENTICATED | - | NotificationResponse | 200, 404 |
| GET | `/api/notifications/unread-count` | AUTHENTICATED | - | CountResponse | 200 |

### 📊 Dominio: Monitoreo (Monitoring)
| Método | Ruta | Roles | Request Schema | Response Schema | Códigos HTTP |
|--------|------|-------|----------------|----------------|--------------|
| GET | `/api/monitoring/system-status` | ADMIN | - | SystemStatusResponse | 200 |
| GET | `/api/monitoring/health` | ADMIN | - | HealthResponse | 200 |
| GET | `/api/monitoring/metrics` | ADMIN | - | MetricsResponse | 200 |
| GET | `/api/monitoring/logs` | ADMIN | Query Params | LogsResponse | 200 |
| GET | `/api/analytics/applications` | ADMIN | Query Params | ApplicationAnalytics | 200 |
| GET | `/api/analytics/users` | ADMIN | Query Params | UserAnalytics | 200 |
| GET | `/api/analytics/evaluations` | ADMIN | Query Params | EvaluationAnalytics | 200 |
| GET | `/api/analytics/performance` | ADMIN | Query Params | PerformanceAnalytics | 200 |
| GET | `/api/dashboard/admin` | ADMIN | - | AdminDashboardResponse | 200 |
| GET | `/api/dashboard/teacher` | TEACHER, PSYCHOLOGIST, CYCLE_DIRECTOR | - | TeacherDashboardResponse | 200 |
| GET | `/api/dashboard/apoderado` | APODERADO | - | ApoderadoDashboardResponse | 200 |
| GET | `/api/dashboard/statistics` | ADMIN | - | DashboardStatistics | 200 |

### ⚙️ Dominio: Administración (Administration)
| Método | Ruta | Roles | Request Schema | Response Schema | Códigos HTTP |
|--------|------|-------|----------------|----------------|--------------|
| POST | `/api/backup/create` | ADMIN | BackupRequest | BackupResponse | 200, 400 |
| GET | `/api/backup/list` | ADMIN | - | List<BackupInfo> | 200 |
| POST | `/api/backup/restore/{backupId}` | ADMIN | - | RestoreResponse | 200, 400, 404 |
| DELETE | `/api/backup/{backupId}` | ADMIN | - | MessageResponse | 200, 404 |
| POST | `/api/cache/clear` | ADMIN | CacheRequest | MessageResponse | 200 |
| GET | `/api/cache/stats` | ADMIN | - | CacheStatistics | 200 |
| POST | `/api/security/audit` | ADMIN | AuditRequest | AuditResponse | 200 |
| GET | `/api/security/events` | ADMIN | Query Params | SecurityEventsResponse | 200 |

### 🧪 Dominio: Testing/Debug (Others)
| Método | Ruta | Roles | Request Schema | Response Schema | Códigos HTTP |
|--------|------|-------|----------------|----------------|--------------|
| GET | `/api/test/ping` | PUBLIC | - | String | 200 |
| GET | `/api/test/protected` | AUTHENTICATED | - | String | 200, 401 |
| GET | `/api/test/admin-only` | ADMIN | - | String | 200, 403 |
| POST | `/api/test/echo` | PUBLIC | Any JSON | Any JSON | 200 |
| GET | `/api/debug/info` | ADMIN | - | DebugInfo | 200 |
| GET | `/api/debug/threads` | ADMIN | - | ThreadInfo | 200 |
| GET | `/api/debug/memory` | ADMIN | - | MemoryInfo | 200 |
| GET | `/api/cors-test` | PUBLIC | - | String | 200 |
| GET | `/api/database-test/connection` | ADMIN | - | ConnectionStatus | 200 |
| GET | `/api/database-test/query` | ADMIN | - | QueryResult | 200 |

---

## Actuator Endpoints (Observabilidad)

| Ruta | Descripción | Roles |
|------|-------------|-------|
| `/actuator/health` | Health check del sistema | PUBLIC |
| `/actuator/info` | Información de la aplicación | PUBLIC |
| `/actuator/metrics` | Métricas de la aplicación | ADMIN |
| `/actuator/prometheus` | Métricas en formato Prometheus | ADMIN |
| `/actuator/loggers` | Configuración de loggers | ADMIN |
| `/actuator/env` | Variables de entorno | ADMIN |
| `/actuator/configprops` | Propiedades de configuración | ADMIN |
| `/actuator/mappings` | Mapeo de endpoints | ADMIN |

---

## OpenAPI Documentation

| Ruta | Descripción |
|------|-------------|
| `/v3/api-docs` | Especificación OpenAPI en JSON |
| `/v3/api-docs.yaml` | Especificación OpenAPI en YAML |
| `/swagger-ui.html` | Interfaz Swagger UI |
| `/swagger-ui/index.html` | Interfaz Swagger UI alternativa |

---

**Total de Endpoints:** ~123 endpoints distribuidos en 9 dominios principales

**Última actualización:** $(date '+%Y-%m-%d %H:%M:%S') - Fase 0 Pre-flight