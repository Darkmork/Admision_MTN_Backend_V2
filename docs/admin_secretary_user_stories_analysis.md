# Análisis de Historias de Usuario - Administrador de Admisión / Secretaría
## Sistema de Admisión MTN - Colegio Monte Tabor y Nazaret

**Documento de Análisis Técnico y de Producto**
**Fecha:** 2025-10-01
**Versión:** 1.0

---

## ÍNDICE

1. [Resumen Ejecutivo](#resumen-ejecutivo)
2. [Clasificación de Historias de Usuario](#clasificación-de-historias-de-usuario)
3. [Análisis Detallado por Historia](#análisis-detallado-por-historia)
4. [Matriz de Trazabilidad](#matriz-de-trazabilidad)
5. [Arquitectura Técnica](#arquitectura-técnica)
6. [Plan de Implementación por Fases](#plan-de-implementación-por-fases)
7. [Riesgos y Estrategias de Mitigación](#riesgos-y-estrategias-de-mitigación)
8. [Modelos de Datos Propuestos](#modelos-de-datos-propuestos)
9. [Próximos Pasos Inmediatos](#próximos-pasos-inmediatos)
10. [Métricas de Éxito y KPIs](#métricas-de-éxito-y-kpis)

---

## 1. RESUMEN EJECUTIVO

### Contexto del Sistema

El Sistema de Admisión MTN es una plataforma de gestión de postulaciones escolares que integra:
- **Frontend:** React 19 + TypeScript + Vite (puerto 5173)
- **Backend:** NGINX Gateway (puerto 8080) + Node.js mock services (puertos 8082-8086)
- **Base de Datos:** PostgreSQL "Admisión_MTN_DB" (37 tablas)
- **Autenticación:** JWT con HS512, RBAC con roles ADMIN, TEACHER, COORDINATOR, etc.

### Estado Actual de Implementación

**Completado (US-9):**
- ✅ Tabla `application_status_history` con auditoría completa
- ✅ Endpoint `PATCH /api/applications/:id/status` (cambio de estado)
- ✅ Endpoint `GET /api/applications/:id/status-history` (historial)
- ✅ Notificaciones automáticas por email en cambio de estado
- ✅ Validación de estados y transiciones permitidas
- ✅ Transaccionalidad con rollback en caso de error

**Disponible en el Sistema:**
- 37 tablas operativas (applications, students, evaluations, interviews, documents, etc.)
- Sistema de emails con templates dinámicos
- API Gateway NGINX configurado
- Frontend API client tipado para applications
- Roles RBAC configurados

### Alcance de las 10 Historias de Usuario

Las historias cubren las necesidades del **Administrador de Admisión/Secretaría** para:
1. Gestionar postulaciones de forma centralizada
2. Validar documentos
3. Filtrar y ordenar aplicaciones
4. Registrar observaciones internas
5. Generar reportes Excel/PDF
6. Recibir alertas de postulaciones incompletas
7. Exportar listas de entrevistas
8. Asignar entrevistas
9. **[COMPLETADO]** Cambiar estados manualmente
10. Reenviar notificaciones a apoderados

---

## 2. CLASIFICACIÓN DE HISTORIAS DE USUARIO

### Tabla de Clasificación

| ID | Historia | Prioridad | Complejidad | Estado Actual | Esfuerzo Estimado |
|----|----------|-----------|-------------|---------------|-------------------|
| **US-1** | Panel centralizado de postulaciones | **P0 - Crítico** | **L (Large)** | Not Started | 5 días |
| **US-2** | Validación de documentos | **P0 - Crítico** | **M (Medium)** | Not Started | 3 días |
| **US-3** | Filtros y ordenamiento | **P0 - Crítico** | **M (Medium)** | Not Started | 3 días |
| **US-4** | Observaciones internas | **P1 - Alta** | **S (Small)** | Not Started | 2 días |
| **US-5** | Reportes Excel/PDF | **P1 - Alta** | **L (Large)** | Not Started | 4 días |
| **US-6** | Alertas de postulaciones incompletas | **P2 - Media** | **M (Medium)** | Not Started | 3 días |
| **US-7** | Exportar listas de entrevistas | **P1 - Alta** | **M (Medium)** | Not Started | 2 días |
| **US-8** | Asignar entrevistas | **P0 - Crítico** | **L (Large)** | Not Started | 4 días |
| **US-9** | Cambiar estados manualmente | **P0 - Crítico** | **M (Medium)** | ✅ **Completed** | - |
| **US-10** | Reenviar notificaciones | **P2 - Media** | **S (Small)** | Not Started | 1 día |

### Leyenda de Prioridades

- **P0 (Crítico):** Esencial para MVP, bloquea funcionalidad core
- **P1 (Alta):** Importante para operación diaria, no bloquea MVP
- **P2 (Media):** Mejora experiencia, puede posponerse a fase 2
- **P3 (Baja):** Nice-to-have, optimización futura

### Leyenda de Complejidad

- **XS (Extra Small):** 1-2 horas
- **S (Small):** 1-2 días (16 horas)
- **M (Medium):** 3-4 días (24-32 horas)
- **L (Large):** 5-7 días (40-56 horas)
- **XL (Extra Large):** 1-2 semanas (80-160 horas)

---

## 3. ANÁLISIS DETALLADO POR HISTORIA

### US-1: Panel Centralizado de Postulaciones

**Historia de Usuario:**
```
Como encargado de admisión,
Quiero revisar las postulaciones recibidas en un panel centralizado,
Para gestionar el proceso de manera más eficiente.
```

#### Criterios de Aceptación (Gherkin)

```gherkin
Escenario: Visualización de panel de postulaciones con datos completos
  Dado que soy un usuario autenticado con rol ADMIN o COORDINATOR
  Y existen 25 postulaciones en el sistema
  Cuando accedo a la ruta /admin/applications
  Entonces veo una tabla con las siguientes columnas:
    | ID | Estudiante | RUT | Grado | Estado | Fecha Postulación | Acciones |
  Y los datos se cargan en menos de 2 segundos
  Y veo paginación con 10 postulaciones por página
  Y veo el total de postulaciones en el encabezado

Escenario: Acceso a detalle de postulación desde panel
  Dado que estoy en el panel de postulaciones
  Cuando hago clic en el botón "Ver Detalles" de una postulación
  Entonces soy redirigido a /admin/applications/:id
  Y veo todos los datos del estudiante
  Y veo todos los documentos adjuntos
  Y veo el historial de cambios de estado
  Y veo las evaluaciones realizadas
  Y veo las entrevistas programadas

Escenario: Error de permisos para usuarios no autorizados
  Dado que soy un usuario autenticado con rol APODERADO
  Cuando intento acceder a /admin/applications
  Entonces veo un mensaje de error "No tienes permisos para acceder a esta sección"
  Y soy redirigido a mi dashboard de apoderado

Escenario: Panel vacío sin postulaciones
  Dado que soy ADMIN y no hay postulaciones en el sistema
  Cuando accedo a /admin/applications
  Entonces veo el mensaje "No hay postulaciones registradas"
  Y veo un botón "Crear Postulación Manual" (para casos especiales)

Escenario: Búsqueda rápida en el panel
  Dado que estoy en el panel de postulaciones
  Cuando escribo "María González" en el campo de búsqueda
  Entonces veo solo las postulaciones que coinciden con ese nombre
  Y la búsqueda se actualiza en tiempo real (debounce 300ms)
  Y veo el contador "3 resultados encontrados"
```

#### Requerimientos No Funcionales (NFR)

- **Seguridad:**
  - Verificación JWT en cada request
  - RBAC: Solo ADMIN, COORDINATOR, CYCLE_DIRECTOR pueden acceder
  - Logging de accesos al panel (auditoría)

- **Privacidad (Ley 19.628):**
  - Datos personales cifrados en tránsito (HTTPS)
  - Minimización: Solo mostrar datos necesarios en la tabla
  - Registro de quién accede a datos sensibles (RUT, dirección)

- **Performance:**
  - Carga inicial < 2s (con 100 postulaciones)
  - Paginación server-side para > 50 registros
  - Caché de 5 minutos para estadísticas del panel

- **Auditabilidad:**
  - Log de cada acceso al panel (timestamp, user_id, IP)
  - Registro de exportaciones de datos

#### Dependencias Técnicas

**Servicios:**
- `application-service` (puerto 8083)
- `user-service` (puerto 8082)
- `dashboard-service` (puerto 8086)

**API Endpoints:**
- `GET /api/applications/public/all` (ya existe en applicationsClient)
- `GET /api/applications/statistics` (ya existe)
- `GET /api/applications/:id` (ya existe)

**Componentes Frontend (Nuevos):**
- `AdminApplicationsPanel.tsx` (componente principal)
- `ApplicationsTable.tsx` (tabla reutilizable)
- `ApplicationsSearch.tsx` (búsqueda rápida)
- `ApplicationsPagination.tsx` (paginación)

**Tablas de BD:**
- `applications` (existente)
- `students` (existente)
- `application_status_history` (existente)

#### Definition of Ready (DoR)

- [x] Endpoints de API `/api/applications/public/all` existen y funcionan
- [x] Roles RBAC definidos (ADMIN, COORDINATOR)
- [ ] UI/UX mockups aprobados por dirección
- [ ] Datos de prueba creados (mínimo 50 postulaciones)
- [ ] Reglas de negocio para búsqueda documentadas

#### Definition of Done (DoD)

- [ ] Componente `AdminApplicationsPanel` implementado con TypeScript
- [ ] Pruebas unitarias ≥90% coverage (Jest + React Testing Library)
- [ ] Pruebas E2E con Playwright (`admin-applications-panel.spec.ts`)
- [ ] RBAC validado (usuarios sin permisos rechazan)
- [ ] Performance validada (< 2s carga con 100 registros)
- [ ] Auditoría de accesos implementada
- [ ] Documentación de usuario actualizada
- [ ] Code review aprobado
- [ ] Deployed to staging

---

### US-2: Validación de Documentos

**Historia de Usuario:**
```
Como encargado de admisión,
Quiero validar documentos subidos por los apoderados,
Para asegurar que la información es correcta y oficial.
```

#### Criterios de Aceptación (Gherkin)

```gherkin
Escenario: Revisión de documento con aprobación
  Dado que soy ADMIN y accedo al detalle de una postulación
  Y veo un documento "Certificado de Nacimiento" con estado "UPLOADED"
  Cuando hago clic en "Revisar Documento"
  Y selecciono la opción "Aprobar"
  Y opcionalmente agrego la nota "Documento válido, firma legible"
  Y hago clic en "Confirmar Aprobación"
  Entonces el estado del documento cambia a "APPROVED"
  Y se registra mi usuario como revisor (reviewed_by)
  Y se registra la fecha de revisión (reviewed_at)
  Y el apoderado recibe un email "Documento aprobado"

Escenario: Rechazo de documento con motivo
  Dado que estoy revisando un documento "Certificado de Notas"
  Y detecto que la imagen está borrosa
  Cuando selecciono "Rechazar"
  Y escribo el motivo "Imagen ilegible. Por favor, sube una foto más clara."
  Y hago clic en "Confirmar Rechazo"
  Entonces el estado cambia a "REJECTED"
  Y se guarda el motivo en `rejection_reason`
  Y el apoderado recibe email "Documento rechazado" con instrucciones
  Y la postulación vuelve a estado "DOCUMENTS_REQUESTED"

Escenario: Validación de tipo de archivo permitido
  Dado que un apoderado sube un archivo .docx
  Cuando el sistema valida el tipo de archivo
  Entonces rechaza la subida
  Y muestra mensaje "Solo se permiten archivos PDF, JPG, PNG (máx 5MB)"

Escenario: Detección de documentos vencidos
  Dado que reviso un "Certificado Médico" con fecha de emisión > 6 meses
  Cuando el sistema valida la fecha de vencimiento
  Entonces marca el documento como "EXPIRED"
  Y muestra alerta "Este documento ha vencido. Requiere actualización."

Escenario: Historial de versiones de documentos
  Dado que un documento fue rechazado y el apoderado subió uno nuevo
  Cuando accedo al historial de documentos
  Entonces veo:
    | Versión | Archivo | Estado | Revisor | Fecha Revisión | Motivo Rechazo |
    | v2 | certificado_v2.pdf | PENDING | - | - | - |
    | v1 | certificado_v1.jpg | REJECTED | Admin User | 2025-09-28 | Imagen borrosa |
```

#### Requerimientos No Funcionales (NFR)

- **Seguridad:**
  - Verificación de tipo MIME real (no solo extensión)
  - Escaneo antivirus en uploads (integración futura con ClamAV)
  - URLs pre-firmadas para descarga de documentos (S3/storage)

- **Privacidad (Ley 19.628):**
  - Documentos almacenados cifrados (AES-256)
  - Registro de quién accede a cada documento
  - Retención limitada: documentos de postulaciones rechazadas eliminados después de 1 año

- **Performance:**
  - Previsualización de PDF < 1s
  - Carga de imagen < 500ms
  - Validación de tipo de archivo < 100ms

- **Auditabilidad:**
  - Log completo de aprobaciones/rechazos
  - Trazabilidad de quién revisó cada documento
  - Historial de versiones completo

#### Dependencias Técnicas

**Servicios:**
- `application-service` (puerto 8083) - manejo de documentos

**API Endpoints (Nuevos):**
```
PATCH /api/documents/:id/review
  Body: { action: 'APPROVE' | 'REJECT', note?: string, reason?: string }
  Response: { document, updated: true }

GET /api/documents/:id/versions
  Response: { versions: Document[] }

GET /api/documents/:id/download
  Response: Blob (archivo firmado, expira en 5 min)

PUT /api/documents/:id/replace
  Body: FormData { file, documentId }
  Response: { newDocument, previousVersion }
```

**Componentes Frontend (Nuevos):**
- `DocumentReviewPanel.tsx` - panel de revisión con preview
- `DocumentViewer.tsx` - visor de PDF/imágenes
- `DocumentVersionHistory.tsx` - historial de versiones
- `DocumentActions.tsx` - botones aprobar/rechazar

**Tablas de BD:**
- `documents` (existente) - con campos `review_status`, `reviewed_by`, `rejection_reason`
- Agregar columna `replaces_document_id` para versiones

#### Definition of Ready (DoR)

- [ ] Tabla `documents` actualizada con campos de revisión
- [ ] Storage configurado (local o S3) para archivos
- [ ] Tipos de documentos requeridos definidos por negocio
- [ ] Email templates creados (aprobación/rechazo)
- [ ] Mockups de UI aprobados

#### Definition of Done (DoD)

- [ ] Endpoint `PATCH /api/documents/:id/review` implementado
- [ ] Sistema de versiones de documentos funcional
- [ ] Componente `DocumentReviewPanel` con preview
- [ ] Notificaciones por email funcionando
- [ ] Pruebas unitarias ≥90% coverage
- [ ] E2E test: `document-review-flow.spec.ts`
- [ ] Validación de seguridad: tipos de archivo, tamaño
- [ ] Logs de auditoría implementados
- [ ] Code review + deployment

---

### US-3: Filtros y Ordenamiento de Postulaciones

**Historia de Usuario:**
```
Como encargado de admisión,
Quiero filtrar y ordenar postulaciones por estado, nivel o fecha,
Para priorizar revisiones y entrevistas.
```

#### Criterios de Aceptación (Gherkin)

```gherkin
Escenario: Filtrado por estado de postulación
  Dado que estoy en el panel de postulaciones
  Cuando selecciono el filtro "Estado: EN_REVISION"
  Entonces veo solo las postulaciones con status = 'UNDER_REVIEW'
  Y el contador muestra "15 postulaciones en revisión"
  Y la URL se actualiza a /admin/applications?status=UNDER_REVIEW
  Y puedo compartir esta URL filtrada con otros admins

Escenario: Filtrado por grado/nivel educativo
  Dado que accedo a los filtros avanzados
  Cuando selecciono "Grado: Kinder"
  Entonces veo solo postulaciones donde grade_applying = 'KINDER'
  Y veo subcategorías si aplica (Pre-kinder, Kinder)

Escenario: Filtrado por rango de fechas
  Dado que quiero ver postulaciones de la última semana
  Cuando selecciono "Fecha: Últimos 7 días"
  Entonces veo postulaciones donde created_at >= NOW() - 7 days
  Y puedo seleccionar un rango personalizado con datepicker

Escenario: Ordenamiento por fecha de postulación
  Dado que tengo postulaciones filtradas
  Cuando hago clic en el header "Fecha Postulación"
  Entonces las postulaciones se ordenan por submission_date DESC
  Y el ícono cambia a flecha hacia abajo (↓)
  Cuando hago clic nuevamente
  Entonces el orden cambia a ASC (más antiguas primero)

Escenario: Filtros combinados múltiples
  Dado que quiero ver casos urgentes
  Cuando aplico los filtros:
    - Estado: DOCUMENTS_REQUESTED
    - Grado: Primero Básico
    - Fecha: Hace más de 30 días
  Entonces veo solo postulaciones que cumplen TODAS las condiciones
  Y veo "3 postulaciones críticas (documentos pendientes >30 días)"

Escenario: Limpieza de filtros aplicados
  Dado que tengo múltiples filtros activos
  Cuando hago clic en "Limpiar Filtros"
  Entonces se eliminan todos los filtros
  Y vuelvo a ver todas las postulaciones
  Y la URL vuelve a /admin/applications

Escenario: Persistencia de filtros en sesión
  Dado que tengo filtros aplicados
  Cuando recargo la página
  Entonces los filtros se mantienen activos
  Y veo los mismos resultados filtrados
  (Los filtros se guardan en localStorage)

Escenario: Filtrado por categoría especial
  Dado que quiero priorizar casos especiales
  Cuando selecciono "Categoría: Hijo de Empleado"
  Entonces veo solo postulaciones donde is_employee_child = true
  Y puedo filtrar por: Hijo de Exalumno, Inclusión, Regular
```

#### Requerimientos No Funcionales (NFR)

- **Performance:**
  - Aplicación de filtros < 500ms (con 100 registros)
  - Debounce de 300ms en búsqueda de texto
  - Paginación server-side para >50 resultados
  - Índices en BD para columnas filtradas

- **Usabilidad:**
  - Filtros visibles y accesibles (no ocultos en menús)
  - Indicadores visuales de filtros activos (badges)
  - URLs compartibles con filtros aplicados

- **Auditabilidad:**
  - Log de filtros usados frecuentemente (analytics)
  - No se registra qué admin filtró qué (privacidad)

#### Dependencias Técnicas

**Servicios:**
- `application-service` (puerto 8083)

**API Endpoints (Actualizar existentes):**
```
GET /api/applications/public/all
  Query params:
    ?status=UNDER_REVIEW
    &gradeApplying=KINDER
    &submissionDateFrom=2025-09-01
    &submissionDateTo=2025-09-30
    &specialCategory=employee
    &sort=submissionDate
    &direction=desc
    &page=0
    &size=20
```

**Componentes Frontend (Nuevos):**
- `ApplicationFilters.tsx` - panel de filtros con checkboxes, selects
- `DateRangePicker.tsx` - selector de rango de fechas
- `ActiveFiltersBar.tsx` - chips mostrando filtros activos
- `FilterPresets.tsx` - filtros predefinidos ("Urgentes", "Pendientes >30 días")

**Índices de BD (Optimización):**
```sql
CREATE INDEX idx_applications_status ON applications(status);
CREATE INDEX idx_applications_grade_applying ON applications(grade_applying);
CREATE INDEX idx_applications_submission_date ON applications(submission_date);
CREATE INDEX idx_applications_special_category ON applications(is_employee_child, is_alumni_child, is_inclusion_student);
```

#### Definition of Ready (DoR)

- [x] API `/api/applications/public/all` acepta parámetros de query
- [ ] Índices de BD creados para performance
- [ ] Catálogo de filtros definido por negocio
- [ ] UX de filtros diseñado (sidebar vs top bar)

#### Definition of Done (DoD)

- [ ] Componente `ApplicationFilters` funcional con todos los filtros
- [ ] Query params reflejados en URL (compartible)
- [ ] Persistencia de filtros en localStorage
- [ ] Índices de BD creados y performance validada
- [ ] Pruebas unitarias ≥90% coverage
- [ ] E2E test: `application-filters.spec.ts`
- [ ] Accesibilidad: navegable con teclado (Tab, Enter)
- [ ] Code review + deployment

---

### US-4: Observaciones Internas sobre Postulantes

**Historia de Usuario:**
```
Como encargado de admisión,
Quiero registrar observaciones internas sobre cada postulante,
Para apoyar la toma de decisiones del comité.
```

#### Criterios de Aceptación (Gherkin)

```gherkin
Escenario: Agregar observación interna a postulación
  Dado que soy ADMIN y estoy viendo el detalle de una postulación
  Cuando hago clic en "Agregar Observación"
  Y escribo "Familia muy comprometida, padre exalumno 1995. Priorizar entrevista."
  Y hago clic en "Guardar Observación"
  Entonces la observación se guarda con mi usuario y timestamp
  Y aparece en la sección "Observaciones Internas" del detalle
  Y la observación NO es visible para el apoderado

Escenario: Edición de observación propia
  Dado que agregué una observación hace 5 minutos
  Cuando hago clic en "Editar"
  Y modifico el texto
  Y hago clic en "Actualizar"
  Entonces la observación se actualiza
  Y se registra un flag `edited: true` con timestamp de edición
  Y se muestra "Editado por Admin User el 2025-09-28 15:30"

Escenario: Restricción de edición de observaciones de otros
  Dado que veo una observación creada por otro admin
  Cuando intento editarla
  Entonces no veo el botón "Editar"
  Y solo veo "Eliminar" si soy COORDINATOR o superior

Escenario: Observación con categoría/etiqueta
  Dado que agrego una observación
  Cuando selecciono la categoría "Seguimiento Psicológico"
  Y escribo la observación
  Entonces se guarda con category = 'PSYCHOLOGICAL'
  Y puedo filtrar observaciones por categoría

Escenario: Historial completo de observaciones
  Dado que una postulación tiene 8 observaciones
  Cuando accedo a "Ver Historial de Observaciones"
  Entonces veo todas ordenadas por fecha DESC
  Y veo quién escribió cada una
  Y veo categorías codificadas por color
  Y puedo exportar el historial a PDF

Escenario: Observaciones visibles en comité de admisión
  Dado que un profesor del comité revisa la postulación
  Cuando accede al detalle
  Entonces ve todas las observaciones internas
  Y puede agregar sus propias observaciones
  Pero NO puede editar las de otros

Escenario: Alerta de observaciones críticas
  Dado que marco una observación como "Crítica"
  Cuando la guardo
  Entonces se notifica por email a COORDINATOR
  Y aparece con ícono de alerta (⚠️) en el detalle
```

#### Requerimientos No Funcionales (NFR)

- **Seguridad:**
  - Observaciones solo visibles para roles ADMIN, COORDINATOR, TEACHER, PSYCHOLOGIST
  - Nunca exponer observaciones en API pública
  - Auditoría de quién accede a observaciones críticas

- **Privacidad (Ley 19.628):**
  - Observaciones son datos internos, no parte del expediente del estudiante
  - Retención: Eliminar observaciones de postulaciones rechazadas después de 2 años
  - Anonimización en reportes analíticos

- **Auditabilidad:**
  - Log de creación, edición, eliminación de observaciones
  - Registro de quién accede a observaciones críticas

#### Dependencias Técnicas

**Servicios:**
- `application-service` (puerto 8083)

**API Endpoints (Nuevos):**
```
POST /api/applications/:id/observations
  Body: { content: string, category?: string, critical?: boolean }
  Response: { observation }

GET /api/applications/:id/observations
  Response: { observations: Observation[] }

PUT /api/observations/:id
  Body: { content: string }
  Response: { observation, edited: true }

DELETE /api/observations/:id
  Response: { deleted: true }
```

**Componentes Frontend (Nuevos):**
- `ObservationsPanel.tsx` - panel de observaciones en detalle
- `ObservationForm.tsx` - formulario para agregar/editar
- `ObservationCard.tsx` - tarjeta de observación individual
- `ObservationFilters.tsx` - filtros por categoría

**Tabla de BD (Nueva):**
```sql
CREATE TABLE application_observations (
  id SERIAL PRIMARY KEY,
  application_id INTEGER NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
  content TEXT NOT NULL,
  category VARCHAR(50),  -- GENERAL, PSYCHOLOGICAL, ACADEMIC, FINANCIAL, OTHER
  created_by INTEGER NOT NULL REFERENCES users(id),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP,
  edited BOOLEAN DEFAULT false,
  critical BOOLEAN DEFAULT false,
  visible_to_roles TEXT[] DEFAULT ARRAY['ADMIN', 'COORDINATOR', 'TEACHER', 'PSYCHOLOGIST']
);

CREATE INDEX idx_app_obs_application_id ON application_observations(application_id);
CREATE INDEX idx_app_obs_created_at ON application_observations(created_at);
CREATE INDEX idx_app_obs_category ON application_observations(category);
CREATE INDEX idx_app_obs_critical ON application_observations(critical) WHERE critical = true;
```

#### Definition of Ready (DoR)

- [ ] Tabla `application_observations` creada
- [ ] Catálogo de categorías de observaciones definido
- [ ] Permisos RBAC para observaciones documentados
- [ ] UI mockup aprobado

#### Definition of Done (DoD)

- [ ] Endpoints CRUD de observaciones implementados
- [ ] Componente `ObservationsPanel` funcional
- [ ] RBAC validado (apoderados no ven observaciones)
- [ ] Notificación email para observaciones críticas
- [ ] Pruebas unitarias ≥90% coverage
- [ ] E2E test: `application-observations.spec.ts`
- [ ] Logs de auditoría implementados
- [ ] Code review + deployment

---

### US-5: Reportes en Excel/PDF del Estado de Postulaciones

**Historia de Usuario:**
```
Como encargado de admisión,
Quiero emitir reportes en Excel/PDF con el estado de postulaciones,
Para compartir con dirección o profesores.
```

#### Criterios de Aceptación (Gherkin)

```gherkin
Escenario: Exportación de reporte a Excel con filtros aplicados
  Dado que tengo 50 postulaciones filtradas por "Estado: EN_REVISION"
  Cuando hago clic en "Exportar a Excel"
  Entonces se descarga un archivo "postulaciones_en_revision_2025-09-28.xlsx"
  Y el archivo contiene las columnas:
    | ID | Estudiante | RUT | Grado | Estado | Fecha Postulación | Observaciones |
  Y los datos están formateados (fechas en español, estados traducidos)
  Y el archivo tiene un header con logo del colegio y fecha de generación

Escenario: Generación de reporte PDF ejecutivo
  Dado que soy COORDINATOR
  Cuando selecciono "Generar Reporte Ejecutivo PDF"
  Y selecciono el rango de fechas "01/09/2025 - 30/09/2025"
  Entonces se genera un PDF "reporte_ejecutivo_septiembre_2025.pdf" con:
    - Resumen estadístico (total, aprobadas, rechazadas, en proceso)
    - Gráfico de barras: postulaciones por estado
    - Tabla detallada de postulaciones
    - Footer con firma digital del coordinador

Escenario: Reporte personalizado con columnas seleccionables
  Dado que accedo a "Reportes Personalizados"
  Cuando selecciono las columnas: [Estudiante, RUT, Email Apoderado, Teléfono, Estado]
  Y hago clic en "Generar Excel"
  Entonces se descarga un archivo solo con esas columnas
  Y puedo guardar esta configuración como "Reporte de Contacto"

Escenario: Reporte de postulaciones urgentes (SLA vencido)
  Dado que quiero ver casos atrasados
  Cuando selecciono "Reporte: Postulaciones Urgentes"
  Entonces el sistema genera un Excel con postulaciones donde:
    - Estado = 'DOCUMENTS_REQUESTED' Y días_transcurridos > 30
    - O Estado = 'UNDER_REVIEW' Y días_transcurridos > 15
  Y las filas críticas se marcan en rojo

Escenario: Programación de reportes automáticos
  Dado que soy ADMIN
  Cuando configuro "Reporte Semanal Automatizado"
  Y selecciono destinatarios [director@mtn.cl, coordinador@mtn.cl]
  Y programo envío "Todos los lunes a las 9:00 AM"
  Entonces cada lunes se genera y envía por email el reporte
  Y se registra en tabla `scheduled_reports`

Escenario: Exportación con datos sensibles omitidos
  Dado que genero un reporte para compartir externamente
  Cuando selecciono "Omitir Datos Sensibles"
  Entonces el reporte NO incluye: RUT completo (muestra solo últimos 3 dígitos), direcciones, teléfonos
  Y se agrega marca de agua "Confidencial - Solo para uso interno MTN"

Escenario: Límite de exportación para performance
  Dado que intento exportar 5,000 postulaciones
  Cuando hago clic en "Exportar a Excel"
  Entonces veo alerta "Reporte muy grande (5000 filas). ¿Desea generar en segundo plano?"
  Y si acepto, se envía email cuando el archivo esté listo
```

#### Requerimientos No Funcionales (NFR)

- **Performance:**
  - Exportación Excel < 3s para 500 registros
  - Generación PDF < 5s para 100 registros
  - Reportes >1000 filas: procesamiento asíncrono con email

- **Seguridad:**
  - URLs de descarga con firma temporal (expiran en 1 hora)
  - Marca de agua con usuario y timestamp en PDFs
  - Logs de quién descargó qué reporte

- **Privacidad (Ley 19.628):**
  - Opción de omitir datos sensibles (RUT, dirección)
  - Reportes con datos completos solo para ADMIN/COORDINATOR
  - Retención de reportes generados: 30 días, luego eliminación

- **Auditabilidad:**
  - Log de generación de reportes (usuario, timestamp, filtros aplicados)
  - Registro de reportes descargados

#### Dependencias Técnicas

**Servicios:**
- `application-service` (puerto 8083)
- `dashboard-service` (puerto 8086) - para reportes ejecutivos

**API Endpoints (Nuevos):**
```
POST /api/reports/applications/excel
  Body: { filters, columns, includeObservations, omitSensitiveData }
  Response: { downloadUrl, expiresAt }

POST /api/reports/applications/pdf
  Body: { filters, reportType: 'EXECUTIVE' | 'DETAILED', dateRange }
  Response: { downloadUrl, expiresAt }

GET /api/reports/templates
  Response: { templates: ReportTemplate[] }

POST /api/reports/schedule
  Body: { reportType, filters, recipients, schedule: cron }
  Response: { scheduledReport }
```

**Componentes Frontend (Nuevos):**
- `ReportsPanel.tsx` - panel de generación de reportes
- `ReportTemplatesSelector.tsx` - selector de plantillas
- `ReportColumnsConfig.tsx` - configurador de columnas
- `ScheduledReportsManager.tsx` - gestor de reportes programados

**Librerías Requeridas:**
- **Backend:** `exceljs` (Excel), `pdfkit` o `puppeteer` (PDF)
- **Frontend:** `file-saver` para descarga de archivos

**Tabla de BD (Nueva):**
```sql
CREATE TABLE report_templates (
  id SERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  report_type VARCHAR(50),  -- EXCEL, PDF, CSV
  columns JSONB,  -- Array de columnas a incluir
  filters JSONB,  -- Filtros predefinidos
  created_by INTEGER REFERENCES users(id),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  is_public BOOLEAN DEFAULT false
);

CREATE TABLE scheduled_reports (
  id SERIAL PRIMARY KEY,
  report_template_id INTEGER REFERENCES report_templates(id),
  schedule_cron VARCHAR(50),  -- '0 9 * * 1' = Lunes 9 AM
  recipients TEXT[],  -- Emails destinatarios
  last_run_at TIMESTAMP,
  next_run_at TIMESTAMP,
  is_active BOOLEAN DEFAULT true,
  created_by INTEGER REFERENCES users(id)
);

CREATE TABLE report_executions (
  id SERIAL PRIMARY KEY,
  report_template_id INTEGER REFERENCES report_templates(id),
  executed_by INTEGER REFERENCES users(id),
  executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  filters_applied JSONB,
  file_url TEXT,
  file_expires_at TIMESTAMP,
  download_count INTEGER DEFAULT 0
);
```

#### Definition of Ready (DoR)

- [ ] Librerías `exceljs` y `pdfkit/puppeteer` instaladas
- [ ] Tablas `report_templates`, `scheduled_reports`, `report_executions` creadas
- [ ] Plantillas de reportes definidas por negocio
- [ ] Storage configurado para archivos temporales
- [ ] Mockups de UI aprobados

#### Definition of Done (DoD)

- [ ] Endpoint `POST /api/reports/applications/excel` funcional
- [ ] Endpoint `POST /api/reports/applications/pdf` funcional
- [ ] Componente `ReportsPanel` con generación y descarga
- [ ] Reportes programados con cron jobs funcionando
- [ ] Marca de agua en PDFs implementada
- [ ] Pruebas unitarias ≥90% coverage
- [ ] E2E test: `report-generation.spec.ts`
- [ ] Logs de auditoría para reportes
- [ ] Code review + deployment

---

### US-6: Alertas de Postulaciones Incompletas

**Historia de Usuario:**
```
Como encargado de admisión,
Quiero recibir alertas de nuevas postulaciones incompletas,
Para hacer seguimiento proactivo.
```

#### Criterios de Aceptación (Gherkin)

```gherkin
Escenario: Alerta diaria de postulaciones incompletas
  Dado que hay 5 postulaciones con documentos faltantes
  Y es lunes 9:00 AM
  Cuando el sistema ejecuta el job de alertas
  Entonces se envía email a admision@mtn.cl con:
    - Asunto: "5 postulaciones con documentos pendientes"
    - Lista de postulaciones con estudiante, grado, documentos faltantes
    - Botón "Revisar Postulaciones" con link directo al panel filtrado

Escenario: Notificación en tiempo real de nueva postulación incompleta
  Dado que un apoderado crea una postulación pero no sube documentos
  Cuando la postulación queda en estado "INCOMPLETE"
  Entonces se crea una notificación interna para ADMIN
  Y aparece en el panel de notificaciones (campana 🔔)
  Y se incrementa el contador de alertas

Escenario: Escalamiento de postulaciones antiguas sin respuesta
  Dado que una postulación lleva >30 días con documentos pendientes
  Cuando el sistema detecta este caso
  Entonces se envía email a coordinador@mtn.cl (escalamiento)
  Y se marca la postulación como "CRITICAL" en el panel
  Y se registra el escalamiento en tabla `escalations`

Escenario: Configuración de umbrales de alerta
  Dado que soy ADMIN en "Configuración de Alertas"
  Cuando configuro:
    - Alerta de documentos pendientes: 7 días
    - Escalamiento crítico: 30 días
    - Email diario: Habilitado (9:00 AM)
  Entonces se guardan las preferencias
  Y el sistema usa estos umbrales para futuras alertas

Escenario: Silenciar alerta para postulación específica
  Dado que una postulación tiene documentos pendientes pero está en proceso manual
  Cuando hago clic en "Silenciar Alertas" para esa postulación
  Y selecciono "Silenciar por 15 días"
  Entonces no se generan alertas para esa postulación durante 15 días
  Y se registra la razón del silenciamiento

Escenario: Dashboard de alertas activas
  Dado que accedo a "Panel de Alertas"
  Cuando reviso las alertas activas
  Entonces veo:
    - Postulaciones incompletas (últimas 24h): 3
    - Postulaciones críticas (>30 días): 2
    - Documentos rechazados sin resubir: 5
  Y puedo hacer clic en cada alerta para ir directo al caso
```

#### Requerimientos No Funcionales (NFR)

- **Performance:**
  - Job de alertas diarias < 30s (con 1000 postulaciones)
  - Notificaciones en tiempo real < 500ms (WebSocket o Server-Sent Events)

- **Escalabilidad:**
  - Sistema de colas para procesamiento asíncrono (opcional: Bull/Redis)
  - Batching de emails para evitar sobrecarga de SMTP

- **Configurabilidad:**
  - Umbrales de alertas configurables por admin
  - Destinatarios de alertas configurables
  - Horarios de envío personalizables

#### Dependencias Técnicas

**Servicios:**
- `application-service` (puerto 8083)
- `notification-service` (puerto 8085)
- `dashboard-service` (puerto 8086)

**API Endpoints (Nuevos):**
```
GET /api/alerts/incomplete-applications
  Response: { applications: Application[], count: number }

POST /api/alerts/configure
  Body: { alertType, threshold, recipients, schedule }
  Response: { alertConfig }

POST /api/alerts/:id/snooze
  Body: { days: number, reason: string }
  Response: { snoozed: true, until: Date }

GET /api/alerts/dashboard
  Response: {
    incompleteToday: number,
    criticalApplications: Application[],
    escalatedCases: number
  }
```

**Componentes Frontend (Nuevos):**
- `AlertsPanel.tsx` - panel de alertas activas
- `AlertsConfiguration.tsx` - configuración de umbrales
- `NotificationBell.tsx` - campana de notificaciones en navbar
- `AlertCard.tsx` - tarjeta de alerta individual

**Job Scheduler (Backend):**
- Usar `node-cron` para jobs programados
- Job diario: `0 9 * * *` (9:00 AM)
- Job de escalamiento: `0 10 * * *` (10:00 AM)

**Tabla de BD (Nueva):**
```sql
CREATE TABLE alert_configurations (
  id SERIAL PRIMARY KEY,
  alert_type VARCHAR(50),  -- INCOMPLETE_DOCUMENTS, CRITICAL_TIMEOUT, REJECTED_DOCUMENT
  threshold_days INTEGER,
  recipients TEXT[],
  schedule_cron VARCHAR(50),
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE application_alerts (
  id SERIAL PRIMARY KEY,
  application_id INTEGER REFERENCES applications(id) ON DELETE CASCADE,
  alert_type VARCHAR(50),
  severity VARCHAR(20),  -- INFO, WARNING, CRITICAL
  message TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  resolved_at TIMESTAMP,
  resolved_by INTEGER REFERENCES users(id),
  snoozed_until TIMESTAMP,
  snooze_reason TEXT
);

CREATE INDEX idx_app_alerts_application_id ON application_alerts(application_id);
CREATE INDEX idx_app_alerts_resolved ON application_alerts(resolved_at) WHERE resolved_at IS NULL;
CREATE INDEX idx_app_alerts_snoozed ON application_alerts(snoozed_until) WHERE snoozed_until IS NOT NULL;
```

#### Definition of Ready (DoR)

- [ ] Tablas `alert_configurations`, `application_alerts` creadas
- [ ] `node-cron` instalado en backend
- [ ] Configuración de alertas por defecto definida
- [ ] Email templates para alertas creados

#### Definition of Done (DoD)

- [ ] Job diario de alertas funcional con `node-cron`
- [ ] Endpoint `GET /api/alerts/incomplete-applications` implementado
- [ ] Componente `AlertsPanel` en dashboard
- [ ] Notificaciones en navbar (campana) funcionales
- [ ] Sistema de snooze de alertas funcional
- [ ] Pruebas unitarias ≥90% coverage
- [ ] E2E test: `alerts-incomplete-applications.spec.ts`
- [ ] Logs de alertas enviadas
- [ ] Code review + deployment

---

### US-7: Exportar Listas de Postulantes a Entrevistas

**Historia de Usuario:**
```
Como encargado de admisión,
Quiero exportar listas de postulantes a entrevistas en formato Excel/PDF,
Para coordinar con profesores.
```

#### Criterios de Aceptación (Gherkin)

```gherkin
Escenario: Exportación de lista de entrevistas del día
  Dado que hay 10 entrevistas programadas para hoy
  Cuando selecciono "Exportar Entrevistas de Hoy" en formato Excel
  Entonces se descarga "entrevistas_2025-09-28.xlsx" con columnas:
    | Hora | Estudiante | RUT | Grado | Entrevistador | Sala | Estado |
  Y las filas están ordenadas por hora ascendente
  Y el archivo tiene header con logo y fecha

Escenario: Lista de entrevistas por entrevistador
  Dado que quiero generar listas individuales
  Cuando selecciono "Exportar por Entrevistador"
  Entonces se generan archivos separados:
    - entrevistas_prof_juan_perez.pdf (3 entrevistas)
    - entrevistas_psic_maria_lopez.pdf (5 entrevistas)
  Y cada archivo tiene solo las entrevistas de ese profesor

Escenario: Lista de entrevistas con datos de contacto
  Dado que necesito contactar a los apoderados
  Cuando genero "Lista de Contacto para Entrevistas"
  Entonces el Excel incluye:
    | Estudiante | Apoderado | Email | Teléfono | Fecha Entrevista | Confirmado |
  Y puedo usar esta lista para llamadas de recordatorio

Escenario: Generación de cronograma semanal PDF
  Dado que es viernes y quiero planificar la próxima semana
  Cuando selecciono "Cronograma Semanal PDF"
  Y selecciono rango "30/09/2025 - 06/10/2025"
  Entonces se genera PDF con calendario visual:
    - Día por día con horarios
    - Color-coded por entrevistador
    - Total de entrevistas por día en footer

Escenario: Exportación con filtros aplicados
  Dado que filtro entrevistas por "Grado: Kinder" y "Estado: CONFIRMED"
  Cuando hago clic en "Exportar Filtrados"
  Entonces solo se exportan las entrevistas que cumplen esos filtros
  Y el archivo indica "Filtrado por: Kinder, Confirmadas"

Escenario: Lista con códigos QR para check-in
  Dado que genero lista PDF para recepción
  Cuando activo "Incluir Códigos QR"
  Entonces cada fila tiene un código QR con:
    - ID de la entrevista
    - Nombre del estudiante
    - Hora programada
  Y el personal de recepción puede escanear para check-in
```

#### Requerimientos No Funcionales (NFR)

- **Performance:**
  - Exportación Excel < 2s para 100 entrevistas
  - Generación PDF cronograma < 3s

- **Usabilidad:**
  - Botones de exportación visibles en panel de entrevistas
  - Opción de envío directo por email a entrevistadores

#### Dependencias Técnicas

**Servicios:**
- `evaluation-service` (puerto 8084) - maneja entrevistas

**API Endpoints (Nuevos):**
```
POST /api/interviews/export/excel
  Body: { filters, includeContactInfo, groupByInterviewer }
  Response: { downloadUrl, expiresAt }

POST /api/interviews/export/pdf
  Body: { dateRange, viewType: 'LIST' | 'SCHEDULE', includeQR }
  Response: { downloadUrl, expiresAt }

GET /api/interviews/by-interviewer/:userId
  Response: { interviews: Interview[] }
```

**Componentes Frontend (Nuevos):**
- `InterviewsExportPanel.tsx` - panel de exportación
- `InterviewsListExport.tsx` - opciones de lista
- `InterviewsScheduleExport.tsx` - opciones de cronograma

**Librerías:**
- `qrcode` para generación de QR codes en PDF

#### Definition of Ready (DoR)

- [x] Tabla `interviews` existe y tiene datos
- [ ] Librería `qrcode` instalada
- [ ] Formatos de exportación aprobados

#### Definition of Done (DoD)

- [ ] Endpoint `POST /api/interviews/export/excel` funcional
- [ ] Endpoint `POST /api/interviews/export/pdf` funcional
- [ ] Generación de QR codes en PDF implementada
- [ ] Componente `InterviewsExportPanel` funcional
- [ ] Pruebas unitarias ≥90% coverage
- [ ] E2E test: `interviews-export.spec.ts`
- [ ] Code review + deployment

---

### US-8: Asignar Entrevistas a Postulantes

**Historia de Usuario:**
```
Como encargado de admisión,
Quiero poder asignar entrevistas a postulantes,
Para calendarizar el proceso de selección.
```

#### Criterios de Aceptación (Gherkin)

```gherkin
Escenario: Asignación manual de entrevista individual
  Dado que estoy viendo el detalle de una postulación con estado "UNDER_REVIEW"
  Cuando hago clic en "Asignar Entrevista"
  Y selecciono entrevistador "Juan Pérez (TEACHER)"
  Y selecciono fecha "05/10/2025" y hora "10:00"
  Y selecciono sala "Sala de Entrevistas 1"
  Y hago clic en "Confirmar Asignación"
  Entonces se crea la entrevista con estado "SCHEDULED"
  Y se envía email al apoderado con fecha/hora/instrucciones
  Y se envía email al entrevistador con datos del estudiante
  Y el estado de la postulación cambia a "INTERVIEW_SCHEDULED"

Escenario: Validación de disponibilidad del entrevistador
  Dado que intento asignar entrevista el 05/10/2025 a las 10:00
  Y el profesor Juan Pérez ya tiene una entrevista a esa hora
  Cuando hago clic en "Confirmar Asignación"
  Entonces veo error "El entrevistador no está disponible en ese horario"
  Y se muestra una lista de horarios disponibles del profesor
  Y puedo seleccionar un horario alternativo

Escenario: Detección de conflicto de salas
  Dado que asigno entrevista en "Sala 1" el 05/10/2025 a las 11:00
  Y esa sala ya está reservada en ese horario
  Cuando intento confirmar
  Entonces veo advertencia "Sala ocupada. Disponibles: Sala 2, Sala 3"
  Y puedo cambiar la sala o el horario

Escenario: Asignación masiva de entrevistas
  Dado que tengo 20 postulaciones pendientes de entrevista
  Cuando selecciono las 20 postulaciones en la tabla
  Y hago clic en "Asignar Entrevistas Masivas"
  Y selecciono rango de fechas "05/10/2025 - 10/10/2025"
  Y el sistema distribuye automáticamente:
    - Por disponibilidad de entrevistadores
    - Evitando conflictos de horarios
    - Distribuyendo equitativamente la carga
  Entonces se crean 20 entrevistas
  Y se envían notificaciones a apoderados y entrevistadores
  Y veo resumen "20 entrevistas asignadas exitosamente"

Escenario: Reasignación de entrevista cancelada
  Dado que una entrevista fue cancelada por el apoderado
  Cuando accedo a la postulación
  Y hago clic en "Reasignar Entrevista"
  Entonces veo opciones sugeridas de fecha/hora basadas en disponibilidad
  Y puedo confirmar nueva asignación
  Y se notifica al apoderado del nuevo horario

Escenario: Configuración de disponibilidad del entrevistador
  Dado que soy COORDINATOR
  Cuando accedo a "Configurar Disponibilidad de Entrevistadores"
  Y selecciono profesor Juan Pérez
  Entonces puedo configurar:
    - Días disponibles: Lunes a Viernes
    - Horarios: 09:00 - 13:00, 15:00 - 18:00
    - Duración de entrevista: 45 minutos
    - Descanso entre entrevistas: 15 minutos
  Y esta configuración se usa en asignación automática
```

#### Requerimientos No Funcionales (NFR)

- **Seguridad:**
  - Solo ADMIN, COORDINATOR pueden asignar entrevistas
  - Validación de permisos en cada asignación

- **Performance:**
  - Cálculo de disponibilidad < 500ms
  - Asignación masiva de 20 entrevistas < 5s

- **Auditabilidad:**
  - Log de asignaciones, reasignaciones, cancelaciones
  - Registro de conflictos detectados y resueltos

#### Dependencias Técnicas

**Servicios:**
- `evaluation-service` (puerto 8084)
- `notification-service` (puerto 8085)

**API Endpoints (Nuevos/Actualizar):**
```
POST /api/interviews
  Body: {
    applicationId,
    interviewerId,
    scheduledDate,
    scheduledTime,
    room,
    duration
  }
  Response: { interview, notifications: { guardian: sent, interviewer: sent } }

GET /api/interviewers/:id/availability
  Query: ?date=2025-10-05
  Response: { availableSlots: TimeSlot[] }

POST /api/interviews/bulk-assign
  Body: {
    applicationIds: number[],
    dateRange: { from, to },
    autoDistribute: true
  }
  Response: {
    assigned: Interview[],
    conflicts: Conflict[],
    summary
  }

PUT /api/interviews/:id/reassign
  Body: { newDate, newTime, reason }
  Response: { interview, notifications }
```

**Componentes Frontend (Nuevos):**
- `InterviewAssignmentModal.tsx` - modal de asignación individual
- `InterviewerAvailabilityCalendar.tsx` - calendario de disponibilidad
- `BulkInterviewAssignment.tsx` - asignación masiva
- `InterviewConflictResolver.tsx` - resolver conflictos

**Tabla de BD (Actualizar existente):**
```sql
-- Tabla interviews ya existe, agregar campos si faltan:
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS room VARCHAR(100);
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS duration_minutes INTEGER DEFAULT 45;

-- Tabla interviewer_schedules ya existe, validar estructura
```

#### Definition of Ready (DoR)

- [x] Tabla `interviews` existe
- [x] Tabla `interviewer_schedules` existe
- [ ] Algoritmo de detección de conflictos definido
- [ ] Email templates para asignación/reasignación creados

#### Definition of Done (DoD)

- [ ] Endpoint `POST /api/interviews` con validación de conflictos
- [ ] Endpoint `POST /api/interviews/bulk-assign` funcional
- [ ] Componente `InterviewAssignmentModal` con calendario
- [ ] Detección y resolución de conflictos implementada
- [ ] Notificaciones a apoderados y entrevistadores funcionando
- [ ] Pruebas unitarias ≥90% coverage
- [ ] E2E test: `interview-assignment.spec.ts`
- [ ] Logs de auditoría
- [ ] Code review + deployment

---

### US-9: Cambiar Estado de Postulaciones Manualmente ✅

**Historia de Usuario:**
```
Como encargado de admisión,
Quiero cambiar el estado de las postulaciones manualmente (pendiente, en revisión, aceptada, rechazada),
Para controlar el flujo de cada caso.
```

#### Estado de Implementación: ✅ **COMPLETADO**

**Backend Implementado:**
- ✅ Endpoint `PATCH /api/applications/:id/status`
  - Validación de estados permitidos
  - Transacciones con rollback
  - Prevención de cambios redundantes
  - Registro en `application_status_history`
  - Notificación automática por email al apoderado

- ✅ Endpoint `GET /api/applications/:id/status-history`
  - Historial completo de cambios
  - Join con `users` para datos del revisor

**Base de Datos:**
- ✅ Tabla `application_status_history` creada y funcional
  - Campos: `previous_status`, `new_status`, `changed_by`, `change_note`, `changed_at`
  - Índices para performance
  - Foreign keys para integridad referencial

**Características:**
- ✅ Auditoría completa de cambios
- ✅ Notificaciones automáticas
- ✅ Validación de transiciones
- ✅ Transaccionalidad garantizada

**Próximos Pasos (Frontend):**
- [ ] Crear componente `ApplicationStatusChanger.tsx`
- [ ] Integrar con API client
- [ ] Mostrar historial de cambios en UI
- [ ] Pruebas E2E

---

### US-10: Reenviar Notificaciones a Apoderados

**Historia de Usuario:**
```
Como encargado de admisión,
Quiero reenviar notificaciones automáticas a apoderados en caso de actualización del estado,
Para mantenerlos informados.
```

#### Criterios de Aceptación (Gherkin)

```gherkin
Escenario: Reenvío manual de notificación de cambio de estado
  Dado que una postulación cambió a estado "APPROVED" hace 2 días
  Y el apoderado reporta que no recibió el email
  Cuando accedo a "Historial de Notificaciones" en el detalle
  Y hago clic en "Reenviar" junto a la notificación "Estado Aprobado"
  Entonces se envía nuevamente el email al apoderado
  Y se registra en `email_events` como "RESENT"
  Y veo confirmación "Email reenviado a apoderado@example.com"

Escenario: Cambio de destinatario de notificación
  Dado que el apoderado cambió su email
  Cuando actualizo el email en su perfil
  Y hago clic en "Reenviar Última Notificación"
  Entonces el email se envía al nuevo email actualizado
  Y se registra el cambio de destinatario en log

Escenario: Reenvío masivo de notificaciones fallidas
  Dado que hay 10 notificaciones con estado "FAILED" (error SMTP)
  Cuando accedo a "Panel de Notificaciones Fallidas"
  Y selecciono las 10 notificaciones
  Y hago clic en "Reintentar Envío"
  Entonces el sistema reintenta enviar todas
  Y muestra resultado "8 enviadas, 2 fallidas (email inválido)"

Escenario: Historial de envíos de una notificación
  Dado que una notificación se reenvió 3 veces
  Cuando accedo al detalle de esa notificación
  Entonces veo:
    | Intento | Fecha | Destinatario | Estado | Error |
    | 1 | 2025-09-25 | old@email.com | FAILED | Invalid email |
    | 2 | 2025-09-26 | new@email.com | SENT | - |
    | 3 | 2025-09-28 | new@email.com | DELIVERED | - |

Escenario: Notificación con plantilla personalizada
  Dado que quiero enviar un recordatorio personalizado
  Cuando selecciono "Enviar Notificación Personalizada"
  Y escribo el mensaje "Recordatorio: Faltan documentos por subir antes del 30/09"
  Y hago clic en "Enviar"
  Entonces se envía el email con mi mensaje personalizado
  Y se mantiene el diseño de la plantilla del colegio
  Y se registra como notificación tipo "CUSTOM"
```

#### Requerimientos No Funcionales (NFR)

- **Confiabilidad:**
  - Sistema de reintentos automáticos (3 intentos con backoff exponencial)
  - Detección de emails inválidos antes de enviar

- **Auditabilidad:**
  - Log completo de todos los envíos (intentos, entregas, fallos)
  - Registro de quién solicitó el reenvío

#### Dependencias Técnicas

**Servicios:**
- `notification-service` (puerto 8085)

**API Endpoints (Nuevos):**
```
POST /api/notifications/:id/resend
  Body: { recipientEmail?: string }
  Response: { sent: true, deliveryStatus }

POST /api/notifications/bulk-resend
  Body: { notificationIds: number[] }
  Response: { results: { sent: number, failed: number, details } }

GET /api/notifications/:id/history
  Response: { attempts: NotificationAttempt[] }

POST /api/notifications/custom
  Body: { applicationId, recipientEmail, message, templateType }
  Response: { notification, sent: true }
```

**Componentes Frontend (Nuevos):**
- `NotificationsHistoryPanel.tsx` - historial de notificaciones
- `ResendNotificationButton.tsx` - botón de reenvío
- `FailedNotificationsManager.tsx` - gestor de fallos
- `CustomNotificationModal.tsx` - envío personalizado

**Tablas de BD (Usar existentes):**
- `email_notifications` (existente)
- `email_events` (existente)
- Agregar columna `resent_count INTEGER DEFAULT 0` a `email_notifications`

#### Definition of Ready (DoR)

- [x] Tabla `email_notifications` existe
- [x] Tabla `email_events` existe
- [ ] Sistema de reintentos configurado en SMTP
- [ ] Email templates personalizables creados

#### Definition of Done (DoD)

- [ ] Endpoint `POST /api/notifications/:id/resend` funcional
- [ ] Endpoint `POST /api/notifications/bulk-resend` funcional
- [ ] Componente `NotificationsHistoryPanel` funcional
- [ ] Sistema de reintentos automáticos implementado
- [ ] Pruebas unitarias ≥90% coverage
- [ ] E2E test: `notification-resend.spec.ts`
- [ ] Logs de auditoría
- [ ] Code review + deployment

---

## 4. MATRIZ DE TRAZABILIDAD

### Tabla de Trazabilidad Completa

| Epic | User Story | Frontend Component | Backend Endpoint | DB Table | Test File | Priority | Status |
|------|------------|-------------------|------------------|----------|-----------|----------|--------|
| **Admin Dashboard** | US-1: Panel centralizado | `AdminApplicationsPanel.tsx`<br>`ApplicationsTable.tsx`<br>`ApplicationsSearch.tsx` | `GET /api/applications/public/all`<br>`GET /api/applications/statistics` | `applications`<br>`students`<br>`application_status_history` | `admin-applications-panel.spec.ts` | P0 | Not Started |
| **Document Management** | US-2: Validación de documentos | `DocumentReviewPanel.tsx`<br>`DocumentViewer.tsx`<br>`DocumentVersionHistory.tsx` | `PATCH /api/documents/:id/review`<br>`GET /api/documents/:id/versions`<br>`GET /api/documents/:id/download` | `documents` | `document-review-flow.spec.ts` | P0 | Not Started |
| **Admin Dashboard** | US-3: Filtros y ordenamiento | `ApplicationFilters.tsx`<br>`DateRangePicker.tsx`<br>`ActiveFiltersBar.tsx` | `GET /api/applications/public/all?filters` | `applications` (índices) | `application-filters.spec.ts` | P0 | Not Started |
| **Application Management** | US-4: Observaciones internas | `ObservationsPanel.tsx`<br>`ObservationForm.tsx`<br>`ObservationCard.tsx` | `POST /api/applications/:id/observations`<br>`GET /api/applications/:id/observations`<br>`PUT /api/observations/:id` | `application_observations` | `application-observations.spec.ts` | P1 | Not Started |
| **Reports** | US-5: Reportes Excel/PDF | `ReportsPanel.tsx`<br>`ReportTemplatesSelector.tsx`<br>`ScheduledReportsManager.tsx` | `POST /api/reports/applications/excel`<br>`POST /api/reports/applications/pdf`<br>`POST /api/reports/schedule` | `report_templates`<br>`scheduled_reports`<br>`report_executions` | `report-generation.spec.ts` | P1 | Not Started |
| **Alerts** | US-6: Alertas incompletas | `AlertsPanel.tsx`<br>`AlertsConfiguration.tsx`<br>`NotificationBell.tsx` | `GET /api/alerts/incomplete-applications`<br>`POST /api/alerts/configure`<br>`POST /api/alerts/:id/snooze` | `alert_configurations`<br>`application_alerts` | `alerts-incomplete-applications.spec.ts` | P2 | Not Started |
| **Interviews** | US-7: Exportar listas entrevistas | `InterviewsExportPanel.tsx`<br>`InterviewsListExport.tsx`<br>`InterviewsScheduleExport.tsx` | `POST /api/interviews/export/excel`<br>`POST /api/interviews/export/pdf` | `interviews`<br>`interviewer_schedules` | `interviews-export.spec.ts` | P1 | Not Started |
| **Interviews** | US-8: Asignar entrevistas | `InterviewAssignmentModal.tsx`<br>`InterviewerAvailabilityCalendar.tsx`<br>`BulkInterviewAssignment.tsx` | `POST /api/interviews`<br>`GET /api/interviewers/:id/availability`<br>`POST /api/interviews/bulk-assign` | `interviews`<br>`interviewer_schedules` | `interview-assignment.spec.ts` | P0 | Not Started |
| **Application Management** | US-9: Cambiar estados ✅ | `ApplicationStatusChanger.tsx` (pendiente) | `PATCH /api/applications/:id/status` ✅<br>`GET /api/applications/:id/status-history` ✅ | `application_status_history` ✅ | `application-status-change.spec.ts` (pendiente) | P0 | **Backend Completed** |
| **Notifications** | US-10: Reenviar notificaciones | `NotificationsHistoryPanel.tsx`<br>`ResendNotificationButton.tsx`<br>`FailedNotificationsManager.tsx` | `POST /api/notifications/:id/resend`<br>`POST /api/notifications/bulk-resend`<br>`GET /api/notifications/:id/history` | `email_notifications`<br>`email_events` | `notification-resend.spec.ts` | P2 | Not Started |

### Resumen por Epic

| Epic | Total Stories | Completed | In Progress | Not Started | Priority Distribution |
|------|---------------|-----------|-------------|-------------|-----------------------|
| **Admin Dashboard** | 2 | 0 | 0 | 2 | P0: 2 |
| **Document Management** | 1 | 0 | 0 | 1 | P0: 1 |
| **Application Management** | 2 | 1 | 0 | 1 | P0: 1, P1: 1 |
| **Reports** | 1 | 0 | 0 | 1 | P1: 1 |
| **Alerts** | 1 | 0 | 0 | 1 | P2: 1 |
| **Interviews** | 2 | 0 | 0 | 2 | P0: 1, P1: 1 |
| **Notifications** | 1 | 0 | 0 | 1 | P2: 1 |
| **TOTAL** | **10** | **1** | **0** | **9** | P0: 5, P1: 3, P2: 2 |

---

## 5. ARQUITECTURA TÉCNICA

### Diagrama de Componentes

```
┌─────────────────────────────────────────────────────────────┐
│                    NGINX API Gateway                         │
│                    (Puerto 8080)                             │
└─────────────────────────────────────────────────────────────┘
                              │
          ┌───────────────────┼───────────────────┐
          │                   │                   │
          ▼                   ▼                   ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│ application-     │ │ notification-    │ │ evaluation-      │
│ service (8083)   │ │ service (8085)   │ │ service (8084)   │
│                  │ │                  │ │                  │
│ - Applications   │ │ - Email Sending  │ │ - Interviews     │
│ - Documents      │ │ - SMS (future)   │ │ - Evaluations    │
│ - Observations   │ │ - Templates      │ │ - Schedules      │
│ - Status History │ │ - Notifications  │ │ - Assignments    │
└──────────────────┘ └──────────────────┘ └──────────────────┘
          │                   │                   │
          └───────────────────┼───────────────────┘
                              ▼
                    ┌──────────────────┐
                    │   PostgreSQL DB  │
                    │ "Admisión_MTN_DB"│
                    │   (37 tables)    │
                    └──────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                  React 19 Frontend (5173)                    │
│                                                              │
│  Pages:                    Components:                       │
│  - /admin/applications     - AdminApplicationsPanel         │
│  - /admin/interviews       - DocumentReviewPanel            │
│  - /admin/reports          - InterviewAssignmentModal       │
│  - /admin/alerts           - ReportsPanel                   │
│                            - AlertsPanel                     │
│                                                              │
│  API Client: applications.client.ts (typed SDK)             │
└─────────────────────────────────────────────────────────────┘
```

### Stack Tecnológico

**Frontend:**
- React 19 + TypeScript
- Vite (build tool)
- Axios (HTTP client con interceptores JWT)
- React Router (navegación)
- React Testing Library + Playwright (testing)

**Backend:**
- Node.js (mock services)
- Express.js (framework)
- PostgreSQL (base de datos)
- JWT (HS512) para autenticación
- Nodemailer (SMTP para emails)

**Infraestructura:**
- NGINX (API Gateway)
- Docker (opcional, para microservicios)
- node-cron (scheduled jobs)

**Librerías Adicionales Requeridas:**
```json
{
  "backend": [
    "exceljs",          // Generación de Excel
    "pdfkit",           // Generación de PDF
    "qrcode",           // Códigos QR
    "node-cron",        // Jobs programados
    "multer"            // Upload de archivos
  ],
  "frontend": [
    "file-saver",       // Descarga de archivos
    "react-datepicker", // Selector de fechas
    "recharts"          // Gráficos en reportes
  ]
}
```

### Principios de Arquitectura

1. **Separación de Responsabilidades:**
   - `application-service`: CRUD de postulaciones, documentos, observaciones
   - `notification-service`: Comunicaciones (email, SMS futuro)
   - `evaluation-service`: Entrevistas, evaluaciones, calendarios
   - `dashboard-service`: Estadísticas, reportes, analytics

2. **API Gateway Pattern:**
   - Punto único de entrada (NGINX puerto 8080)
   - Enrutamiento centralizado
   - CORS configurado globalmente
   - Rate limiting (futuro)

3. **Security:**
   - JWT obligatorio en todos los endpoints
   - RBAC en cada operación sensible
   - HTTPS en producción
   - Datos sensibles cifrados en BD

4. **Compliance (Ley 19.628):**
   - Minimización de datos en APIs
   - Logs de acceso a datos personales
   - Retención limitada de datos
   - Consentimiento explícito registrado

---

## 6. PLAN DE IMPLEMENTACIÓN POR FASES

### FASE 1: MVP - Funcionalidad Core (3-4 semanas)

**Objetivo:** Habilitar gestión básica de postulaciones y entrevistas

**Sprint 1 (Semana 1-2): Admin Dashboard Core**
- ✅ US-9: Cambiar estados manualmente (COMPLETADO)
- 🔄 US-1: Panel centralizado de postulaciones (5 días)
  - Componente `AdminApplicationsPanel`
  - Tabla con búsqueda y paginación
  - Integración con API existente
- 🔄 US-3: Filtros y ordenamiento (3 días)
  - Componente `ApplicationFilters`
  - Persistencia en localStorage
  - Índices de BD

**Sprint 2 (Semana 3-4): Entrevistas y Documentos**
- 🔄 US-8: Asignar entrevistas (4 días)
  - Modal de asignación
  - Detección de conflictos
  - Notificaciones automáticas
- 🔄 US-2: Validación de documentos (3 días)
  - Panel de revisión
  - Aprobación/rechazo con motivo
  - Historial de versiones

**Entregables Fase 1:**
- Panel administrativo funcional con listado, filtros, búsqueda
- Sistema de cambio de estados con auditoría
- Asignación de entrevistas con validación
- Revisión básica de documentos

---

### FASE 2: Features Avanzadas (2-3 semanas)

**Sprint 3 (Semana 5-6): Observaciones y Reportes**
- 🔄 US-4: Observaciones internas (2 días)
  - Panel de observaciones
  - Categorización
  - Permisos RBAC
- 🔄 US-5: Reportes Excel/PDF (4 días)
  - Exportación Excel básica
  - PDF con estadísticas
  - Plantillas personalizables

**Sprint 4 (Semana 7): Exportación de Entrevistas**
- 🔄 US-7: Exportar listas de entrevistas (2 días)
  - Excel por entrevistador
  - PDF con cronograma
  - Códigos QR para check-in

**Entregables Fase 2:**
- Sistema de observaciones internas completo
- Generación de reportes ejecutivos
- Exportaciones de entrevistas personalizadas

---

### FASE 3: Automatización y Optimización (1-2 semanas)

**Sprint 5 (Semana 8-9): Alertas y Notificaciones**
- 🔄 US-6: Alertas de postulaciones incompletas (3 días)
  - Jobs programados con node-cron
  - Panel de alertas
  - Sistema de snooze
- 🔄 US-10: Reenviar notificaciones (1 día)
  - Historial de envíos
  - Reenvío manual y masivo
  - Reintentos automáticos

**Entregables Fase 3:**
- Sistema de alertas automáticas
- Gestión de notificaciones fallidas
- Jobs programados operativos

---

### Timeline Visual

```
Semana 1-2  │ SPRINT 1: Admin Dashboard Core
            │ ✅ US-9 (Completado)
            │ 🔄 US-1: Panel centralizado (5d)
            │ 🔄 US-3: Filtros (3d)
            │
Semana 3-4  │ SPRINT 2: Entrevistas y Documentos
            │ 🔄 US-8: Asignar entrevistas (4d)
            │ 🔄 US-2: Validación documentos (3d)
            │
Semana 5-6  │ SPRINT 3: Observaciones y Reportes
            │ 🔄 US-4: Observaciones (2d)
            │ 🔄 US-5: Reportes (4d)
            │
Semana 7    │ SPRINT 4: Exportación Entrevistas
            │ 🔄 US-7: Exportar listas (2d)
            │
Semana 8-9  │ SPRINT 5: Alertas y Notificaciones
            │ 🔄 US-6: Alertas (3d)
            │ 🔄 US-10: Reenviar notificaciones (1d)
            │
═══════════════════════════════════════════════════
TOTAL: 9 semanas (aprox 2.25 meses)
```

---

## 7. RIESGOS Y ESTRATEGIAS DE MITIGACIÓN

### Tabla de Riesgos

| # | Riesgo | Probabilidad | Impacto | Estrategia de Mitigación | Owner |
|---|--------|--------------|---------|--------------------------|-------|
| **R1** | **Performance degradado con >500 postulaciones** | Media | Alto | - Implementar índices de BD en columnas filtradas<br>- Paginación server-side obligatoria<br>- Caché de consultas frecuentes (5 min TTL)<br>- Load testing antes de producción | Backend Dev |
| **R2** | **Conflictos de concurrencia en asignación de entrevistas** | Alta | Alto | - Optimistic locking en tabla `interviews`<br>- Transacciones con nivel SERIALIZABLE<br>- Validación doble: cliente y servidor<br>- Mensajes claros de conflicto al usuario | Backend Dev |
| **R3** | **Fallos en generación de reportes PDF con muchos datos** | Media | Medio | - Límite de 500 registros por reporte<br>- Procesamiento asíncrono para reportes grandes<br>- Notificación por email cuando esté listo<br>- Monitoreo de memoria en servidor | Backend Dev |
| **R4** | **Incumplimiento Ley 19.628 en exportaciones** | Baja | Crítico | - Validación legal de todas las exportaciones<br>- Opción obligatoria "Omitir datos sensibles"<br>- Marca de agua con restricción de uso<br>- Auditoría de todas las descargas<br>- Capacitación al equipo sobre normativa | Legal + Dev |
| **R5** | **Jobs de alertas consumen muchos recursos** | Media | Medio | - Ejecutar fuera de horario pico (ej: 3 AM)<br>- Batching de emails (máx 50 por minuto)<br>- Circuit breaker si SMTP falla<br>- Monitoreo de duración de jobs | DevOps |
| **R6** | **Interfaz compleja para usuarios no técnicos** | Alta | Medio | - Sesiones de UX testing con admins reales<br>- Tooltips y ayuda contextual<br>- Wizards para flujos complejos<br>- Video tutoriales embebidos | UX + Frontend |
| **R7** | **Pérdida de datos en cambio de estado** | Baja | Crítico | - Transacciones obligatorias en todos los cambios<br>- Rollback automático en caso de error<br>- Backups diarios de BD<br>- Tabla de auditoría inmutable | Backend + DBA |
| **R8** | **Notificaciones bloqueadas por filtros anti-spam** | Media | Alto | - Configurar SPF, DKIM, DMARC en dominio<br>- Usar SMTP reputado (SendGrid/AWS SES)<br>- Validar emails antes de enviar<br>- Sistema de reintentos con diferentes IPs | DevOps + Backend |
| **R9** | **Dependencia de servicios externos (SMTP, Storage)** | Media | Alto | - Implementar circuit breakers<br>- Fallback a modo degradado<br>- Queue de emails para reintentos<br>- SLA con proveedores | Backend + DevOps |
| **R10** | **Escalabilidad limitada de mock services** | Alta | Medio | - Migración gradual a Spring Boot microservices<br>- Monitoreo de latencia y throughput<br>- Horizontal scaling con load balancer<br>- Plan de migración documentado | Arquitectura |

### Plan de Contingencia

**Escenario Crítico: Caída de Base de Datos**
1. NGINX detecta servicio no disponible → retorna 503
2. Frontend muestra mensaje "Sistema en mantenimiento"
3. Notificación automática a DevOps vía Slack/PagerDuty
4. Restauración desde backup más reciente (<4 horas)
5. Validación de integridad de datos
6. Reactivación gradual de servicios

**Escenario: SMTP Caído (notificaciones no se envían)**
1. Circuit breaker abre después de 5 fallos
2. Emails se encolan en tabla `email_queue`
3. Sistema muestra advertencia "Notificaciones temporalmente diferidas"
4. Job de reintento cada 15 minutos
5. Notificación a admins para contacto manual si crítico

---

## 8. MODELOS DE DATOS PROPUESTOS

### 8.1. Tabla: `application_observations`

```sql
-- Observaciones internas de admins sobre postulaciones
CREATE TABLE application_observations (
  id SERIAL PRIMARY KEY,
  application_id INTEGER NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
  content TEXT NOT NULL,
  category VARCHAR(50) DEFAULT 'GENERAL',
  -- GENERAL, PSYCHOLOGICAL, ACADEMIC, FINANCIAL, SPECIAL_NEEDS, OTHER

  created_by INTEGER NOT NULL REFERENCES users(id),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP,
  edited BOOLEAN DEFAULT false,

  critical BOOLEAN DEFAULT false,  -- Si requiere atención urgente
  visible_to_roles TEXT[] DEFAULT ARRAY['ADMIN', 'COORDINATOR', 'TEACHER', 'PSYCHOLOGIST'],

  -- Auditoría
  last_edited_by INTEGER REFERENCES users(id),
  last_edited_at TIMESTAMP
);

-- Índices
CREATE INDEX idx_app_obs_application_id ON application_observations(application_id);
CREATE INDEX idx_app_obs_created_at ON application_observations(created_at DESC);
CREATE INDEX idx_app_obs_category ON application_observations(category);
CREATE INDEX idx_app_obs_critical ON application_observations(critical) WHERE critical = true;

-- Comentarios
COMMENT ON TABLE application_observations IS 'Observaciones internas de admins (no visibles para apoderados)';
COMMENT ON COLUMN application_observations.critical IS 'Marca observaciones que requieren seguimiento urgente';
COMMENT ON COLUMN application_observations.visible_to_roles IS 'Roles que pueden ver esta observación (RBAC)';
```

### 8.2. Tabla: `report_templates`

```sql
-- Plantillas de reportes personalizables
CREATE TABLE report_templates (
  id SERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  report_type VARCHAR(50) NOT NULL,  -- EXCEL, PDF, CSV

  -- Configuración del reporte
  columns JSONB NOT NULL,  -- ["studentName", "rut", "gradeApplying", "status"]
  filters JSONB,           -- {"status": "UNDER_REVIEW", "gradeApplying": "KINDER"}
  sort_config JSONB,       -- {"column": "submissionDate", "direction": "DESC"}

  -- Opciones de privacidad
  include_sensitive_data BOOLEAN DEFAULT false,
  omit_fields TEXT[],      -- ["rut", "address", "phone"] si include_sensitive_data = false

  -- Metadatos
  created_by INTEGER NOT NULL REFERENCES users(id),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP,
  is_public BOOLEAN DEFAULT false,  -- Si está disponible para todos los admins

  -- Estadísticas de uso
  usage_count INTEGER DEFAULT 0
);

CREATE INDEX idx_report_templates_type ON report_templates(report_type);
CREATE INDEX idx_report_templates_created_by ON report_templates(created_by);
CREATE INDEX idx_report_templates_public ON report_templates(is_public) WHERE is_public = true;

COMMENT ON TABLE report_templates IS 'Plantillas reutilizables de reportes configuradas por admins';
```

### 8.3. Tabla: `scheduled_reports`

```sql
-- Reportes programados (ejecución automática)
CREATE TABLE scheduled_reports (
  id SERIAL PRIMARY KEY,
  report_template_id INTEGER NOT NULL REFERENCES report_templates(id) ON DELETE CASCADE,

  -- Configuración de programación
  schedule_cron VARCHAR(50) NOT NULL,  -- Formato cron: '0 9 * * 1' = Lunes 9 AM
  timezone VARCHAR(50) DEFAULT 'America/Santiago',

  -- Destinatarios
  recipients TEXT[] NOT NULL,  -- Emails de destinatarios
  subject_template VARCHAR(200),
  body_template TEXT,

  -- Estado
  is_active BOOLEAN DEFAULT true,
  last_run_at TIMESTAMP,
  next_run_at TIMESTAMP,
  last_run_status VARCHAR(20),  -- SUCCESS, FAILED, PARTIAL
  failure_reason TEXT,

  -- Auditoría
  created_by INTEGER NOT NULL REFERENCES users(id),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE INDEX idx_scheduled_reports_next_run ON scheduled_reports(next_run_at) WHERE is_active = true;
CREATE INDEX idx_scheduled_reports_active ON scheduled_reports(is_active);

COMMENT ON TABLE scheduled_reports IS 'Reportes automáticos programados con cron';
COMMENT ON COLUMN scheduled_reports.schedule_cron IS 'Expresión cron para ejecución (ej: 0 9 * * 1)';
```

### 8.4. Tabla: `report_executions`

```sql
-- Log de ejecuciones de reportes
CREATE TABLE report_executions (
  id SERIAL PRIMARY KEY,
  report_template_id INTEGER REFERENCES report_templates(id),
  scheduled_report_id INTEGER REFERENCES scheduled_reports(id),

  -- Quién ejecutó (NULL si fue automático)
  executed_by INTEGER REFERENCES users(id),
  executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  -- Configuración aplicada
  filters_applied JSONB,
  columns_included TEXT[],
  total_records INTEGER,

  -- Archivo generado
  file_url TEXT,
  file_format VARCHAR(10),  -- xlsx, pdf, csv
  file_size_bytes BIGINT,
  file_expires_at TIMESTAMP,

  -- Seguridad
  download_count INTEGER DEFAULT 0,
  sensitive_data_included BOOLEAN DEFAULT false,

  -- Auditoría de descargas
  last_downloaded_at TIMESTAMP,
  last_downloaded_by INTEGER REFERENCES users(id)
);

CREATE INDEX idx_report_exec_executed_at ON report_executions(executed_at DESC);
CREATE INDEX idx_report_exec_template ON report_executions(report_template_id);
CREATE INDEX idx_report_exec_scheduled ON report_executions(scheduled_report_id);
CREATE INDEX idx_report_exec_expires ON report_executions(file_expires_at) WHERE file_expires_at IS NOT NULL;

COMMENT ON TABLE report_executions IS 'Historial de todas las ejecuciones de reportes con auditoría';
COMMENT ON COLUMN report_executions.file_expires_at IS 'URLs firmadas expiran después de este timestamp';
```

### 8.5. Tabla: `alert_configurations`

```sql
-- Configuración de alertas del sistema
CREATE TABLE alert_configurations (
  id SERIAL PRIMARY KEY,
  alert_type VARCHAR(50) NOT NULL UNIQUE,
  -- INCOMPLETE_DOCUMENTS, CRITICAL_TIMEOUT, REJECTED_DOCUMENT, INTERVIEW_REMINDER

  -- Umbrales
  threshold_days INTEGER,
  threshold_count INTEGER,

  -- Destinatarios y notificación
  recipients TEXT[] NOT NULL,
  notification_method VARCHAR(20) DEFAULT 'EMAIL',  -- EMAIL, SMS, INTERNAL
  schedule_cron VARCHAR(50),  -- NULL = solo en tiempo real

  -- Plantilla de mensaje
  subject_template VARCHAR(200),
  body_template TEXT,

  -- Estado
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP,
  last_triggered_at TIMESTAMP
);

CREATE INDEX idx_alert_config_type ON alert_configurations(alert_type);
CREATE INDEX idx_alert_config_active ON alert_configurations(is_active) WHERE is_active = true;

COMMENT ON TABLE alert_configurations IS 'Configuración global de alertas del sistema';
COMMENT ON COLUMN alert_configurations.threshold_days IS 'Días de umbral para activar alerta (ej: 30 días sin respuesta)';
```

### 8.6. Tabla: `application_alerts`

```sql
-- Alertas generadas para postulaciones
CREATE TABLE application_alerts (
  id SERIAL PRIMARY KEY,
  application_id INTEGER NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
  alert_type VARCHAR(50) NOT NULL,

  -- Contenido de la alerta
  severity VARCHAR(20) NOT NULL,  -- INFO, WARNING, CRITICAL
  message TEXT NOT NULL,
  details JSONB,  -- Información adicional estructurada

  -- Estado
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  resolved_at TIMESTAMP,
  resolved_by INTEGER REFERENCES users(id),
  resolution_note TEXT,

  -- Snooze (posponer)
  snoozed_until TIMESTAMP,
  snoozed_by INTEGER REFERENCES users(id),
  snooze_reason TEXT,

  -- Auditoría
  notification_sent BOOLEAN DEFAULT false,
  notification_sent_at TIMESTAMP
);

CREATE INDEX idx_app_alerts_application ON application_alerts(application_id);
CREATE INDEX idx_app_alerts_unresolved ON application_alerts(resolved_at) WHERE resolved_at IS NULL;
CREATE INDEX idx_app_alerts_snoozed ON application_alerts(snoozed_until) WHERE snoozed_until IS NOT NULL;
CREATE INDEX idx_app_alerts_severity ON application_alerts(severity);
CREATE INDEX idx_app_alerts_type ON application_alerts(alert_type);

COMMENT ON TABLE application_alerts IS 'Alertas activas y resueltas por postulación';
COMMENT ON COLUMN application_alerts.snoozed_until IS 'Fecha hasta la cual la alerta está silenciada';
```

### 8.7. Script de Migración Completo

```sql
-- Migration: Add admin/secretary user stories tables
-- Version: 1.0
-- Date: 2025-10-01

BEGIN;

-- 1. Observaciones internas
CREATE TABLE IF NOT EXISTS application_observations (
  id SERIAL PRIMARY KEY,
  application_id INTEGER NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
  content TEXT NOT NULL,
  category VARCHAR(50) DEFAULT 'GENERAL',
  created_by INTEGER NOT NULL REFERENCES users(id),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP,
  edited BOOLEAN DEFAULT false,
  critical BOOLEAN DEFAULT false,
  visible_to_roles TEXT[] DEFAULT ARRAY['ADMIN', 'COORDINATOR', 'TEACHER', 'PSYCHOLOGIST'],
  last_edited_by INTEGER REFERENCES users(id),
  last_edited_at TIMESTAMP
);

-- 2. Plantillas de reportes
CREATE TABLE IF NOT EXISTS report_templates (
  id SERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  report_type VARCHAR(50) NOT NULL,
  columns JSONB NOT NULL,
  filters JSONB,
  sort_config JSONB,
  include_sensitive_data BOOLEAN DEFAULT false,
  omit_fields TEXT[],
  created_by INTEGER NOT NULL REFERENCES users(id),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP,
  is_public BOOLEAN DEFAULT false,
  usage_count INTEGER DEFAULT 0
);

-- 3. Reportes programados
CREATE TABLE IF NOT EXISTS scheduled_reports (
  id SERIAL PRIMARY KEY,
  report_template_id INTEGER NOT NULL REFERENCES report_templates(id) ON DELETE CASCADE,
  schedule_cron VARCHAR(50) NOT NULL,
  timezone VARCHAR(50) DEFAULT 'America/Santiago',
  recipients TEXT[] NOT NULL,
  subject_template VARCHAR(200),
  body_template TEXT,
  is_active BOOLEAN DEFAULT true,
  last_run_at TIMESTAMP,
  next_run_at TIMESTAMP,
  last_run_status VARCHAR(20),
  failure_reason TEXT,
  created_by INTEGER NOT NULL REFERENCES users(id),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP
);

-- 4. Ejecuciones de reportes
CREATE TABLE IF NOT EXISTS report_executions (
  id SERIAL PRIMARY KEY,
  report_template_id INTEGER REFERENCES report_templates(id),
  scheduled_report_id INTEGER REFERENCES scheduled_reports(id),
  executed_by INTEGER REFERENCES users(id),
  executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  filters_applied JSONB,
  columns_included TEXT[],
  total_records INTEGER,
  file_url TEXT,
  file_format VARCHAR(10),
  file_size_bytes BIGINT,
  file_expires_at TIMESTAMP,
  download_count INTEGER DEFAULT 0,
  sensitive_data_included BOOLEAN DEFAULT false,
  last_downloaded_at TIMESTAMP,
  last_downloaded_by INTEGER REFERENCES users(id)
);

-- 5. Configuración de alertas
CREATE TABLE IF NOT EXISTS alert_configurations (
  id SERIAL PRIMARY KEY,
  alert_type VARCHAR(50) NOT NULL UNIQUE,
  threshold_days INTEGER,
  threshold_count INTEGER,
  recipients TEXT[] NOT NULL,
  notification_method VARCHAR(20) DEFAULT 'EMAIL',
  schedule_cron VARCHAR(50),
  subject_template VARCHAR(200),
  body_template TEXT,
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP,
  last_triggered_at TIMESTAMP
);

-- 6. Alertas de postulaciones
CREATE TABLE IF NOT EXISTS application_alerts (
  id SERIAL PRIMARY KEY,
  application_id INTEGER NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
  alert_type VARCHAR(50) NOT NULL,
  severity VARCHAR(20) NOT NULL,
  message TEXT NOT NULL,
  details JSONB,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  resolved_at TIMESTAMP,
  resolved_by INTEGER REFERENCES users(id),
  resolution_note TEXT,
  snoozed_until TIMESTAMP,
  snoozed_by INTEGER REFERENCES users(id),
  snooze_reason TEXT,
  notification_sent BOOLEAN DEFAULT false,
  notification_sent_at TIMESTAMP
);

-- Índices para performance
CREATE INDEX IF NOT EXISTS idx_app_obs_application_id ON application_observations(application_id);
CREATE INDEX IF NOT EXISTS idx_app_obs_created_at ON application_observations(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_app_obs_category ON application_observations(category);
CREATE INDEX IF NOT EXISTS idx_app_obs_critical ON application_observations(critical) WHERE critical = true;

CREATE INDEX IF NOT EXISTS idx_report_templates_type ON report_templates(report_type);
CREATE INDEX IF NOT EXISTS idx_report_templates_created_by ON report_templates(created_by);
CREATE INDEX IF NOT EXISTS idx_report_templates_public ON report_templates(is_public) WHERE is_public = true;

CREATE INDEX IF NOT EXISTS idx_scheduled_reports_next_run ON scheduled_reports(next_run_at) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_scheduled_reports_active ON scheduled_reports(is_active);

CREATE INDEX IF NOT EXISTS idx_report_exec_executed_at ON report_executions(executed_at DESC);
CREATE INDEX IF NOT EXISTS idx_report_exec_template ON report_executions(report_template_id);
CREATE INDEX IF NOT EXISTS idx_report_exec_expires ON report_executions(file_expires_at) WHERE file_expires_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_alert_config_type ON alert_configurations(alert_type);
CREATE INDEX IF NOT EXISTS idx_alert_config_active ON alert_configurations(is_active) WHERE is_active = true;

CREATE INDEX IF NOT EXISTS idx_app_alerts_application ON application_alerts(application_id);
CREATE INDEX IF NOT EXISTS idx_app_alerts_unresolved ON application_alerts(resolved_at) WHERE resolved_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_app_alerts_severity ON application_alerts(severity);

-- Datos iniciales de configuración de alertas
INSERT INTO alert_configurations (alert_type, threshold_days, recipients, schedule_cron, subject_template, body_template, is_active)
VALUES
  ('INCOMPLETE_DOCUMENTS', 7, ARRAY['admision@mtn.cl'], '0 9 * * *',
   'Postulaciones con documentos pendientes',
   'Hay {{count}} postulaciones con documentos pendientes desde hace más de {{threshold_days}} días.',
   true),
  ('CRITICAL_TIMEOUT', 30, ARRAY['coordinador@mtn.cl'], '0 10 * * *',
   'Postulaciones críticas - Escalamiento',
   'Se detectaron {{count}} postulaciones sin respuesta hace más de 30 días. Revisar urgentemente.',
   true)
ON CONFLICT (alert_type) DO NOTHING;

COMMIT;

-- Verificación
SELECT 'Migration completed successfully!' AS status;
SELECT COUNT(*) AS alert_configs FROM alert_configurations;
```

---

## 9. PRÓXIMOS PASOS INMEDIATOS

### Basado en el progreso actual (US-9 backend completado)

#### 1. **COMPLETAR US-9 FRONTEND** (1-2 días)
**Prioridad: ALTA**

**Tareas:**
- [ ] Crear componente `ApplicationStatusChanger.tsx`
  ```tsx
  interface Props {
    applicationId: number;
    currentStatus: string;
    onStatusChanged: () => void;
  }
  ```
- [ ] Integrar con endpoint `PATCH /api/applications/:id/status`
- [ ] Mostrar historial con `GET /api/applications/:id/status-history`
- [ ] Validación frontend de estados permitidos
- [ ] Confirmación modal antes de cambio crítico (REJECTED, ARCHIVED)
- [ ] Test E2E: `application-status-change.spec.ts`

**Entregable:** US-9 100% funcional (backend + frontend + tests)

---

#### 2. **IMPLEMENTAR US-1: PANEL CENTRALIZADO** (5 días)
**Prioridad: CRÍTICA (MVP)**

**Tareas:**
- [ ] **Día 1-2: Componente base**
  - Crear `AdminApplicationsPanel.tsx`
  - Integrar `applicationsClient.getPublicApplications()`
  - Tabla con columnas básicas
  - Paginación con react-paginate

- [ ] **Día 3: Búsqueda y navegación**
  - Componente `ApplicationsSearch.tsx` con debounce
  - Routing a `/admin/applications/:id` para detalle
  - Breadcrumbs de navegación

- [ ] **Día 4: Estadísticas y performance**
  - Widget de estadísticas en header (total, por estado)
  - Optimización de carga (lazy loading)
  - Loading skeletons

- [ ] **Día 5: Testing y refinamiento**
  - Tests unitarios con React Testing Library
  - E2E test con Playwright
  - Ajustes de UX

**Entregable:** Dashboard administrativo navegable con búsqueda

---

#### 3. **CREAR ÍNDICES DE BD PARA PERFORMANCE** (1 día)
**Prioridad: ALTA**

**Script SQL:**
```sql
-- Índices para US-1 (Panel) y US-3 (Filtros)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_applications_status
  ON applications(status);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_applications_grade_applying
  ON applications(grade_applying)
  WHERE grade_applying IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_applications_submission_date
  ON applications(submission_date DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_applications_created_at
  ON applications(created_at DESC);

-- Índice compuesto para filtros múltiples
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_applications_status_grade_date
  ON applications(status, grade_applying, submission_date);

-- Estadísticas actualizadas
ANALYZE applications;
```

**Ejecutar:** Antes de implementar filtros

---

#### 4. **IMPLEMENTAR US-3: FILTROS Y ORDENAMIENTO** (3 días)
**Prioridad: CRÍTICA (MVP)**

**Tareas:**
- [ ] **Día 1: Componente de filtros**
  - Crear `ApplicationFilters.tsx`
  - Filtros: Estado, Grado, Rango de fechas
  - Persistencia en `localStorage`

- [ ] **Día 2: Integración con API**
  - Actualizar `applicationsClient` para query params
  - URL compartible (filtros en query string)
  - Botón "Limpiar Filtros"

- [ ] **Día 3: Ordenamiento y testing**
  - Ordenamiento por columnas (click en headers)
  - Tests E2E de filtros combinados
  - Performance testing (100+ registros)

**Entregable:** Sistema de filtros completo y performante

---

#### 5. **PLANIFICACIÓN DE SPRINT 2** (Semana 3-4)
**Preparar documentación técnica:**

- [ ] Especificación detallada US-8 (Asignar entrevistas)
  - Algoritmo de detección de conflictos
  - Reglas de negocio para disponibilidad
  - Mockups de UI aprobados

- [ ] Especificación detallada US-2 (Validación documentos)
  - Tipos de documentos permitidos
  - Flujo de aprobación/rechazo
  - Email templates

- [ ] Setup de librerías adicionales:
  ```bash
  npm install exceljs pdfkit qrcode node-cron
  npm install --save-dev @types/pdfkit @types/qrcode
  ```

---

### Roadmap Visual (Próximas 2 Semanas)

```
Semana Actual (Días 1-5)
├── Día 1: ✅ US-9 Frontend - Componente base
├── Día 2: ✅ US-9 Frontend - Historial + Tests
├── Día 3: 🚀 US-1 - Panel base + Tabla
├── Día 4: 🚀 US-1 - Búsqueda + Navegación
└── Día 5: 🚀 US-1 - Estadísticas + Testing

Semana Siguiente (Días 6-10)
├── Día 6: 📊 Índices BD + Optimización
├── Día 7: 🔍 US-3 - Filtros componente
├── Día 8: 🔍 US-3 - Integración API
├── Día 9: 🔍 US-3 - Ordenamiento + Tests
└── Día 10: 📋 Planificación Sprint 2
```

---

### Checklist de Acción Inmediata

**Para el Desarrollador Frontend:**
- [ ] Crear branch `feature/us-9-frontend`
- [ ] Implementar `ApplicationStatusChanger.tsx`
- [ ] Agregar componente en página de detalle de aplicación
- [ ] Tests E2E con Playwright
- [ ] PR + Code Review

**Para el Desarrollador Backend:**
- [ ] Crear script de índices de BD
- [ ] Ejecutar en staging y validar performance
- [ ] Documentar queries optimizadas
- [ ] Preparar endpoints para US-3 (filtros)

**Para el Product Owner:**
- [ ] Validar mockups de US-1 (Panel)
- [ ] Definir prioridad de columnas en tabla
- [ ] Aprobar flujo de navegación
- [ ] Preparar casos de prueba de aceptación

**Para QA:**
- [ ] Preparar datos de prueba (50+ postulaciones)
- [ ] Crear matriz de pruebas para filtros
- [ ] Configurar entorno de staging
- [ ] Preparar scripts de Playwright

---

## 10. MÉTRICAS DE ÉXITO Y KPIs

### KPIs de Producto

| Métrica | Objetivo | Medición | Responsable |
|---------|----------|----------|-------------|
| **Tiempo de gestión de postulación** | Reducir de 15 min a 5 min | Tiempo promedio desde apertura hasta cierre de caso | Product Owner |
| **Adopción del panel de admin** | 100% de admins usando sistema | Usuarios activos diarios / Total admins | Product Manager |
| **Tasa de documentos aprobados al primer intento** | >80% | Documentos aprobados / Total subidos | QA Lead |
| **Alertas resueltas en < 48h** | >90% | Alertas resueltas en 2 días / Total alertas | Admin Team |
| **Reportes generados semanales** | >10 reportes/semana | Conteo de reportes ejecutados | Analytics |
| **Satisfacción del usuario admin** | NPS >50 | Encuesta trimestral | UX Team |

### KPIs Técnicos

| Métrica | Objetivo | Medición | Herramienta |
|---------|----------|----------|-------------|
| **Tiempo de carga del panel** | <2s (P95) | Latencia desde click hasta render completo | Lighthouse / New Relic |
| **Disponibilidad del sistema** | 99.5% uptime | Minutos de downtime / Total minutos | StatusPage |
| **Tasa de error en APIs** | <1% | Responses 5xx / Total requests | NGINX logs |
| **Cobertura de tests** | >90% | Lines covered / Total lines | Jest + Coverage |
| **Tiempo de generación de reportes** | <5s (100 registros) | Tiempo ejecución endpoint | APM |
| **Tasa de entrega de emails** | >95% | Emails delivered / Sent | SMTP analytics |

### Métricas de Compliance (Ley 19.628)

| Métrica | Objetivo | Medición | Responsable |
|---------|----------|----------|-------------|
| **Auditorías de acceso a datos sensibles** | 100% registradas | Logs de acceso a RUT, dirección, etc. | Security Team |
| **Retención de datos cumplida** | 100% compliance | Datos eliminados según política / Total datos expirados | DPO |
| **Reportes con datos sensibles marcados** | 100% | Reportes con watermark / Total reportes | Legal + Dev |
| **Consentimientos registrados** | 100% | Postulaciones con consentimiento / Total | Compliance |

### Dashboard de Métricas (Propuesto)

**Panel Ejecutivo (Vista COORDINATOR):**
- Gráfico de línea: Postulaciones creadas vs resueltas (últimos 30 días)
- KPI cards: Tiempo promedio de gestión, % documentos aprobados, alertas activas
- Top 5 cuellos de botella (estados con más tiempo promedio)

**Panel Técnico (Vista DevOps):**
- Gráfico de barras: Latencia por endpoint (P50, P95, P99)
- Timeline: Disponibilidad del sistema (últimas 24h)
- Alertas: Errores en logs, jobs fallidos, emails no entregados

**Herramientas Sugeridas:**
- **Grafana + Prometheus:** Métricas técnicas en tiempo real
- **Google Analytics / Mixpanel:** Eventos de usuario (clicks, navegación)
- **Sentry:** Error tracking y alertas
- **PostgreSQL queries:** Reportes de negocio (ejecutados desde dashboard-service)

---

## CONCLUSIÓN Y RECOMENDACIONES

### Resumen Ejecutivo

Se han analizado 10 historias de usuario para el rol **Administrador de Admisión/Secretaría**, con el siguiente balance:

- **✅ 1 Historia Completada (US-9):** Backend funcional para cambio de estados
- **🔄 9 Historias Pendientes:** Organizadas en 3 fases de implementación (9 semanas)
- **5 Historias P0 (Críticas):** Conforman el MVP funcional
- **37 Tablas de BD disponibles:** Infraestructura robusta ya existente
- **6 Tablas Nuevas Propuestas:** Para observaciones, reportes, alertas

### Decisiones Técnicas Clave

1. **Reutilización Máxima:** Aprovechar API client existente (`applicationsClient`), tabla `application_status_history`, sistema de emails
2. **Performance First:** Índices de BD desde día 1, paginación obligatoria >50 registros
3. **Compliance by Design:** Ley 19.628 integrada en cada feature (auditoría, retención, marcas de agua)
4. **Testability:** Cobertura ≥90%, E2E con Playwright para flujos críticos

### Riesgos Principales a Mitigar

1. **Performance con volumen:** Implementar índices y caché antes de escalar
2. **Complejidad UX:** Validar con usuarios reales en cada sprint
3. **Notificaciones fallidas:** Circuit breakers y queues desde el inicio

### Próximos Pasos Críticos (Esta Semana)

1. ✅ **Completar US-9 Frontend** (1-2 días)
2. 🚀 **Implementar US-1: Panel Centralizado** (3-5 días)
3. 📊 **Crear índices de BD** (1 día)
4. 📋 **Planificar Sprint 2** (US-8, US-2)

### Recomendación Final

**Priorizar MVP (Fase 1) para tener valor funcional en 4 semanas:**
- Panel centralizado con filtros (US-1, US-3)
- Gestión de estados completa (US-9 ✅)
- Asignación de entrevistas (US-8)
- Validación de documentos (US-2)

Con esto, el equipo administrativo podrá gestionar el 80% de sus operaciones diarias. Las fases 2 y 3 agregan optimizaciones y automatización.

---

**Documento aprobado para implementación.**

**Autor:** Product Backlog Architect - Sistema MTN
**Revisores:** Tech Lead, Product Owner, Legal Compliance
**Fecha de Última Actualización:** 2025-10-01
**Versión:** 1.0

---

## ANEXOS

### ANEXO A: Comandos Útiles

```bash
# Verificar estado de tablas propuestas
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" \
  -c "SELECT table_name FROM information_schema.tables
      WHERE table_schema = 'public'
      AND table_name LIKE '%observation%'
      OR table_name LIKE '%report%'
      OR table_name LIKE '%alert%';"

# Ejecutar migración de tablas
PGPASSWORD=admin123 psql -h localhost -U admin -d "Admisión_MTN_DB" \
  -f migration_admin_secretary_tables.sql

# Instalar dependencias backend
cd Admision_MTN_backend
npm install exceljs pdfkit qrcode node-cron multer

# Instalar dependencias frontend
cd Admision_MTN_front
npm install file-saver react-datepicker recharts
npm install --save-dev @types/file-saver

# Ejecutar tests E2E específicos
npm run e2e -- application-status-change.spec.ts
npm run e2e -- admin-applications-panel.spec.ts
```

### ANEXO B: Email Templates Requeridos

**Templates a crear en `email_templates`:**
1. `DOCUMENT_APPROVED` - Documento aprobado
2. `DOCUMENT_REJECTED` - Documento rechazado con motivo
3. `INTERVIEW_ASSIGNED` - Entrevista asignada (apoderado)
4. `INTERVIEW_ASSIGNED_INTERVIEWER` - Entrevista asignada (profesor)
5. `INTERVIEW_REASSIGNED` - Entrevista reasignada
6. `INCOMPLETE_DOCUMENTS_ALERT` - Alerta de documentos pendientes
7. `CRITICAL_TIMEOUT_ALERT` - Escalamiento por timeout
8. `CUSTOM_NOTIFICATION` - Notificación personalizada

### ANEXO C: Endpoints Summary

**Nuevos Endpoints a Implementar (Total: 25)**

| Servicio | Método | Endpoint | US |
|----------|--------|----------|-----|
| application-service | POST | `/api/applications/:id/observations` | US-4 |
| application-service | GET | `/api/applications/:id/observations` | US-4 |
| application-service | PUT | `/api/observations/:id` | US-4 |
| application-service | DELETE | `/api/observations/:id` | US-4 |
| application-service | PATCH | `/api/documents/:id/review` | US-2 |
| application-service | GET | `/api/documents/:id/versions` | US-2 |
| application-service | GET | `/api/documents/:id/download` | US-2 |
| dashboard-service | POST | `/api/reports/applications/excel` | US-5 |
| dashboard-service | POST | `/api/reports/applications/pdf` | US-5 |
| dashboard-service | GET | `/api/reports/templates` | US-5 |
| dashboard-service | POST | `/api/reports/schedule` | US-5 |
| dashboard-service | GET | `/api/alerts/incomplete-applications` | US-6 |
| dashboard-service | POST | `/api/alerts/configure` | US-6 |
| dashboard-service | POST | `/api/alerts/:id/snooze` | US-6 |
| dashboard-service | GET | `/api/alerts/dashboard` | US-6 |
| evaluation-service | POST | `/api/interviews/export/excel` | US-7 |
| evaluation-service | POST | `/api/interviews/export/pdf` | US-7 |
| evaluation-service | GET | `/api/interviewers/:id/availability` | US-8 |
| evaluation-service | POST | `/api/interviews/bulk-assign` | US-8 |
| evaluation-service | PUT | `/api/interviews/:id/reassign` | US-8 |
| notification-service | POST | `/api/notifications/:id/resend` | US-10 |
| notification-service | POST | `/api/notifications/bulk-resend` | US-10 |
| notification-service | GET | `/api/notifications/:id/history` | US-10 |
| notification-service | POST | `/api/notifications/custom` | US-10 |

---

**FIN DEL DOCUMENTO**
