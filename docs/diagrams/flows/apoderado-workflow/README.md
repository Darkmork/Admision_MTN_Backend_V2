# Proceso del Apoderado (Guardian) - Sistema de Admisión Monte Tabor y Nazaret

## Descripción General

Este documento describe el flujo completo del proceso de admisión desde la perspectiva del apoderado (guardian), desde el acceso inicial hasta la decisión final de admisión.

## Fases del Proceso

### 1. Acceso Inicial y Autenticación

#### 1.1 Verificación de Email
- **Trigger**: Apoderado recibe email de invitación con link de verificación
- **Endpoint**: `POST /api/email/verify-email`
- **Servicio**: Notification Service (puerto 8085)
- **Gateway**: NGINX en puerto 8080
- **Validaciones**:
  - Formato de email válido
  - RUT chileno válido (verificación de dígito verificador)
  - Token de verificación no expirado
- **Respuesta Exitosa**: Redirect a página de registro
- **Errores**:
  - `400`: Email o RUT inválido
  - `404`: Token no encontrado o expirado
  - `409`: Email ya verificado

**Archivos de Implementación**:
- Frontend: `Admision_MTN_front/pages/EmailVerification.tsx`
- Service: `Admision_MTN_front/services/emailVerificationService.ts`
- Backend: `Admision_MTN_backend/mock-notification-service.js` (líneas 180-250)

#### 1.2 Registro de Cuenta
- **Endpoint**: `POST /api/auth/register`
- **Servicio**: User Service (puerto 8082)
- **Datos Requeridos**:
  - Email (verificado previamente)
  - RUT
  - Nombre completo
  - Contraseña (mínimo 8 caracteres)
  - Teléfono
- **Validaciones**:
  - Email único en el sistema
  - RUT único
  - Contraseña cumple requisitos de seguridad
  - Todos los campos requeridos presentes
- **Proceso**:
  1. Hasheo de contraseña con BCrypt
  2. Creación de usuario con rol `APODERADO`
  3. Generación de token JWT (HS512)
  4. Envío de email de bienvenida
- **Respuesta Exitosa**: Usuario creado + token JWT
- **Errores**:
  - `400`: Datos inválidos
  - `409`: Email o RUT ya existe
  - `422`: Validación de campos fallida

**Archivos de Implementación**:
- Frontend: `Admision_MTN_front/pages/Register.tsx`
- Service: `Admision_MTN_front/services/authService.ts`
- Backend: `Admision_MTN_backend/mock-user-service.js` (líneas 400-520)

#### 1.3 Login
- **Endpoint**: `POST /api/auth/login`
- **Servicio**: User Service (puerto 8082)
- **Datos Requeridos**:
  - Email
  - Contraseña
- **Proceso**:
  1. Búsqueda de usuario por email
  2. Verificación de contraseña con BCrypt
  3. Generación de token JWT con expiración de 24h
  4. Almacenamiento en localStorage (`apoderado_token`)
- **Respuesta Exitosa**: Token JWT + datos de usuario
- **Errores**:
  - `401`: Credenciales inválidas
  - `403`: Usuario inactivo o bloqueado
  - `404`: Usuario no encontrado

**Archivos de Implementación**:
- Frontend: `Admision_MTN_front/pages/Login.tsx`
- Service: `Admision_MTN_front/services/apoderadoAuthService.ts`
- Backend: `Admision_MTN_backend/mock-user-service.js` (líneas 550-650)

---

### 2. Envío de Postulación

#### 2.1 Acceso al Formulario
- **Ruta**: `/dashboard/application`
- **Componente**: `ApplicationForm.tsx`
- **Autenticación**: Requiere token JWT válido con rol `APODERADO`
- **Validación Inicial**: Verificar que no existe postulación activa

#### 2.2 Completar Formulario Multi-Paso

**Paso 1: Información del Estudiante**
- Nombre completo
- RUT (con validación de dígito verificador)
- Fecha de nacimiento (cálculo automático de edad)
- Sexo
- Grado al que postula (con validación de edad apropiada)
- Año de postulación (actual + 1 automáticamente)

