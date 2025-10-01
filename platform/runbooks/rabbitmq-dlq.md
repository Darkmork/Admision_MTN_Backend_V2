# Runbook: RabbitMQ Dead Letter Queue No Vacía

**🔴 Criticidad: CRÍTICA**  
**📟 Canal: PagerDuty**  
**🎯 SLO: DLQ messages = 0**

## 🚨 Descripción del Problema

Se detectaron mensajes en las colas de Dead Letter Queue (DLQ), indicando fallos en el procesamiento de mensajes críticos del sistema de admisión. Esto puede resultar en pérdida de notificaciones, evaluaciones sin procesar, o postulaciones no registradas.

### Impacto en el Negocio
- **Notificaciones**: Emails no enviados a familias
- **Evaluaciones**: Resultados no procesados
- **Workflow**: Interrupción del flujo de admisión
- **Data Loss**: Riesgo de pérdida de información crítica

## 📊 Alertas Relacionadas

```yaml
alert: MTNRabbitMQDLQNotEmpty
expr: queue:dlq:messages > 0
for: 0s  # Immediate alert
severity: critical
runbook: https://runbooks.mtn.cl/rabbitmq-dlq
```

## 🔍 Investigación Inicial (< 3 min)

### 1. Identificar Colas Afectadas

```bash
# Ver todas las DLQ con mensajes
curl -s -u admin:admin123 "http://rabbitmq.mtn.cl:15672/api/queues" | \
  jq -r '.[] | select(.name | contains("dlq")) | select(.messages > 0) | "\(.name): \(.messages) messages"'

# Métrica desde Prometheus
curl -s "http://prometheus.mtn.cl:9090/api/v1/query?query=queue:dlq:messages" | \
  jq -r '.data.result[] | select(.value[1] | tonumber > 0) | "\(.metric.queue): \(.value[1]) messages"'
```

