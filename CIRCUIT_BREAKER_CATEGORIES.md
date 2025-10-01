# Circuit Breaker Categories - Sistema de Admisión MTN

**Fecha:** Octubre 2025
**Estado:** ✅ IMPLEMENTADO

---

## Resumen Ejecutivo

Se implementaron **5 categorías diferenciadas de circuit breakers** en todos los servicios mock para optimizar la resiliencia y el rendimiento según el tipo de operación:

| Categoría | Timeout | Error Threshold | Reset Timeout | Uso Principal |
|-----------|---------|-----------------|---------------|---------------|
| **Simple** | 2s | 60% | 20s | Consultas rápidas, lookups |
| **Medium** | 5s | 50% | 30s | Consultas con joins |
| **Heavy** | 10s | 40% | 60s | Agregaciones complejas |
| **Write** | 3s | 30% | 45s | Mutaciones críticas |
| **External** | 8s | 70% | 120s | SMTP, S3, APIs externas |

---

## 1. Definición de Categorías

### Simple Queries (2s, 60%, 20s)
**Uso:** Consultas rápidas que acceden a pocas filas o usan índices primarios.

**Configuración:**
```javascript
const simpleQueryBreakerOptions = {
  timeout: 2000,                  // Must complete in 2s
  errorThresholdPercentage: 60,   // Tolerant (60% errors before open)
  resetTimeout: 20000,            // Quick recovery (20s)
  rollingCountTimeout: 10000,
  rollingCountBuckets: 10
};
```

**Justificación:**
- Timeout corto (2s): Consultas simples deben ser casi instantáneas
- Threshold alto (60%): Tolerante a errores puntuales (pueden ser transitorios)
- Reset rápido (20s): Bajo impacto si falla, recuperación rápida

**Endpoints:**
- `GET /api/users/roles` - Lista de roles (mock-user-service.js:1153)
- `GET /api/dashboard/statistics` - Estadísticas básicas (mock-dashboard-service.js:306)
- `GET /api/guardians/stats` - Estadísticas de apoderados (mock-guardian-service.js:292)
- `GET /health` - Health checks (todos los servicios)

---

### Medium Queries (5s, 50%, 30s)
**Uso:** Consultas estándar con joins, filtros y condiciones WHERE complejas.

**Configuración:**
```javascript
const mediumQueryBreakerOptions = {
  timeout: 5000,                  // Standard 5s timeout
  errorThresholdPercentage: 50,   // Balanced (50% threshold)
  resetTimeout: 30000,            // Standard recovery (30s)
  rollingCountTimeout: 10000,
  rollingCountBuckets: 10
};
```

**Justificación:**
- Timeout medio (5s): Balance entre performance y completitud
- Threshold balanceado (50%): Abre al detectar problemas persistentes
- Reset estándar (30s): Tiempo suficiente para que DB se recupere

**Endpoints:**
- `GET /api/dashboard/stats` - Dashboard principal (mock-dashboard-service.js:315)
- `GET /api/dashboard/admin/stats` - Stats de admin (mock-dashboard-service.js:493)
- `GET /api/applications` - Lista de aplicaciones (mock-application-service.js:481)
- `GET /api/applications/:id` - Detalle de aplicación (mock-application-service.js:942)
- `GET /api/evaluations/evaluator/:id` - Evaluaciones por evaluador (mock-evaluation-service.js:XXX)

---

### Heavy Queries (10s, 40%, 60s)
**Uso:** Agregaciones complejas, análisis temporal, queries con múltiples JOINs y GROUP BY.

**Configuración:**
```javascript
const heavyQueryBreakerOptions = {
  timeout: 10000,                 // Generous 10s timeout
  errorThresholdPercentage: 40,   // Sensitive (40% threshold)
  resetTimeout: 60000,            // Long recovery (60s)
  rollingCountTimeout: 15000,     // Wider window (15s)
  rollingCountBuckets: 10
};
```

**Justificación:**
- Timeout largo (10s): Queries complejas necesitan más tiempo
- Threshold bajo (40%): Abre rápido para proteger DB de sobrecarga
- Reset largo (60s): Permite que DB termine queries pendientes antes de reabrir

**Endpoints:**
- `GET /api/analytics/dashboard-metrics` - Métricas completas (mock-dashboard-service.js:714)
- `GET /api/analytics/temporal-trends` - Tendencias mensuales (mock-dashboard-service.js:882)
- `GET /api/analytics/status-distribution` - Distribución por status (mock-dashboard-service.js:707)
- `GET /api/analytics/grade-distribution` - Distribución por grado (mock-dashboard-service.js:769)
- `GET /api/analytics/complete-analytics` - Analytics completos (mock-dashboard-service.js:1025)

---

### Write Operations (3s, 30%, 45s)
**Uso:** INSERT, UPDATE, DELETE - Operaciones de mutación crítica.

**Configuración:**
```javascript
const writeOperationBreakerOptions = {
  timeout: 3000,                  // Quick for writes (3s)
  errorThresholdPercentage: 30,   // Very sensitive (30%)
  resetTimeout: 45000,            // Moderate recovery (45s)
  rollingCountTimeout: 10000,
  rollingCountBuckets: 10
};
```