**Paso 2: Información del Padre**
- Nombre completo
- RUT
- Profesión/Ocupación
- Dirección completa
- Teléfono
- Email

**Paso 3: Información de la Madre**
- Nombre completo
- RUT
- Profesión/Ocupación
- Dirección completa
- Teléfono
- Email

**Paso 4: Información del Apoderado/Tutor**
- Relación con el estudiante
- Vive con el estudiante (Sí/No)
- Teléfono de emergencia
- Dirección alternativa (si es diferente)

**Paso 5: Información Médica**
- Alergias
- Condiciones médicas pre-existentes
- Medicamentos actuales
- Contacto médico de emergencia

**Paso 6: Información Académica**
- Colegio actual/anterior
- Notas del último año
- Logros académicos
- Actividades extracurriculares

**Validaciones en Cada Paso**:
- Campos requeridos completados
- Formatos válidos (email, RUT, teléfono)
- Rangos válidos (edad, fechas)
- Consistencia entre datos

**Archivos de Implementación**:
- `Admision_MTN_front/components/application/ApplicationForm.tsx`
- `Admision_MTN_front/components/application/steps/StudentInfoStep.tsx`
- `Admision_MTN_front/components/application/steps/FatherInfoStep.tsx`
- `Admision_MTN_front/components/application/steps/MotherInfoStep.tsx`
- `Admision_MTN_front/components/application/steps/GuardianInfoStep.tsx`
- `Admision_MTN_front/components/application/steps/MedicalInfoStep.tsx`
- `Admision_MTN_front/components/application/steps/AcademicInfoStep.tsx`

#### 2.3 Carga de Documentos
- **Endpoint**: `POST /api/applications/documents`
- **Servicio**: Application Service (puerto 8083)
- **Documentos Requeridos**:
  - Certificado de nacimiento (PDF)
  - Foto del estudiante (JPG/PNG)
  - Certificado de notas del último año (PDF)
  - Informe de personalidad (PDF, opcional)
  - Certificado médico (PDF)
- **Validaciones**:
  - Tipo de archivo permitido
  - Tamaño máximo: 5MB por archivo
  - Documentos requeridos presentes
- **Proceso**:
  1. Validación de tipo MIME
  2. Validación de tamaño
  3. Generación de nombre único
  4. Almacenamiento en servidor
  5. Registro en base de datos
- **Errores**:
  - `413`: Archivo muy grande
  - `415`: Tipo de archivo no permitido
  - `422`: Documento requerido faltante

**Archivos de Implementación**:
- `Admision_MTN_front/components/application/DocumentUpload.tsx`
- `Admision_MTN_front/services/documentService.ts`
- `Admision_MTN_backend/mock-application-service.js` (líneas 800-950)

#### 2.4 Envío de Postulación
- **Endpoint**: `POST /api/applications`
- **Servicio**: Application Service (puerto 8083)
- **Proceso**:
  1. Validación completa de todos los datos
  2. Verificación de documentos requeridos
  3. Creación de registro en base de datos con estado `SUBMITTED`
  4. Generación de número de postulación único
  5. Generación de PDF de recibo
  6. Envío de email de confirmación
- **Respuesta Exitosa**:
  - ID de postulación
  - Número de postulación
  - Fecha de envío
  - Link de descarga de recibo PDF
- **Errores**:
  - `400`: Datos incompletos o inválidos
  - `409`: Postulación duplicada
  - `422`: Validación de negocio fallida

**Archivos de Implementación**:
- `Admision_MTN_front/services/applicationService.ts` (función `submitApplication`)
- `Admision_MTN_backend/mock-application-service.js` (líneas 300-450)

#### 2.5 Email de Confirmación
- **Trigger**: Postulación enviada exitosamente
- **Servicio**: Notification Service (puerto 8085)
- **Contenido**:
  - Número de postulación
  - Fecha de envío
  - Datos del estudiante
  - Link de descarga del recibo PDF
  - Próximos pasos
  - Contacto de soporte
- **Template**: Dinámico con año calculado (actual + 1)

**Archivos de Implementación**:
- `Admision_MTN_backend/mock-notification-service.js` (líneas 450-550)
- Template: Email con footer automático (copyright año actual)

---

### 3. Dashboard Familiar

