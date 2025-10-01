# Runbook: SLO Error Rate Alto

**🔴 Criticidad: CRÍTICA**  
**📟 Canal: PagerDuty**  
**🎯 SLO: Error rate < 5%**

## 🚨 Descripción del Problema

El porcentaje de errores HTTP (4xx/5xx) ha superado el 5%, violando el SLO de disponibilidad del sistema. Esto indica fallas que afectan a los usuarios del proceso de admisión.

### Impacto en el Negocio
- **Familias**: No pueden completar postulaciones
- **Evaluadores**: No pueden acceder al sistema de evaluaciones
- **Administradores**: Pérdida de visibilidad y control
- **Reputación**: Impacto en imagen institucional

## 📊 Alertas Relacionadas

```yaml
alert: MTNErrorRateHigh
expr: service:http:error_rate > 5
for: 1m
severity: critical
runbook: https://runbooks.mtn.cl/slo-error-rate
```

## 🔍 Investigación Inicial (< 5 min)

### 1. Confirmar Error Rate Actual

```bash
# Error rate general
curl -s "http://prometheus.mtn.cl:9090/api/v1/query?query=service:http:error_rate" | jq -r '.data.result[] | "\(.metric.service): \(.value[1])%"'

# Breakdown por código HTTP
curl -s "http://prometheus.mtn.cl:9090/api/v1/query?query=rate(http_server_requests_total{status=~\"4..\|5..\"}[5m])*100" | jq '.'
```