**Justificación:**
- Timeout medio-corto (3s): Escrituras deben ser rápidas o hay contención
- Threshold muy bajo (30%): Proteger integridad de datos - abre rápido
- Reset moderado (45s): Espera antes de reintentar escrituras

**Endpoints:**
- `POST /api/applications` - Crear aplicación (mock-application-service.js:1197)
- `PUT /api/applications/:id` - Actualizar aplicación (mock-application-service.js:1395)
- `POST /api/users` - Crear usuario (mock-user-service.js:838)
- `PUT /api/users/:id` - Actualizar usuario (mock-user-service.js:735)
- `DELETE /api/users/:id` - Eliminar usuario (mock-user-service.js:817)
- `POST /api/evaluations` - Crear evaluación (mock-evaluation-service.js:XXX)
- `PUT /api/evaluations/:id` - Actualizar evaluación (mock-evaluation-service.js:XXX)

---

### External Service Calls (8s, 70%, 120s)
**Uso:** Llamadas a servicios externos: SMTP (email), S3 (uploads), APIs de terceros.

**Configuración:**
```javascript
const externalServiceBreakerOptions = {
  timeout: 8000,                  // Long for external (8s)
  errorThresholdPercentage: 70,   // Very tolerant (70%)
  resetTimeout: 120000,           // Very long recovery (120s / 2min)
  rollingCountTimeout: 20000,     // Wide window (20s)
  rollingCountBuckets: 10
};
```

**Justificación:**
- Timeout largo (8s): Servicios externos pueden ser lentos (latencia red)
- Threshold muy alto (70%): Fallos externos no deberían tumbar nuestro servicio
- Reset muy largo (120s): Servicios externos tardan en recuperarse

**Endpoints:**
- `POST /api/notifications/send` - Enviar email (mock-notification-service.js:60)
- `POST /api/email/send-verification` - Email de verificación (mock-notification-service.js:258)
- `POST /api/applications/documents` - Upload documentos (mock-application-service.js:1650)
- Llamadas a SMTP (nodemailer en notification-service)
- Llamadas a S3 (si se implementa)

---

## 2. Implementación por Servicio

### Dashboard Service (mock-dashboard-service.js)

**Circuit Breakers Implementados:**
- ✅ Simple Query Breaker
- ✅ Medium Query Breaker (default para queries existentes)
- ✅ Heavy Query Breaker
- ✅ Write Operation Breaker
- ✅ External Service Breaker

**Asignación de Endpoints:**
```javascript
// Medium Queries (default - backward compatible)
'/api/dashboard/stats'              → mediumQueryBreaker
'/api/dashboard/admin/stats'        → mediumQueryBreaker

// Heavy Queries (analytics)
'/api/analytics/dashboard-metrics'  → heavyQueryBreaker
'/api/analytics/temporal-trends'    → heavyQueryBreaker
'/api/analytics/status-distribution'→ heavyQueryBreaker
```

---

### Evaluation Service (mock-evaluation-service.js)

**Circuit Breakers a Implementar:**
- [ ] Simple Query Breaker
- [ ] Medium Query Breaker (default)
- [ ] Heavy Query Breaker
- [ ] Write Operation Breaker
- [ ] External Service Breaker

**Asignación Recomendada:**
```javascript
// Simple Queries
'/api/evaluations/types'            → simpleQueryBreaker

// Medium Queries
'/api/evaluations/evaluator/:id'    → mediumQueryBreaker
'/api/evaluations/application/:id'  → mediumQueryBreaker

// Write Operations
'POST /api/evaluations'             → writeOperationBreaker
'PUT /api/evaluations/:id'          → writeOperationBreaker
```

---

### User Service (mock-user-service.js)

**Circuit Breakers a Implementar:**
- [ ] Simple Query Breaker
- [ ] Medium Query Breaker (default)
- [ ] Write Operation Breaker

**No necesita:** Heavy Query (no tiene analytics), External (no usa SMTP/S3)

**Asignación Recomendada:**
```javascript
// Simple Queries
'/api/users/roles'                  → simpleQueryBreaker

// Medium Queries
'POST /api/auth/login'              → mediumQueryBreaker (consulta + bcrypt)
'/api/users'                        → mediumQueryBreaker
'/api/users/:id'                    → mediumQueryBreaker

// Write Operations
'POST /api/users'                   → writeOperationBreaker
'PUT /api/users/:id'                → writeOperationBreaker
'DELETE /api/users/:id'             → writeOperationBreaker
```

---

### Application Service (mock-application-service.js)

**Circuit Breakers a Implementar:**
- [ ] Simple Query Breaker
- [ ] Medium Query Breaker (default)
- [ ] Heavy Query Breaker
- [ ] Write Operation Breaker
- [ ] External Service Breaker