#### 3.1 Acceso al Dashboard
- **Ruta**: `/dashboard/family`
- **Endpoint**: `GET /api/applications/my-applications`
- **Servicio**: Application Service (puerto 8083)
- **Autenticación**: Token JWT con rol `APODERADO`
- **Proceso**:
  1. Extracción de `userId` del token JWT
  2. Búsqueda de postulaciones del usuario
  3. Carga de datos relacionados (estudiante, documentos, entrevistas)
- **Datos Retornados**:
  - Lista de postulaciones
  - Estado actual de cada postulación
  - Documentos asociados
  - Entrevistas programadas
  - Notificaciones pendientes

**Archivos de Implementación**:
- `Admision_MTN_front/pages/FamilyDashboard.tsx`
- `Admision_MTN_front/services/applicationService.ts` (función `getMyApplications`)
- `Admision_MTN_backend/mock-application-service.js` (líneas 600-700)

#### 3.2 Visualización de Estado
Estados posibles:
- `SUBMITTED` (Enviada): Badge azul
- `UNDER_REVIEW` (En Revisión): Badge amarillo
- `INTERVIEW_SCHEDULED` (Entrevista Programada): Badge naranja
- `APPROVED` (Aceptada): Badge verde
- `WAITLIST` (Lista de Espera): Badge amarillo
- `REJECTED` (Rechazada): Badge rojo

**Componente**: `ApplicationStatusBadge.tsx`

#### 3.3 Visualización de Documentos
- **Endpoint**: `GET /api/applications/{id}/documents`
- **Servicio**: Application Service (puerto 8083)
- **Funcionalidades**:
  - Lista de documentos cargados
  - Icono según tipo de documento
  - Tamaño de archivo
  - Fecha de carga
  - Botón "Ver" (abre en nueva pestaña)
  - Botón "Descargar"
- **Ver Documento**: `GET /api/applications/documents/{id}/view?token={jwt}`
  - Parámetro `token` en query string para autenticación
  - Content-Disposition: `inline` (abre en navegador)
- **Descargar Documento**: `GET /api/applications/documents/{id}/download?token={jwt}`
  - Content-Disposition: `attachment` (descarga archivo)

**Archivos de Implementación**:
- `Admision_MTN_front/pages/FamilyDashboard.tsx` (líneas 200-350)
- `Admision_MTN_front/services/applicationService.ts` (función `getApplicationDocuments`)
- `Admision_MTN_backend/mock-application-service.js` (líneas 1000-1150)

#### 3.4 Calendario de Entrevistas
- **Endpoint**: `GET /api/interviews/application/{applicationId}`
- **Servicio**: Evaluation Service (puerto 8084)
- **Datos Mostrados**:
  - Fecha y hora de la entrevista
  - Nombre del entrevistador
  - Tipo de entrevista (Director, Psicólogo, Profesor)
  - Ubicación/sala
  - Estado (Pendiente, Confirmada, Completada, Cancelada)
- **Estados Vacíos**:
  - Si no hay entrevistas: "No hay entrevistas programadas"
  - Mensaje informativo sobre próximos pasos

**Archivos de Implementación**:
- `Admision_MTN_front/pages/FamilyDashboard.tsx` (líneas 400-550)
- `Admision_MTN_front/services/interviewService.ts`
- `Admision_MTN_backend/mock-evaluation-service.js` (líneas 500-600)

#### 3.5 Notificaciones
- **Endpoint**: `GET /api/notifications/user/{userId}`
- **Servicio**: Notification Service (puerto 8085)
- **Tipos de Notificaciones**:
  - Cambio de estado de postulación
  - Entrevista programada
  - Recordatorio de entrevista (24h antes)
  - Solicitud de documentos adicionales
  - Decisión de admisión
- **Visualización**:
  - Icono según tipo
  - Mensaje
  - Fecha/hora
  - Estado (leída/no leída)
  - Botón "Marcar como leída"

**Archivos de Implementación**:
- `Admision_MTN_front/components/NotificationCenter.tsx`
- `Admision_MTN_front/services/notificationService.ts`
- `Admision_MTN_backend/mock-notification-service.js` (líneas 300-400)

