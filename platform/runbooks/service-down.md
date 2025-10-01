# Runbook: Servicio Caído

**🔴 Criticidad: CRÍTICA**  
**📟 Canal: PagerDuty**  
**🎯 SLO: Uptime > 99.9%**

## 🚨 Descripción del Problema

Uno o más servicios críticos del sistema de admisión no responde a health checks, indicando una caída total del servicio. Esto resulta en pérdida completa de funcionalidad para los usuarios.

### Impacto en el Negocio
- **Sistema Completo**: Inaccessible para todos los usuarios
- **Proceso Admisión**: Paralizado completamente
- **Reputación**: Impacto crítico en confiabilidad
- **Revenue**: Pérdida de período crítico de admisiones

## 📊 Alertas Relacionadas

```yaml
alert: MTNServiceDown
expr: up{job=~"mtn-.*"} == 0
for: 1m
severity: critical
runbook: https://runbooks.mtn.cl/service-down
```

## 🔍 Investigación Inmediata (< 2 min)

### 1. Confirmar Servicios Afectados

```bash
# Verificar estado de todos los servicios MTN
curl -s "http://prometheus.mtn.cl:9090/api/v1/query?query=up{job=~\"mtn-.*\"}" | \
  jq -r '.data.result[] | select(.value[1] == "0") | "\(.metric.job): DOWN"'

# Estado de pods en Kubernetes
kubectl get pods -n mtn-admission -o wide
```

