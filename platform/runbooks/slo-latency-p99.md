# Runbook: SLO Latencia P99 Alto

**🔴 Criticidad: CRÍTICA**  
**📟 Canal: PagerDuty**  
**🎯 SLO: P99 latencia < 2 segundos**

## 🚨 Descripción del Problema

El percentil 99 de latencia HTTP ha superado los 2 segundos, violando el SLO crítico del sistema. Esto afecta directamente la experiencia de usuarios durante el proceso de admisión.

### Impacto en el Negocio
- **Familias**: Dificultad para completar postulaciones
- **Staff**: Lentitud en evaluaciones y procesamiento
- **Sistema**: Posible colapso por acumulación de requests

## 📊 Alertas Relacionadas

```yaml
alert: MTNLatencyHigh
expr: service:http:p99_latency_seconds > 2
for: 2m
severity: critical
runbook: https://runbooks.mtn.cl/slo-latency-p99
```

## 🔍 Investigación Inicial (< 5 min)

### 1. Confirmar Scope del Problema

```bash
# Verificar latencia actual por servicio
curl -s "http://prometheus.mtn.cl:9090/api/v1/query?query=service:http:p99_latency_seconds" | jq '.'

# Ver endpoints más afectados  
curl -s "http://prometheus.mtn.cl:9090/api/v1/query?query=histogram_quantile(0.99,rate(http_server_requests_seconds_bucket[5m]))" | jq '.'
```