---

### 4. Proceso de Entrevista

#### 4.1 Invitación a Entrevista
- **Trigger**: Administrador programa entrevista
- **Notificación**: Email automático al apoderado
- **Contenido del Email**:
  - Fecha y hora de la entrevista
  - Nombre del entrevistador
  - Tipo de entrevista
  - Ubicación
  - Instrucciones
  - Link de confirmación
  - Contacto para reprogramar

#### 4.2 Visualización de Horarios Disponibles
- **Endpoint**: `GET /api/interviews/available-slots`
- **Servicio**: Evaluation Service (puerto 8084)
- **Parámetros**:
  - `evaluatorId`: ID del entrevistador
  - `date`: Fecha deseada
  - `duration`: Duración de la entrevista (minutos)
- **Validaciones**:
  - Horario dentro del schedule del entrevistador
  - No conflicto con otras entrevistas
  - Horario hábil (lunes a viernes, 8:00-18:00)
- **Respuesta**: Lista de slots disponibles con hora inicio y fin

**Archivos de Implementación**:
- `Admision_MTN_front/components/interview/InterviewScheduler.tsx`
- `Admision_MTN_backend/mock-evaluation-service.js` (líneas 700-850)

#### 4.3 Programación de Entrevista
- **Endpoint**: `POST /api/interviews`
- **Servicio**: Evaluation Service (puerto 8084)
- **Datos Requeridos**:
  - `applicationId`: ID de la postulación
  - `evaluatorId`: ID del entrevistador
  - `scheduledDate`: Fecha y hora (ISO 8601)
  - `duration`: Duración en minutos
  - `interviewType`: Tipo de entrevista
- **Validaciones**:
  - Slot disponible (no hay conflictos)
  - Horario dentro del schedule del entrevistador
  - Postulación en estado válido (`UNDER_REVIEW` o `INTERVIEW_SCHEDULED`)
  - Usuario tiene permisos para programar
- **Proceso**:
  1. Validación de disponibilidad
  2. Verificación de conflictos
  3. Creación de registro de entrevista
  4. Actualización de estado de postulación
  5. Envío de notificación al apoderado
  6. Envío de notificación al entrevistador
- **Errores**:
  - `400`: Datos inválidos
  - `409`: Conflicto de horario
  - `422`: Slot no disponible

**Archivos de Implementación**:
- `Admision_MTN_front/services/interviewService.ts` (función `scheduleInterview`)
- `Admision_MTN_backend/mock-evaluation-service.js` (líneas 900-1050)

#### 4.4 Confirmación de Entrevista
- **Trigger**: Entrevista programada exitosamente
- **Email al Apoderado**:
  - Confirmación de fecha y hora
  - Nombre y cargo del entrevistador
  - Ubicación/sala
  - Mapa o instrucciones de acceso
  - Documentos a llevar
  - Link para reprogramar (si aplica)
  - Contacto de emergencia
- **Email al Entrevistador**:
  - Datos del estudiante
  - Datos del apoderado
  - Hora y ubicación
  - Link a la postulación completa

#### 4.5 Recordatorio de Entrevista
- **Trigger**: 24 horas antes de la entrevista
- **Canales**: Email y/o SMS
- **Contenido**:
  - Recordatorio de fecha y hora
  - Nombre del entrevistador
  - Ubicación
  - Documentos a llevar
  - Contacto de emergencia

#### 4.6 Post-Entrevista
- **Trigger**: Entrevistador marca entrevista como completada
- **Proceso**:
  1. Actualización de estado de entrevista a `COMPLETED`
  2. Registro de evaluación
  3. Notificación al apoderado de completitud
  4. No se revelan resultados inmediatos

---

### 5. Decisión y Pasos Finales

#### 5.1 Actualización de Estado
- **Endpoint**: `PATCH /api/applications/{id}/status`
- **Servicio**: Application Service (puerto 8083)
- **Autorización**: Solo roles `ADMIN`, `COORDINATOR`, `CYCLE_DIRECTOR`
- **Estados Finales**:
  - `APPROVED`: Aceptado
  - `WAITLIST`: Lista de espera
  - `REJECTED`: Rechazado