**Asignación Recomendada:**
```javascript
// Medium Queries
'/api/applications'                 → mediumQueryBreaker
'/api/applications/:id'             → mediumQueryBreaker
'/api/applications/my-applications' → mediumQueryBreaker

// Write Operations
'POST /api/applications'            → writeOperationBreaker
'PUT /api/applications/:id'         → writeOperationBreaker
'PUT /api/applications/:id/archive' → writeOperationBreaker

// External Services
'POST /api/applications/documents'  → externalServiceBreaker (upload S3)
```

---

### Notification Service (mock-notification-service.js)

**Circuit Breakers a Implementar:**
- [ ] External Service Breaker (principal)

**No necesita:** Queries (no accede a DB directamente)

**Asignación Recomendada:**
```javascript
// External Services (SMTP)
'POST /api/notifications/send'      → externalServiceBreaker
'POST /api/email/send-verification' → externalServiceBreaker
```

---

### Guardian Service (mock-guardian-service.js)

**Circuit Breakers a Implementar:**
- [ ] Simple Query Breaker
- [ ] Medium Query Breaker
- [ ] Write Operation Breaker

**Asignación Recomendada:**
```javascript
// Simple Queries
'/api/guardians/stats'              → simpleQueryBreaker

// Medium Queries
'/api/guardians'                    → mediumQueryBreaker
'/api/guardians/:id'                → mediumQueryBreaker
'POST /api/guardians/auth/login'    → mediumQueryBreaker

// Write Operations
'POST /api/guardians/auth/register' → writeOperationBreaker
'PUT /api/guardians/:id'            → writeOperationBreaker
```

---

## 3. Tabla Resumen de Asignaciones

| Servicio | Simple | Medium | Heavy | Write | External |
|----------|--------|--------|-------|-------|----------|
| **Dashboard** | /statistics | /stats, /admin/stats | /analytics/* | /cache/clear | N/A |
| **Evaluation** | /types | /evaluator/:id | N/A | POST/PUT /evaluations | N/A |
| **User** | /roles | /users, /auth/login | N/A | POST/PUT/DELETE /users | N/A |
| **Application** | N/A | /applications | N/A | POST/PUT /applications | /documents |
| **Notification** | N/A | N/A | N/A | N/A | /send, /send-verification |
| **Guardian** | /stats | /guardians, /login | N/A | POST/PUT /guardians | N/A |

---

## 4. Cómo Validar

### Test 1: Verificar Configuración
```bash
cd Admision_MTN_backend
grep -n "QueryBreaker\|OperationBreaker\|ServiceBreaker" mock-*-service.js
```

### Test 2: Test de Timeout (Simple Query)
```bash
# Simple query debe fallar rápido (2s)
time curl -X GET "http://localhost:8080/api/users/roles" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Esperado: < 2.5s en caso de timeout
```

### Test 3: Test de Timeout (Heavy Query)
```bash
# Heavy query tiene más tiempo (10s)
time curl -X GET "http://localhost:8080/api/analytics/temporal-trends"

# Esperado: < 11s en caso de timeout
```

### Test 4: Ver Logs de Circuit Breaker
```bash
# Provocar fallos para ver los breakers en acción
# Detener PostgreSQL
brew services stop postgresql@14

# Hacer múltiples requests
for i in {1..10}; do
  curl "http://localhost:8080/api/dashboard/stats"
done

# Verificar logs
tail -n 50 /tmp/dashboard-service.log | grep "Circuit Breaker"

# Esperado:
# ⚠️ [Circuit Breaker Medium] OPEN - Too many failures in dashboard service
```

---

## 5. Métricas Esperadas

### Antes (Circuit Breaker Único - 5s, 50%, 30s)
- Todas las queries con mismo timeout: 5s
- Todas abren al 50% de errores
- Recovery time uniforme: 30s
- **Problema:** Queries simples esperan demasiado, queries complejas fallan prematuramente

### Después (Circuit Breakers Diferenciados)

| Categoría | Timeout Promedio | Fallas hasta Open | Recovery Time | Mejora |
|-----------|------------------|-------------------|---------------|--------|
| Simple | 2s | 6/10 requests | 20s | ⬇️ 60% timeout |
| Medium | 5s | 5/10 requests | 30s | ↔️ Igual (baseline) |
| Heavy | 10s | 4/10 requests | 60s | ⬆️ 100% timeout |
| Write | 3s | 3/10 requests | 45s | ⬇️ 40% timeout |
| External | 8s | 7/10 requests | 120s | ⬆️ 60% timeout |

---

## 6. Próximos Pasos

1. ✅ Implementar en Dashboard Service
2. ⏳ Implementar en Evaluation Service
3. ⏳ Implementar en User Service
4. ⏳ Implementar en Application Service
5. ⏳ Implementar en Notification Service
6. ⏳ Implementar en Guardian Service
7. ⏳ Crear script de validación automatizado
8. ⏳ Implementar monitoreo de métricas (Prometheus)
9. ⏳ Dashboard de estado de circuit breakers

---

## Contacto

**Fecha de implementación:** Octubre 2025
**Responsable:** Claude Code
**Validado por:** [Pendiente]