### 2. Dashboard Principal
👉 **[Grafana Golden Signals](http://grafana.mtn.cl/d/mtn-golden-signals)**

**Métricas Clave:**
- P99 latencia por servicio
- Request rate (posible correlación)
- Error rate (puede estar relacionado)
- CPU/Memory utilization

### 3. Identificar Servicios Afectados

```bash
# Top servicios con alta latencia
curl -s "http://prometheus.mtn.cl:9090/api/v1/query?query=topk(5,service:http:p99_latency_seconds)" | jq -r '.data.result[] | "\(.metric.service): \(.value[1])s"'
```

## 🔬 Análisis de Causa Raíz (< 10 min)

### A. Verificar Infraestructura

#### CPU/Memory
```bash
# CPU por pod
kubectl top pods -n mtn-admission

# Memory por pod
kubectl describe pods -n mtn-admission | grep -A 5 "Limits\|Requests"
```

**Umbrales:**
- CPU > 80% → Scale up inmediato
- Memory > 85% → Posible memory leak

#### Base de Datos
```bash
# Conexiones activas
kubectl exec -n mtn-admission deployment/mtn-admission-backend -- \
  psql -h postgres -U admin -d mtn_admission -c "SELECT count(*) FROM pg_stat_activity WHERE state = 'active';"

# Queries lentas
kubectl exec -n mtn-admission deployment/mtn-admission-backend -- \
  psql -h postgres -U admin -d mtn_admission -c "SELECT query, query_start, state_change FROM pg_stat_activity WHERE state = 'active' AND query_start < NOW() - INTERVAL '10 seconds';"
```

### B. Verificar Tráfico/Load

#### Request Rate
```bash
# Spike de tráfico?
curl -s "http://prometheus.mtn.cl:9090/api/v1/query?query=rate(http_server_requests_total[5m])" | jq '.'

# Comparar con baseline histórico
curl -s "http://prometheus.mtn.cl:9090/api/v1/query_range?query=rate(http_server_requests_total[5m])&start=$(date -d '1 hour ago' +%s)&end=$(date +%s)&step=60"
```

### C. Traces Distribuidos

👉 **[Jaeger - Traces Lentos](http://jaeger.mtn.cl:16686/search?service=mtn-admission-backend&lookback=1h&minDuration=2s)**

**Buscar:**
- Spans con duración > 2s
- Database queries lentos
- External API calls timeout
- Lock contention

### D. Logs Correlacionados

```bash
# Buscar errores recientes
kubectl logs -n mtn-admission deployment/mtn-admission-backend --since=10m | grep -i "error\|exception\|timeout"

# Performance warnings
kubectl logs -n mtn-admission deployment/mtn-admission-backend --since=10m | grep -i "slow\|performance\|latency"
```

## ⚡ Mitigación Inmediata (< 15 min)

### 1. Scale Up Rápido (Si CPU/Memory Alto)

```bash
# Escalar backend
kubectl scale deployment mtn-admission-backend -n mtn-admission --replicas=6

# Verificar rolling update
kubectl rollout status deployment/mtn-admission-backend -n mtn-admission
```

### 2. Database Connection Pool (Si DB es cuello de botella)

```bash
# Aumentar pool size temporalmente
kubectl set env deployment/mtn-admission-backend -n mtn-admission \
  SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=50

# Verificar aplicación
kubectl rollout status deployment/mtn-admission-backend -n mtn-admission
```

### 3. Cache Warmup (Si aplicable)

```bash
# Trigger cache warmup endpoint
for pod in $(kubectl get pods -n mtn-admission -l app=mtn-admission-backend -o name); do
  kubectl exec $pod -n mtn-admission -- curl -X POST http://localhost:8080/actuator/cache-warmup
done
```

### 4. Rate Limiting (Emergencia)

```bash
# Activar rate limiting en Kong Gateway
kubectl exec -n kong deployment/kong -- \
  curl -X POST http://localhost:8001/services/mtn-admission/plugins \
  --data "name=rate-limiting" \
  --data "config.minute=300" \
  --data "config.policy=local"
```

## ✅ Verificación de Recuperación

### 1. Confirmar Métricas

```bash
# Wait 2-3 minutos, luego verificar
curl -s "http://prometheus.mtn.cl:9090/api/v1/query?query=service:http:p99_latency_seconds" | jq -r '.data.result[] | select(.value[1] | tonumber > 2) | "\(.metric.service): \(.value[1])s"'
```

**Objetivo**: No debe retornar ningún servicio > 2s

### 2. Test de Humo

```bash
# Test endpoints críticos
curl -w "@curl-format.txt" http://api.mtn.cl/api/applications/public/all
curl -w "@curl-format.txt" http://api.mtn.cl/api/auth/test
```

Archivo `curl-format.txt`:
```
     time_namelookup:  %{time_namelookup}\n
        time_connect:  %{time_connect}\n
     time_appconnect:  %{time_appconnect}\n
    time_pretransfer:  %{time_pretransfer}\n
       time_redirect:  %{time_redirect}\n
  time_starttransfer:  %{time_starttransfer}\n
                     ----------\n
          time_total:  %{time_total}\n
```

### 3. Monitoreo Continuo

- **[Dashboard en tiempo real](http://grafana.mtn.cl/d/mtn-golden-signals?refresh=10s)**
- Observar por 10-15 minutos antes de declarar resuelto

## 🛠 Resolución Definitiva

### Si la causa fue:

#### A. **Alta Carga de Tráfico**
```bash
# Configurar HPA
kubectl apply -f - <<EOF
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: mtn-admission-hpa
  namespace: mtn-admission
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: mtn-admission-backend
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
EOF
```

#### B. **Database Queries Lentos**
- Identificar queries problemáticos en logs
- Crear índices faltantes
- Optimizar queries N+1
- Considerar cache de queries

#### C. **Memory Leak**
```bash
# Generar heap dump
kubectl exec -n mtn-admission deployment/mtn-admission-backend -- \
  jcmd 1 GC.run_finalization

kubectl exec -n mtn-admission deployment/mtn-admission-backend -- \
  jcmd 1 VM.classloader_stats
```

#### D. **External API Timeouts**
- Verificar health de APIs externas
- Ajustar timeouts y retries
- Implementar circuit breakers

## 📝 Post-Incidente

### 1. Crear Ticket de Seguimiento
- **Jira**: Crear ticket con label `post-mortem`
- **Descripción**: Timeline, causa raíz, acciones tomadas
- **Assignee**: Tech Lead

### 2. Acciones Preventivas
- [ ] Revisar alertas preventivas (Warn antes de Crit)
- [ ] Ajustar SLO si es necesario
- [ ] Implementar mejoras en monitoreo
- [ ] Actualizar playbook con nuevos aprendizajes

### 3. Comunicación
```markdown
**RESUELTO**: Latencia alta Sistema Admisión

**Duración**: XX minutos  
**Impacto**: Alta latencia en endpoints críticos  
**Causa**: [Descripción]  
**Solución**: [Acciones tomadas]  
**Prevención**: [Medidas implementadas]  

Canal: #incidents-mtn
```

## 🎯 Métricas de Éxito

- **MTTR**: < 30 minutos
- **Falsos Positivos**: < 5% mensual
- **Recurrencia**: 0 incidentes similares en 30 días

---

**Runbook Owner**: SRE Team  
**Última Actualización**: $(date "+%Y-%m-%d")  
**Versión**: 1.0