- **Validaciones**:
  - Transición de estado válida
  - Todas las evaluaciones completadas
  - Decisión justificada con comentarios

**Archivos de Implementación**:
- `Admision_MTN_backend/mock-application-service.js` (líneas 1200-1300)

#### 5.2 Notificación de Decisión
- **Trigger**: Estado actualizado a decisión final
- **Servicio**: Notification Service (puerto 8085)
- **Canal**: Email con copia a SMS

**Decisión: APROBADO**
- Felicitación
- Instrucciones de matrícula
- Documentos adicionales requeridos
- Fechas importantes
- Link a portal de matrícula
- Contacto de soporte

**Decisión: LISTA DE ESPERA**
- Explicación del proceso de lista de espera
- Posición en la lista (si aplica)
- Plazo de espera estimado
- Acciones a tomar
- Contacto para consultas

**Decisión: RECHAZADO**
- Notificación respetuosa
- Razones generales (sin detalles confidenciales)
- Opciones alternativas
- Contacto para feedback (opcional)
- Información sobre re-postulación futura

**Archivos de Implementación**:
- `Admision_MTN_backend/mock-notification-service.js` (líneas 600-800)

#### 5.3 Visualización en Dashboard
- **Actualización Automática**: Dashboard se actualiza con nuevo estado
- **Visualización**:
  - Badge de estado actualizado
  - Mensaje de notificación destacado
  - Instrucciones de próximos pasos
  - Links a recursos relevantes

#### 5.4 Proceso de Matrícula (Si Aprobado)
- **Trigger**: Estado `APPROVED`
- **Acceso**: Link enviado por email
- **Proceso**:
  1. Confirmación de aceptación de cupo
  2. Carga de documentos de matrícula
  3. Firma de contratos digitales
  4. Pago de matrícula
  5. Confirmación final
- **Nota**: Este proceso puede estar en un sistema separado

---

## Mapa de Servicios y Endpoints

### User Service (Puerto 8082)
| Endpoint | Método | Descripción | Autenticación |
|----------|--------|-------------|---------------|
| `/api/email/verify-email` | POST | Verificar email con RUT | No |
| `/api/auth/register` | POST | Registrar nuevo apoderado | No |
| `/api/auth/login` | POST | Autenticar apoderado | No |
| `/api/users/me` | GET | Obtener perfil del usuario | Sí (JWT) |
| `/api/users/{id}` | GET | Obtener usuario por ID | Sí (JWT) |

### Application Service (Puerto 8083)
| Endpoint | Método | Descripción | Autenticación |
|----------|--------|-------------|---------------|
| `/api/applications` | POST | Crear postulación | Sí (APODERADO) |
| `/api/applications/{id}` | GET | Obtener postulación | Sí (APODERADO/ADMIN) |
| `/api/applications/{id}` | PUT | Actualizar postulación | Sí (APODERADO) |
| `/api/applications/{id}/status` | PATCH | Actualizar estado | Sí (ADMIN/COORDINATOR) |
| `/api/applications/my-applications` | GET | Obtener mis postulaciones | Sí (APODERADO) |
| `/api/applications/{id}/documents` | GET | Listar documentos | Sí (APODERADO/ADMIN) |
| `/api/applications/documents` | POST | Subir documento | Sí (APODERADO) |
| `/api/applications/documents/{id}/view` | GET | Ver documento inline | Sí (token en query) |
| `/api/applications/documents/{id}/download` | GET | Descargar documento | Sí (token en query) |
| `/api/applications/{id}/receipt` | GET | Generar PDF recibo | Sí (APODERADO/ADMIN) |

### Evaluation Service (Puerto 8084)
| Endpoint | Método | Descripción | Autenticación |
|----------|--------|-------------|---------------|
| `/api/interviews/available-slots` | GET | Horarios disponibles | Sí (APODERADO/ADMIN) |
| `/api/interviews/validate-slot` | POST | Validar horario | Sí (APODERADO/ADMIN) |
| `/api/interviews` | POST | Programar entrevista | Sí (ADMIN/COORDINATOR) |
| `/api/interviews/application/{id}` | GET | Entrevistas de postulación | Sí (APODERADO/ADMIN) |
| `/api/interviews/{id}` | PATCH | Actualizar entrevista | Sí (ADMIN/EVALUATOR) |