### 2. Dashboard de Errores
👉 **[Grafana Golden Signals - Error Rate Panel](http://grafana.mtn.cl/d/mtn-golden-signals?viewPanel=3)**

**Métricas Clave:**
- Error rate por servicio
- Error rate por endpoint
- Distribución de códigos HTTP
- Request volume (para contexto)

### 3. Top Endpoints con Errores

```bash
# Endpoints con más errores
curl -s "http://prometheus.mtn.cl:9090/api/v1/query?query=topk(10,rate(http_server_requests_total{status=~\"4..|5..\"}[5m]))" | jq -r '.data.result[] | "\(.metric.uri): \(.value[1]) errors/sec (HTTP \(.metric.status))"'
```

## 🔬 Análisis por Tipo de Error (< 10 min)

### A. Errores 4xx (Client Errors)

#### Authentication/Authorization (401/403)
```bash
# Errores de autenticación
curl -s "http://prometheus.mtn.cl:9090/api/v1/query?query=rate(http_server_requests_total{status=~\"401|403\"}[5m])" | jq '.'

# Verificar JWT service
kubectl logs -n mtn-admission deployment/mtn-admission-backend --since=10m | grep -i "jwt\|token\|unauthorized"
```

**Posibles Causas:**
- JWT secret rotado sin redeploy
- Token expiración masiva
- Auth service caído

#### Bad Requests (400)
```bash
# Requests malformados
kubectl logs -n mtn-admission deployment/mtn-admission-backend --since=10m | grep -i "400\|bad.*request\|validation"
```

**Posibles Causas:**
- Frontend enviando datos corruptos
- Cambio de API sin backward compatibility
- Validation rules cambiadas

#### Not Found (404)
```bash
# Recursos no encontrados
curl -s "http://prometheus.mtn.cl:9090/api/v1/query?query=rate(http_server_requests_total{status=\"404\"}[5m])" | jq '.'
```

**Posibles Causas:**
- Routing issues
- Database records deleted
- Static assets missing

### B. Errores 5xx (Server Errors)

#### Internal Server Error (500)
```bash
# Excepciones internas
kubectl logs -n mtn-admission deployment/mtn-admission-backend --since=10m | grep -i "500\|internal.*error\|exception"

# Stack traces recientes
kubectl logs -n mtn-admission deployment/mtn-admission-backend --since=10m | grep -A 10 -B 2 "Exception"
```

#### Bad Gateway (502/503/504)
```bash
# Problemas de conectividad
kubectl get pods -n mtn-admission -o wide
kubectl get svc -n mtn-admission

# Health checks
for pod in $(kubectl get pods -n mtn-admission -l app=mtn-admission-backend -o name); do
  echo "=== $pod ==="
  kubectl exec $pod -n mtn-admission -- curl -f http://localhost:8080/actuator/health || echo "UNHEALTHY"
done
```

## ⚡ Mitigación por Tipo de Error (< 15 min)

### A. Si son errores 401/403 (Auth Issues)

```bash
# Verificar JWT configuration
kubectl get configmap mtn-admission-config -n mtn-admission -o yaml | grep -i jwt

# Restart auth-related pods
kubectl rollout restart deployment/mtn-admission-backend -n mtn-admission

# Check external auth service
curl -I http://auth-service.mtn.cl/health
```

### B. Si son errores 500 (Server Issues)

#### 1. Restart Inmediato
```bash
# Rolling restart para limpiar estado corrupto
kubectl rollout restart deployment/mtn-admission-backend -n mtn-admission

# Verificar rollout
kubectl rollout status deployment/mtn-admission-backend -n mtn-admission --timeout=300s
```

#### 2. Scale Up (Si hay load)
```bash
# Aumentar replicas temporalmente
kubectl scale deployment mtn-admission-backend -n mtn-admission --replicas=6

# Verificar distribución de carga
kubectl get pods -n mtn-admission -o wide
```

### C. Si son errores de DB (Connection/Timeout)

```bash
# Verificar DB health
kubectl exec -n mtn-admission deployment/mtn-admission-backend -- \
  psql -h postgres -U admin -d mtn_admission -c "SELECT 1;" || echo "DB UNREACHABLE"

# Reiniciar connection pool
kubectl exec -n mtn-admission deployment/mtn-admission-backend -- \
  curl -X POST http://localhost:8080/actuator/restart-datasource
```

### D. Si son errores 502/503 (Service Mesh Issues)

```bash
# Verificar Istio/Kong Gateway
kubectl get pods -n istio-system
kubectl get pods -n kong

# Restart gateway si es necesario
kubectl rollout restart deployment/kong -n kong
```

## 🔍 Análisis de Causa Raíz Profundo

### 1. Traces de Errores

👉 **[Jaeger - Traces con Errores](http://jaeger.mtn.cl:16686/search?service=mtn-admission-backend&lookback=30m&tags=%7B%22error%22%3A%22true%22%7D)**

**Buscar:**
- Spans marcados con error=true
- Exception messages en span tags
- Timing de requests fallidos
- Servicios downstream que fallan

### 2. Logs Estructurados

```bash
# Buscar patterns de error
kubectl logs -n mtn-admission deployment/mtn-admission-backend --since=20m | \
  jq -r 'select(.level == "ERROR") | "\(.timestamp) [\(.logger)] \(.message)"' | \
  sort | uniq -c | sort -nr | head -10

# Correlacionar por traceId
TRACE_ID="[trace-id-from-jaeger]"
kubectl logs -n mtn-admission deployment/mtn-admission-backend --since=30m | \
  jq -r "select(.traceId == \"$TRACE_ID\") | \"\(.timestamp) [\(.level)] \(.message)\""
```

### 3. Database Analysis

```bash
# Active connections
kubectl exec -n mtn-admission deployment/mtn-admission-backend -- \
  psql -h postgres -U admin -d mtn_admission -c "
    SELECT state, count(*)
    FROM pg_stat_activity
    WHERE datname = 'mtn_admission'
    GROUP BY state;"

# Lock waits
kubectl exec -n mtn-admission deployment/mtn-admission-backend -- \
  psql -h postgres -U admin -d mtn_admission -c "
    SELECT blocked_locks.pid AS blocked_pid,
           blocked_activity.usename AS blocked_user,
           blocking_locks.pid AS blocking_pid,
           blocking_activity.usename AS blocking_user,
           blocked_activity.query AS blocked_statement
    FROM pg_catalog.pg_locks blocked_locks
    JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
    JOIN pg_catalog.pg_locks blocking_locks ON blocking_locks.locktype = blocked_locks.locktype
    JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
    WHERE NOT blocked_locks.granted;"
```

## ✅ Verificación de Recuperación

### 1. Confirmar Error Rate

```bash
# Wait 5 minutos después de mitigación
sleep 300

# Verificar error rate actual
curl -s "http://prometheus.mtn.cl:9090/api/v1/query?query=service:http:error_rate" | \
  jq -r '.data.result[] | select(.value[1] | tonumber > 5) | "\(.metric.service): \(.value[1])%"'
```

**Objetivo**: No debe retornar ningún servicio > 5%

### 2. Test de Endpoints Críticos

```bash
# Test suite completo
./scripts/smoke-tests.sh

# O manual:
curl -f -s http://api.mtn.cl/api/applications/public/all > /dev/null && echo "Applications API: OK"
curl -f -s http://api.mtn.cl/api/auth/test > /dev/null && echo "Auth API: OK"
curl -f -s http://api.mtn.cl/api/evaluations/health > /dev/null && echo "Evaluations API: OK"
```

### 3. Monitoreo de Estabilidad

- **[Error Rate Dashboard](http://grafana.mtn.cl/d/mtn-golden-signals?viewPanel=3&refresh=30s)**
- Observar por 15-20 minutos
- Confirmar que errores están bajo control

## 🛠 Resolución Definitiva

### Si la causa fue:

#### A. **Application Bug**
```bash
# Rollback a versión anterior si es necesario
kubectl rollout history deployment/mtn-admission-backend -n mtn-admission
kubectl rollout undo deployment/mtn-admission-backend -n mtn-admission --to-revision=N

# Crear hotfix branch
git checkout -b hotfix/error-rate-$(date +%Y%m%d)
```

#### B. **Database Issues**
```bash
# Optimizar queries problemáticos
# Crear índices faltantes
# Ajustar connection pool settings
kubectl set env deployment/mtn-admission-backend -n mtn-admission \
  SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=30 \
  SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT=30000
```

#### C. **Dependency Failures**
```bash
# Implementar circuit breaker
# Agregar retry logic
# Configurar fallbacks
```

#### D. **Configuration Issues**
```bash
# Corregir ConfigMap
kubectl edit configmap mtn-admission-config -n mtn-admission

# Aplicar cambios
kubectl rollout restart deployment/mtn-admission-backend -n mtn-admission
```

## 📊 Análisis de Tendencias

### Error Rate Histórico
```bash
# Últimas 24 horas
curl -s "http://prometheus.mtn.cl:9090/api/v1/query_range?query=service:http:error_rate&start=$(date -d '24 hours ago' +%s)&end=$(date +%s)&step=3600" | \
  jq -r '.data.result[0].values[] | "\(.[0] | strftime("%H:%M")): \(.[1])%"'
```

### Top Error Endpoints (Última Semana)
```bash
curl -s "http://prometheus.mtn.cl:9090/api/v1/query?query=topk(10,increase(http_server_requests_total{status=~\"4..|5..\"}[7d]))" | \
  jq -r '.data.result[] | "\(.metric.uri) (\(.metric.status)): \(.value[1]) errors"'
```

## 📝 Post-Incidente

### 1. Root Cause Analysis
- [ ] **Timeline**: Momento exacto de inicio del problema
- [ ] **Causa Raíz**: Qué causó el aumento de errores
- [ ] **Detection**: Cuánto tiempo tomó detectar el problema
- [ ] **Resolution**: Efectividad de las acciones tomadas

### 2. Mejoras Preventivas
- [ ] **Alerting**: Crear alertas tempranas (warning a 3%)
- [ ] **Monitoring**: Mejorar dashboards de error analysis
- [ ] **Testing**: Agregar casos de test para scenarios encontrados
- [ ] **Documentation**: Actualizar runbooks con lecciones aprendidas

### 3. Comunicación de Cierre
```markdown
**RESUELTO**: Error Rate Alto - Sistema Admisión MTN

**Duración**: XX minutos
**Error Rate Peak**: X.X%
**Requests Afectados**: ~XXX requests
**Causa**: [Descripción detallada]
**Solución**: [Acciones específicas tomadas]
**Prevención**: [Medidas para evitar recurrencia]

**Impact Assessment**:
- Familias afectadas: ~XX
- Postulaciones perdidas: XX
- Evaluaciones retrasadas: XX

Canal: #incidents-mtn
Owner: @sre-team
```

## 🎯 Métricas de Éxito

- **MTTR**: < 20 minutos
- **Error Rate Recovery**: < 1% en 30 minutos
- **Zero Data Loss**: Todas las transacciones deben ser recuperables

---

**Runbook Owner**: SRE Team  
**Última Actualización**: $(date "+%Y-%m-%d")  
**Versión**: 1.0