### 2. Dashboard de Disponibilidad
👉 **[Grafana Service Health](http://grafana.mtn.cl/d/mtn-golden-signals?viewPanel=1)**

### 3. Verificación Rápida de Conectividad

```bash
# Test directo a endpoints
services=("mtn-admission-backend" "mtn-evaluation-service" "mtn-notification-service")
for service in "${services[@]}"; do
  echo "=== $service ==="
  curl -m 5 -s -o /dev/null -w "HTTP: %{http_code} | Time: %{time_total}s\n" \
    "http://${service}.mtn-admission.svc.cluster.local:8080/actuator/health" 2>/dev/null || echo "UNREACHABLE"
done
```

## ⚡ Recovery Inmediato (< 5 min)

### 1. Restart de Servicios Afectados

```bash
# Identificar servicios down y restart
for deployment in $(kubectl get deployments -n mtn-admission -o name | grep mtn); do
  deployment_name=$(echo $deployment | cut -d/ -f2)
  
  # Check if deployment is ready
  ready=$(kubectl get deployment $deployment_name -n mtn-admission -o jsonpath='{.status.readyReplicas}')
  replicas=$(kubectl get deployment $deployment_name -n mtn-admission -o jsonpath='{.spec.replicas}')
  
  if [ "$ready" != "$replicas" ] || [ -z "$ready" ]; then
    echo "🔄 Restarting $deployment_name..."
    kubectl rollout restart deployment/$deployment_name -n mtn-admission
  fi
done
```

### 2. Verificar Restart Status

```bash
# Monitor rollout progress
deployments=($(kubectl get deployments -n mtn-admission -o name | grep mtn | cut -d/ -f2))
for deploy in "${deployments[@]}"; do
  echo "Checking rollout status for $deploy..."
  kubectl rollout status deployment/$deploy -n mtn-admission --timeout=180s
done
```

### 3. Emergency Scale Up

```bash
# Scale up all services for faster recovery
kubectl scale deployment mtn-admission-backend -n mtn-admission --replicas=4
kubectl scale deployment mtn-evaluation-service -n mtn-admission --replicas=2
kubectl scale deployment mtn-notification-service -n mtn-admission --replicas=2

# Wait for pods to become ready
sleep 60
kubectl get pods -n mtn-admission -l app=mtn-admission-backend
```

## 🔍 Análisis de Causa Raíz (< 10 min)

### A. Container/Pod Level Issues

```bash
# Check pod events and describe
kubectl get events -n mtn-admission --sort-by=.metadata.creationTimestamp | tail -20

# Describe problematic pods
for pod in $(kubectl get pods -n mtn-admission | grep -v Running | grep -v NAME | awk '{print $1}'); do
  echo "=== Pod: $pod ==="
  kubectl describe pod $pod -n mtn-admission | grep -A 10 -B 5 "Events:"
done

# Check pod logs for crashes
kubectl logs -n mtn-admission deployment/mtn-admission-backend --previous --tail=50
```

### B. Resource Constraints

```bash
# Node resource usage
kubectl top nodes

# Pod resource usage
kubectl top pods -n mtn-admission

# Check resource limits
kubectl describe pods -n mtn-admission | grep -A 5 "Limits\|Requests"
```

### C. Network/DNS Issues

```bash
# Test internal DNS resolution
kubectl run -it --rm debug --image=nicolaka/netshoot --restart=Never -- bash
# Inside the debug pod:
# nslookup mtn-admission-backend.mtn-admission.svc.cluster.local
# curl -I http://mtn-admission-backend.mtn-admission.svc.cluster.local:8080/actuator/health
```

### D. Database Connectivity

```bash
# Test DB from application pods (if any are running)
kubectl exec -n mtn-admission deployment/mtn-admission-backend -- \
  psql -h postgres -U admin -d mtn_admission -c "SELECT version();" 2>/dev/null || echo "DB UNREACHABLE"

# Check database pod status
kubectl get pods -n postgres -l app=postgresql
kubectl logs -n postgres deployment/postgresql --tail=30
```

### E. Infrastructure Issues

```bash
# Check cluster node status
kubectl get nodes -o wide

# Verify ingress controller
kubectl get pods -n ingress-nginx
kubectl logs -n ingress-nginx deployment/ingress-nginx-controller --tail=20

# Check persistent volumes
kubectl get pv,pvc -n mtn-admission
```

## 🛠 Resolución por Escenario

### A. Memory/CPU Exhaustion

```bash
# Increase resource limits
kubectl patch deployment mtn-admission-backend -n mtn-admission -p '
{
  "spec": {
    "template": {
      "spec": {
        "containers": [{
          "name": "mtn-admission-backend",
          "resources": {
            "limits": {"memory": "2Gi", "cpu": "1000m"},
            "requests": {"memory": "1Gi", "cpu": "500m"}
          }
        }]
      }
    }
  }
}'

# Apply HPA for auto-scaling
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
  minReplicas: 2
  maxReplicas: 8
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
EOF
```

### B. Database Connection Issues

```bash
# Restart database connections
kubectl rollout restart deployment/postgresql -n postgres

# Increase connection pool
kubectl set env deployment/mtn-admission-backend -n mtn-admission \
  SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=50 \
  SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT=60000
```

### C. Image Pull Errors

```bash
# Check image pull secrets
kubectl get secrets -n mtn-admission

# Manually pull image on nodes if needed
# docker pull mtn-registry.com/admission-backend:latest
```

### D. Storage Issues

```bash
# Check PVC status
kubectl get pvc -n mtn-admission

# If PVC is pending, check storage class
kubectl get storageclass
kubectl describe pvc -n mtn-admission
```

## ✅ Verificación de Recuperación (< 5 min)

### 1. Health Checks

```bash
# Wait for pods to be ready
kubectl wait --for=condition=ready pod -l app=mtn-admission-backend -n mtn-admission --timeout=300s

# Verify all services are UP
curl -s "http://prometheus.mtn.cl:9090/api/v1/query?query=up{job=~\"mtn-.*\"}" | \
  jq -r '.data.result[] | "\(.metric.job): \(.value[1] == "1" and "UP" or "DOWN")"'
```

### 2. End-to-End Smoke Tests

```bash
# Critical API endpoints
endpoints=(
  "http://api.mtn.cl/api/applications/public/all"
  "http://api.mtn.cl/api/auth/test"
  "http://api.mtn.cl/actuator/health"
)

for endpoint in "${endpoints[@]}"; do
  echo -n "Testing $endpoint: "
  if curl -f -s -m 10 "$endpoint" > /dev/null; then
    echo "✅ OK"
  else
    echo "❌ FAILED"
  fi
done
```

### 3. User Acceptance Test

```bash
# Simulate user workflow
./scripts/user-workflow-test.sh

# Or manual verification:
# 1. Load frontend application
# 2. Login with test user
# 3. Submit test application
# 4. Verify in admin dashboard
```

## 📊 Post-Recovery Monitoring

### 1. Stability Check (15 minutes)

```bash
# Monitor for 15 minutes post-recovery
echo "Monitoring stability for 15 minutes..."
for i in {1..15}; do
  echo "Minute $i/15: $(date)"
  
  # Check service health
  down_services=$(curl -s "http://prometheus.mtn.cl:9090/api/v1/query?query=up{job=~\"mtn-.*\"}" | \
    jq -r '.data.result[] | select(.value[1] == "0") | .metric.job' | wc -l)
  
  if [ "$down_services" -gt 0 ]; then
    echo "⚠️  $down_services services still down!"
  else
    echo "✅ All services UP"
  fi
  
  sleep 60
done
```

### 2. Performance Verification

```bash
# Check latency is acceptable
curl -s "http://prometheus.mtn.cl:9090/api/v1/query?query=service:http:p99_latency_seconds" | \
  jq -r '.data.result[] | select(.value[1] | tonumber > 2) | "HIGH LATENCY: \(.metric.service): \(.value[1])s"'

# Check error rate is low
curl -s "http://prometheus.mtn.cl:9090/api/v1/query?query=service:http:error_rate" | \
  jq -r '.data.result[] | select(.value[1] | tonumber > 5) | "HIGH ERROR RATE: \(.metric.service): \(.value[1])%"'
```

## 📝 Post-Mortem Actions

### 1. Impact Assessment

```sql
-- Calculate downtime impact (run after recovery)
SELECT 
  'Service Downtime: ' || EXTRACT(EPOCH FROM (NOW() - '2024-XX-XX XX:XX:XX'::timestamp))/60 || ' minutes' as duration,
  'Peak Hour: ' || CASE WHEN EXTRACT(HOUR FROM NOW()) BETWEEN 8 AND 18 THEN 'YES' ELSE 'NO' END as peak_impact,
  'School Day: ' || CASE WHEN EXTRACT(DOW FROM NOW()) BETWEEN 1 AND 5 THEN 'YES' ELSE 'NO' END as school_day;
```

### 2. Timeline Reconstruction

```bash
# Extract timeline from logs and metrics
echo "=== INCIDENT TIMELINE ==="
echo "Detection: $(date)"
echo "Recovery Started: $(date)"
echo "Services Restored: $(date)"

# Count affected requests during downtime
curl -s "http://prometheus.mtn.cl:9090/api/v1/query_range?query=increase(http_server_requests_total[1m])&start=${START_TIME}&end=${END_TIME}&step=60" | \
  jq '.data.result[0].values | length as $total | "Affected Requests: Approximately \($total) requests during downtime"'
```

### 3. Root Cause Documentation

```markdown
## Incident Report: Service Downtime

**Date**: $(date "+%Y-%m-%d %H:%M CLT")
**Duration**: XX minutes
**Severity**: Critical (Complete service outage)
**MTTR**: XX minutes

### Root Cause
[Detailed technical explanation of what caused the outage]

### Impact
- **Users Affected**: Estimated XXX users
- **Business Impact**: XXX applications blocked, XXX evaluations delayed
- **Revenue Impact**: N/A (public service)

### Timeline
- XX:XX - Service goes down (automated detection)
- XX:XX - PagerDuty alert triggered
- XX:XX - SRE response begins
- XX:XX - Root cause identified
- XX:XX - Recovery actions initiated
- XX:XX - Service restored
- XX:XX - Stability confirmed

### Actions Taken
1. [List all recovery actions performed]
2. [Include any emergency changes]

### Prevention Measures
- [ ] Improve monitoring for early detection
- [ ] Add redundancy where applicable
- [ ] Update runbooks with lessons learned
- [ ] Implement additional health checks

### Follow-up Items
- [ ] Technical debt ticket: [JIRA-XXX]
- [ ] Process improvement: [JIRA-XXX]
- [ ] Infrastructure upgrade: [JIRA-XXX]
```

### 4. Stakeholder Communication

```markdown
**SERVICE RESTORED - MTN Admission System**

The admission system is now fully operational after a XX-minute outage.

**What Happened**: [Brief, non-technical explanation]
**Impact**: Temporary inability to access the system
**Resolution**: [Brief description of fix]
**Prevention**: [High-level prevention measures]

**Timeline**:
- XX:XX - Issue detected
- XX:XX - Recovery began
- XX:XX - Service restored

**Next Steps**: We are conducting a thorough review to prevent future occurrences.

For technical details: [Link to post-mortem]

*SRE Team - Colegio Monte Tabor y Nazaret*
```

## 🎯 Success Metrics

- **RTO (Recovery Time Objective)**: < 15 minutes
- **RPO (Recovery Point Objective)**: 0 data loss
- **MTTR (Mean Time To Recovery)**: < 10 minutes
- **Communication**: Stakeholders notified within 5 minutes

## 🔄 Continuous Improvement

### Enhanced Monitoring
- Add synthetic monitoring for critical user journeys
- Implement chaos engineering for resilience testing
- Create more granular health checks

### Infrastructure Improvements
- Multi-zone deployment for high availability
- Blue-green deployment for zero-downtime updates
- Automated failover mechanisms

---

**Runbook Owner**: SRE Team  
**Última Actualización**: $(date "+%Y-%m-%d")  
**Versión**: 1.0  
**Emergency Contact**: +56 9 XXXX XXXX