### Notification Service (Puerto 8085)
| Endpoint | Método | Descripción | Autenticación |
|----------|--------|-------------|---------------|
| `/api/notifications/user/{userId}` | GET | Notificaciones del usuario | Sí (APODERADO) |
| `/api/notifications/{id}/read` | PATCH | Marcar como leída | Sí (APODERADO) |
| `/api/email/send` | POST | Enviar email (interno) | Sí (SERVICE) |

### Dashboard Service (Puerto 8086)
| Endpoint | Método | Descripción | Autenticación |
|----------|--------|-------------|---------------|
| `/api/dashboard/family/{userId}` | GET | Datos del dashboard familiar | Sí (APODERADO) |

---

## Manejo de Errores

### Errores de Autenticación (401)
- **Causa**: Token JWT inválido, expirado o ausente
- **Manejo Frontend**:
  1. Interceptor de Axios captura 401
  2. Limpia localStorage
  3. Redirect a página de login
  4. Muestra mensaje: "Sesión expirada, por favor inicia sesión nuevamente"

**Implementación**: `Admision_MTN_front/services/api.ts` (interceptor de respuesta)

### Errores de Autorización (403)
- **Causa**: Usuario autenticado pero sin permisos para la acción
- **Manejo Frontend**:
  1. Muestra modal de error
  2. Mensaje: "No tienes permisos para realizar esta acción"
  3. Botón para volver al dashboard

### Errores de Validación (400, 422)
- **Causa**: Datos inválidos o incompletos
- **Manejo Frontend**:
  1. Mostrar errores inline en el formulario
  2. Highlight de campos con error
  3. Mensajes descriptivos por campo
  4. Prevenir envío hasta corregir

**Implementación**: `Admision_MTN_front/components/ui/ErrorModal.tsx`

### Errores de Conflicto (409)
- **Causas Comunes**:
  - Email duplicado en registro
  - RUT duplicado
  - Postulación duplicada
  - Conflicto de horario en entrevista
- **Manejo Frontend**:
  1. Mostrar modal explicativo
  2. Sugerir acción correctiva
  3. Opción de contactar soporte

### Errores de Servidor (500, 503)
- **Causa**: Error interno del servidor o servicio no disponible
- **Manejo Frontend**:
  1. Mostrar mensaje genérico
  2. Sugerir reintentar más tarde
  3. Botón "Reintentar"
  4. Link a soporte técnico
  5. Log del error en consola para debugging

### Errores de Gateway (504)
- **Causa**: Timeout en comunicación entre servicios
- **Manejo Frontend**:
  1. Mensaje: "El servidor está tardando más de lo esperado"
  2. Botón "Reintentar"
  3. Sugerencia de verificar conexión

**Circuit Breaker**: Implementado en NGINX con reintentos automáticos

---

## Validaciones de Datos

### RUT Chileno
- **Formato**: 12.345.678-9 o 12345678-9
- **Validación**: Dígito verificador válido usando algoritmo Modulo 11
- **Implementación**: `Admision_MTN_front/utils/rutValidator.ts`

### Email
- **Formato**: RFC 5322 estándar
- **Adicional**: Verificación de dominio válido
- **Implementación**: `Admision_MTN_front/utils/emailValidator.ts`

### Teléfono Chileno
- **Formatos Aceptados**:
  - +56 9 1234 5678
  - 912345678
  - 9 1234 5678
- **Validación**: 9 dígitos, comienza con 9

### Edad del Estudiante
- **Validación por Grado**:
  - Pre-Kinder: 4 años al 31 de marzo
  - Kinder: 5 años al 31 de marzo
  - 1° Básico: 6 años al 31 de marzo
  - etc.
- **Cálculo**: Automático desde fecha de nacimiento

### Documentos
- **Tipos Permitidos**:
  - PDF: application/pdf
  - Imágenes: image/jpeg, image/png
- **Tamaño Máximo**: 5MB por archivo
- **Nombre**: Sanitizado para evitar inyecciones

---

## Seguridad