### 2. Dashboard RabbitMQ
👉 **[Grafana RabbitMQ Dashboard](http://grafana.mtn.cl/d/mtn-rabbitmq)**

**Métricas Críticas:**
- DLQ message count por cola
- Message age en DLQ
- Publish rate vs consume rate
- Consumer count por cola

### 3. RabbitMQ Management UI
👉 **[RabbitMQ Management](http://rabbitmq.mtn.cl:15672)**

**Usuario**: admin / **Password**: admin123

## 🔬 Análisis por Cola DLQ (< 5 min)

### A. Applications DLQ

```bash
# Detalles de applications DLQ
curl -s -u admin:admin123 "http://rabbitmq.mtn.cl:15672/api/queues/mtn/mtn.applications.dlq" | \
  jq '{name: .name, messages: .messages, consumers: .consumers, message_stats: .message_stats}'

# Sample de mensajes (solo metadata, no body por privacidad)
curl -s -u admin:admin123 "http://rabbitmq.mtn.cl:15672/api/queues/mtn/mtn.applications.dlq/get" \
  -d '{"count":5,"encoding":"auto","ackmode":"ack_requeue_false","truncate":1000}' | \
  jq '.[] | {routing_key: .routing_key, properties: .properties, exchange: .exchange}'
```

**Problemas Comunes:**
- Application service down
- Database connection lost
- Validation errors en payload
- Timeout en procesamiento

### B. Evaluations DLQ

```bash
# Detalles de evaluations DLQ
curl -s -u admin:admin123 "http://rabbitmq.mtn.cl:15672/api/queues/mtn/mtn.evaluations.dlq" | \
  jq '{name: .name, messages: .messages, consumers: .consumers}'

# Verificar evaluations service
kubectl get pods -n mtn-admission -l app=mtn-evaluations-service
kubectl logs -n mtn-admission deployment/mtn-evaluations-service --since=10m --tail=50
```

### C. Notifications DLQ

```bash
# Detalles de notifications DLQ
curl -s -u admin:admin123 "http://rabbitmq.mtn.cl:15672/api/queues/mtn/mtn.notifications.dlq" | \
  jq '{name: .name, messages: .messages, consumers: .consumers}'

# Verificar email service
kubectl logs -n mtn-admission deployment/mtn-notification-service --since=10m | grep -i "error\|fail\|smtp"
```

## ⚡ Mitigación Inmediata (< 10 min)

### 1. Verificar Servicios Consumidores

```bash
# Verificar todos los consumers
kubectl get pods -n mtn-admission | grep -E "(application|evaluation|notification)"

# Restart unhealthy services
for service in application evaluation notification; do
  if ! kubectl get pods -n mtn-admission -l app=mtn-${service}-service | grep -q "Running"; then
    echo "Restarting ${service} service..."
    kubectl rollout restart deployment/mtn-${service}-service -n mtn-admission
  fi
done
```

### 2. Re-queue Mensajes Críticos (Si es seguro)

**⚠️ CUIDADO**: Solo re-queue si estás seguro de que el problema consumidor está resuelto.

```bash
# Function para mover mensajes de DLQ a cola principal
requeue_dlq_messages() {
  local dlq_name=$1
  local main_queue=$2
  
  echo "Moving messages from $dlq_name to $main_queue..."
  
  # Get messages from DLQ
  curl -s -u admin:admin123 "http://rabbitmq.mtn.cl:15672/api/queues/mtn/${dlq_name}/get" \
    -d '{"count":100,"encoding":"auto","ackmode":"ack_requeue_false"}' | \
    jq -c '.[]' | while read message; do
      
      # Publish to main queue
      echo "$message" | jq '{
        properties: .properties,
        routing_key: "'$main_queue'",
        payload: .payload,
        payload_encoding: .payload_encoding
      }' | curl -s -u admin:admin123 \
        "http://rabbitmq.mtn.cl:15672/api/exchanges/mtn/mtn.events/publish" \
        -d @-
    done
}

# Solo ejecutar después de confirmar que consumers están healthy
# requeue_dlq_messages "mtn.applications.dlq" "application.submit"
```

### 3. Scale Up Consumers (Si hay backlog)

```bash
# Aumentar replicas temporalmente
kubectl scale deployment mtn-application-service -n mtn-admission --replicas=3
kubectl scale deployment mtn-evaluation-service -n mtn-admission --replicas=2
kubectl scale deployment mtn-notification-service -n mtn-admission --replicas=3

# Verificar que consumers se conecten
sleep 30
curl -s -u admin:admin123 "http://rabbitmq.mtn.cl:15672/api/queues" | \
  jq -r '.[] | select(.name | contains("mtn.") and (contains("applications") or contains("evaluations") or contains("notifications"))) | "\(.name): \(.consumers) consumers"'
```

## 🔍 Análisis de Causa Raíz

### 1. Revisar Logs de Error

```bash
# Buscar exceptions en consumers
kubectl logs -n mtn-admission deployment/mtn-application-service --since=30m | \
  grep -i "exception\|error\|failed" | tail -20

kubectl logs -n mtn-admission deployment/mtn-evaluation-service --since=30m | \
  grep -i "exception\|error\|failed" | tail -20

kubectl logs -n mtn-admission deployment/mtn-notification-service --since=30m | \
  grep -i "exception\|error\|failed" | tail -20
```

### 2. Database Connectivity

```bash
# Test DB connections desde cada service
for service in application evaluation notification; do
  echo "=== Testing $service DB connection ==="
  kubectl exec -n mtn-admission deployment/mtn-${service}-service -- \
    psql -h postgres -U admin -d mtn_admission -c "SELECT 1;" || echo "DB UNREACHABLE for $service"
done
```

### 3. Mensaje Sample Analysis

```bash
# Examinar un mensaje de DLQ para entender el problema
MESSAGE=$(curl -s -u admin:admin123 "http://rabbitmq.mtn.cl:15672/api/queues/mtn/mtn.applications.dlq/get" \
  -d '{"count":1,"encoding":"auto","ackmode":"ack_requeue_false"}' | jq '.[0]')

echo "$MESSAGE" | jq '{
  routing_key: .routing_key,
  exchange: .exchange,
  message_count: .message_count,
  properties: .properties,
  payload_bytes: (.payload | length)
}'

# Verificar headers para debugging
echo "$MESSAGE" | jq '.properties.headers'
```

### 4. Traces de Mensajes Fallidos

👉 **[Jaeger - AMQP Traces con Error](http://jaeger.mtn.cl:16686/search?service=mtn-application-service&lookback=1h&tags=%7B%22error%22%3A%22true%22%2C%22component%22%3A%22rabbitmq%22%7D)**

**Buscar:**
- Spans de message processing con error=true
- Exception details en span logs
- Timing de message processing
- Database query failures

## 🛠 Resolución por Tipo de Fallo

### A. Database Connection Issues

```bash
# Restart DB connection pools
kubectl exec -n mtn-admission deployment/mtn-application-service -- \
  curl -X POST http://localhost:8080/actuator/restart-datasource

# Increase connection timeout
kubectl set env deployment/mtn-application-service -n mtn-admission \
  SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT=60000
```

### B. Memory/Resource Issues

```bash
# Check memory usage
kubectl top pods -n mtn-admission

# Increase memory limits if needed
kubectl patch deployment mtn-application-service -n mtn-admission -p '
{
  "spec": {
    "template": {
      "spec": {
        "containers": [{
          "name": "mtn-application-service",
          "resources": {
            "limits": {"memory": "1Gi"},
            "requests": {"memory": "512Mi"}
          }
        }]
      }
    }
  }
}'
```

### C. Application Logic Errors

```bash
# Rollback to previous version if needed
kubectl rollout history deployment/mtn-application-service -n mtn-admission
kubectl rollout undo deployment/mtn-application-service -n mtn-admission --to-revision=N
```

### D. External Service Failures

```bash
# Check external dependencies
curl -I http://external-api.example.com/health

# Enable circuit breaker if available
kubectl set env deployment/mtn-notification-service -n mtn-admission \
  CIRCUIT_BREAKER_ENABLED=true
```

## ✅ Verificación de Recuperación

### 1. Confirmar DLQ Vacías

```bash
# Wait 5-10 minutos para que se procesen mensajes
sleep 600

# Verificar que DLQs estén vacías
curl -s -u admin:admin123 "http://rabbitmq.mtn.cl:15672/api/queues" | \
  jq -r '.[] | select(.name | contains("dlq")) | select(.messages > 0) | "\(.name): \(.messages) messages"'
```

**Objetivo**: No debe retornar ninguna cola con mensajes

### 2. Monitorear Processing Rate

```bash
# Verificar que consumers están procesando
curl -s "http://prometheus.mtn.cl:9090/api/v1/query?query=queue:messages:consume_rate" | \
  jq -r '.data.result[] | "\(.metric.queue): \(.value[1]) msg/sec"'
```

### 3. Test de Flujo End-to-End

```bash
# Simular workflow completo
./scripts/test-admission-workflow.sh

# O manual:
# 1. Submit application
# 2. Trigger evaluation
# 3. Send notification
# 4. Verificar que todo se procesa sin ir a DLQ
```

## 📊 Prevención Futura

### 1. Monitoring Mejorado

```yaml
# Nueva alerta preventiva
alert: MTNRabbitMQHighProcessingLatency
expr: histogram_quantile(0.99, rabbitmq_queue_message_age_seconds_bucket) > 300
for: 2m
severity: warning
summary: "Message processing latency high"
```

### 2. Circuit Breakers

```bash
# Implementar circuit breaker en consumers
kubectl set env deployment/mtn-application-service -n mtn-admission \
  SPRING_CLOUD_CIRCUITBREAKER_RESILIENCE4J_ENABLED=true
```

### 3. Dead Letter Exchange Routing

```bash
# Configurar TTL más alto para debugging
curl -u admin:admin123 -X PUT "http://rabbitmq.mtn.cl:15672/api/queues/mtn/mtn.applications.dlq" \
  -d '{"arguments":{"x-message-ttl":86400000}}' # 24 horas
```

## 📝 Post-Incidente

### 1. Análisis de Mensajes Perdidos

```bash
# Count de mensajes que fueron a DLQ por período
curl -s "http://prometheus.mtn.cl:9090/api/v1/query?query=increase(queue:dlq:messages[1h])" | \
  jq -r '.data.result[] | "\(.metric.queue): \(.value[1]) messages lost in last hour"'
```

### 2. Business Impact Assessment

- **Notificaciones perdidas**: Estimar familias no notificadas
- **Evaluaciones retrasadas**: Count de evaluaciones pendientes
- **Applications no procesadas**: Verificar integridad de data

### 3. Acciones Correctivas

- [ ] **Code Review**: Revisar error handling en consumers
- [ ] **Testing**: Agregar tests para failure scenarios
- [ ] **Monitoring**: Mejorar alertas preventivas
- [ ] **Documentation**: Actualizar runbooks con nueva información

### 4. Comunicación de Cierre

```markdown
**RESUELTO**: RabbitMQ DLQ Messages - Sistema Admisión

**Duración**: XX minutos  
**Mensajes Afectados**: XX mensajes  
**Colas Afectadas**: mtn.applications.dlq, mtn.notifications.dlq  
**Causa**: [Descripción específica]  
**Recuperación**: XX% mensajes recuperados  
**Data Loss**: Ninguna (todos los mensajes re-procesados)  

**Acciones Tomadas**:
- Restart de consumer services
- Re-queue de mensajes críticos  
- Scale up temporal de consumers

Canal: #incidents-mtn
```

## 🎯 Métricas de Éxito

- **MTTR**: < 15 minutos
- **Message Recovery**: > 99% mensajes recuperados
- **Zero Business Impact**: Todas las transacciones críticas preservadas

---

**Runbook Owner**: SRE Team  
**Última Actualización**: $(date "+%Y-%m-%d")  
**Versión**: 1.0