### JWT (JSON Web Token)
- **Algoritmo**: HS512
- **Expiración**: 24 horas
- **Almacenamiento**: localStorage (`apoderado_token`)
- **Payload**:
  ```json
  {
    "userId": "123",
    "email": "apoderado@example.com",
    "role": "APODERADO",
    "iat": 1234567890,
    "exp": 1234654290
  }
  ```
- **Validación**: En cada request mediante middleware

### CORS (Cross-Origin Resource Sharing)
- **Configuración NGINX**: `local-gateway.conf`
- **Origen Permitido**: `http://localhost:5173` (frontend)
- **Headers Permitidos**:
  - Origin
  - X-Requested-With
  - Content-Type
  - Accept
  - Authorization
  - Cache-Control
  - x-correlation-id
  - x-request-time
  - x-timezone
  - x-client-type
  - x-client-version
- **Métodos Permitidos**: GET, POST, PUT, PATCH, DELETE, OPTIONS
- **Credentials**: Permitidas

### Protección de Endpoints
- **Autenticación**: Middleware JWT en todos los endpoints excepto:
  - `/api/email/verify-email`
  - `/api/auth/register`
  - `/api/auth/login`
- **Autorización**: Verificación de rol por endpoint
- **Rate Limiting**: Implementado en NGINX (max 100 requests/minuto)

### Prevención de Inyecciones
- **SQL Injection**: Queries parametrizadas con `pg` library
- **XSS**: Sanitización de inputs en frontend y backend
- **Path Traversal**: Validación de rutas de archivos
- **File Upload**: Validación de tipo MIME real (no solo extensión)

---

## Monitoreo y Logs

### Logging en Backend
- **Biblioteca**: Winston (Node.js)
- **Niveles**: error, warn, info, debug
- **Archivos de Log**:
  - `/tmp/user-service.log`
  - `/tmp/application-service.log`
  - `/tmp/evaluation-service.log`
  - `/tmp/notification-service.log`
  - `/tmp/dashboard-service.log`

### Logging en Frontend
- **Desarrollo**: `console.log` con emojis para fácil identificación
- **Producción**: Envío a servicio de monitoreo (ej. Sentry)

### Health Checks
- **Endpoint**: `/health` en cada servicio
- **Respuesta**:
  ```json
  {
    "status": "UP",
    "service": "application-service",
    "timestamp": "2025-10-02T10:30:00Z"
  }
  ```
- **Monitoreo**: Gateway verifica cada 30 segundos

### Gateway Status
- **Endpoint**: `GET /gateway/status`
- **Respuesta**:
  ```json
  {
    "status": "UP",
    "services": {
      "user-service": "UP",
      "application-service": "UP",
      "evaluation-service": "UP",
      "notification-service": "UP",
      "dashboard-service": "UP"
    },
    "timestamp": "2025-10-02T10:30:00Z"
  }
  ```

---

## Diagramas

Para visualizar el flujo completo del proceso del apoderado, consultar:
- **Diagrama Mermaid**: `apoderado-process-flow.mermaid`
- **Diagrama SVG**: `apoderado-process-flow.svg`
- **Diagrama PNG**: `apoderado-process-flow.png`

---

## Contacto y Soporte

- **Email de Soporte**: soporte@mtn.cl
- **Teléfono**: +56 2 1234 5678
- **Horario de Atención**: Lunes a Viernes, 9:00 - 18:00
- **Repositorio**: GitHub (privado)

---

## Versionamiento

- **Versión del Documento**: 1.0.0
- **Fecha de Creación**: 2025-10-02
- **Última Actualización**: 2025-10-02
- **Autor**: Sistema de Documentación Automática

---

## Referencias

- [CLAUDE.md](../../../CLAUDE.md) - Instrucciones generales del proyecto
- [MICROSERVICES_GUIDE.md](../../../MICROSERVICES_GUIDE.md) - Guía de microservicios
- [INTEGRATION_GUIDE.md](../../../INTEGRATION_GUIDE.md) - Guía de integración frontend-backend
- [API_CONSOLIDATION_STRATEGY.md](../../../API_CONSOLIDATION_STRATEGY.md) - Estrategia de consolidación de APIs
