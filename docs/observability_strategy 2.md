# Estrategia de Observabilidad End-to-End para MTN

## Visión General

La observabilidad en el sistema MTN sigue los **Tres Pilares de Observabilidad**:
- **Métricas**: Datos agregados y series temporales
- **Logs**: Eventos discretos con contexto detallado  
- **Traces**: Seguimiento de requests a través de múltiples servicios

## Stack Tecnológico

### Core Observability
- **Métricas**: Micrometer + Prometheus + Grafana
- **Logs**: Logback + ELK Stack (Elasticsearch, Logstash, Kibana)
- **Tracing**: OpenTelemetry + Jaeger
- **Alerting**: Prometheus AlertManager + PagerDuty

### Integración Spring Boot
- **Spring Boot Actuator**: Endpoints de métricas y salud
- **Micrometer**: Abstracción para métricas
- **Sleuth**: Distributed tracing (migrando a OpenTelemetry)

## Métricas por Dominio

### Application Service - Métricas de Negocio
```yaml
# Admisiones por estado
mtn.applications.by_status:
  labels: [status, school, level]
  type: gauge
  
# Tiempo de procesamiento de aplicaciones
mtn.application.processing_duration:
  labels: [status_transition, school]
  type: timer
  
# Documentos subidos
mtn.documents.uploaded:
  labels: [document_type, status]
  type: counter

# Evaluaciones pendientes por nivel
mtn.applications.pending_evaluations:
  labels: [educational_level, subject]
  type: gauge
```

### Evaluation Service - Métricas de Evaluación
```yaml
# Evaluaciones completadas
mtn.evaluations.completed:
  labels: [subject, level, result]
  type: counter
  
# Tiempo de evaluación por evaluador
mtn.evaluation.duration:
  labels: [evaluator_id, subject, level]
  type: timer
  
# Carga de trabajo de evaluadores
mtn.evaluators.workload:
  labels: [evaluator_id, subject]
  type: gauge
  
# SLA de evaluaciones
mtn.evaluation.sla_compliance:
  labels: [subject, level, exceeded]
  type: counter
```

### Notification Service - Métricas de Comunicación  
```yaml
# Emails enviados
mtn.notifications.emails_sent:
  labels: [template, delivery_status]
  type: counter
  
# Tiempo de entrega de notificaciones
mtn.notification.delivery_time:
  labels: [channel, template]
  type: timer
  
# Tasa de apertura de emails
mtn.notifications.email_open_rate:
  labels: [template]
  type: gauge
```

### Infrastructure Metrics
```yaml
# Eventos procesados por segundo
mtn.events.processed_per_second:
  labels: [service, event_type]
  type: counter
  
# Duración de Sagas
mtn.saga.duration:
  labels: [saga_type, outcome]
  type: timer
  
# Cola de eventos pendientes  
mtn.events.queue_depth:
  labels: [service, queue_name]
  type: gauge
```

## Distributed Tracing

### Configuración OpenTelemetry
```java
@Configuration
@EnableAutoConfiguration
public class TracingConfiguration {
    
    @Bean
    public OpenTelemetry openTelemetry() {
        return OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(BatchSpanProcessor.builder(
                        JaegerGrpcSpanExporter.builder()
                            .setEndpoint("http://jaeger:14250")
                            .build())
                        .build())
                    .setResource(Resource.getDefault()
                        .merge(Resource.create(
                            Attributes.of(ResourceAttributes.SERVICE_NAME, "mtn-microservice"))))
                    .build())
            .buildAndRegisterGlobal();
    }
}
```

### Spans Personalizados para Sagas
```java
@Component
public class AdmissionProcessingSaga {
    
    private final Tracer tracer = GlobalOpenTelemetry.getTracer("saga");
    
    @SagaOrchestrationStart
    public void processAdmission(ProcessAdmissionCommand command) {
        Span sagaSpan = tracer.spanBuilder("admission-processing-saga")
            .setAttribute("application.id", command.getApplicationId())
            .setAttribute("saga.type", "AdmissionProcessing")
            .startSpan();
            
        try (Scope scope = sagaSpan.makeCurrent()) {
            // Procesar steps de la saga con spans anidados
            processDocumentVerification(command);
            processEvaluationAssignment(command);
            processNotificationSending(command);
        } finally {
            sagaSpan.end();
        }
    }
    
    private void processDocumentVerification(ProcessAdmissionCommand command) {
        Span stepSpan = tracer.spanBuilder("document-verification-step")
            .setAttribute("application.id", command.getApplicationId())
            .startSpan();
            
        try (Scope scope = stepSpan.makeCurrent()) {
            // Lógica de verificación
        } catch (Exception e) {
            stepSpan.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            stepSpan.end();
        }
    }
}
```

## Structured Logging

### Configuración Logback
```xml
<!-- logback-spring.xml -->
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    
    <!-- Console Appender para desarrollo -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp/>
                <logLevel/>
                <loggerName/>
                <message/>
                <mdc/>
                <arguments/>
                <stackTrace/>
                <pattern>
                    <pattern>
                        {
                            "service": "evaluation-service",
                            "version": "1.0.0",
                            "environment": "${ENVIRONMENT:-development}"
                        }
                    </pattern>
                </pattern>
            </providers>
        </encoder>
    </appender>
    
    <!-- File Appender para producción -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/evaluation-service.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/evaluation-service.%d{yyyy-MM-dd}.%i.gz</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <!-- Misma configuración JSON que console -->
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

### Structured Logging en Código
```java
@Service
@Slf4j
public class EvaluationService {
    
    public Evaluation completeEvaluation(UUID evaluationId, EvaluationResult result, String userId) {
        // Structured logging con contexto rico
        MDC.put("evaluation.id", evaluationId.toString());
        MDC.put("user.id", userId);
        MDC.put("operation", "complete_evaluation");
        
        try {
            log.info("Starting evaluation completion", 
                kv("evaluationId", evaluationId),
                kv("userId", userId),
                kv("totalScore", result.getTotalScore()),
                kv("passed", result.isPassed()));
            
            Evaluation evaluation = completeEvaluationInternal(evaluationId, result, userId);
            
            log.info("Evaluation completed successfully",
                kv("evaluationId", evaluationId),
                kv("applicationId", evaluation.getApplicationId()),
                kv("processingTime", evaluation.getProcessingTimeMinutes()),
                kv("slaExceeded", evaluation.getSlaExceeded()));
                
            return evaluation;
            
        } catch (Exception e) {
            log.error("Failed to complete evaluation",
                kv("evaluationId", evaluationId),
                kv("userId", userId),
                kv("error", e.getMessage()),
                e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}
```

## Health Checks Avanzados

### Health Indicators Personalizados
```java
@Component("evaluationServiceHealth")
public class EvaluationServiceHealthIndicator implements HealthIndicator {
    
    private final EvaluationRepository evaluationRepository;
    private final OutboxProcessor outboxProcessor;
    
    @Override
    public Health health() {
        try {
            // Verificar conectividad a base de datos
            long pendingEvaluations = evaluationRepository.countByStatus(PENDING);
            
            // Verificar estado del outbox
            OutboxStatistics outboxStats = outboxProcessor.getOutboxStatistics();
            
            // Verificar servicios externos
            boolean applicationServiceUp = checkApplicationServiceHealth();
            
            Health.Builder builder = Health.up()
                .withDetail("pending_evaluations", pendingEvaluations)
                .withDetail("outbox_pending", outboxStats.getPendingEvents())
                .withDetail("application_service", applicationServiceUp ? "UP" : "DOWN");
            
            // Health check compuesto
            if (pendingEvaluations > 100) {
                builder = Health.down()
                    .withDetail("reason", "Too many pending evaluations");
            }
            
            if (!applicationServiceUp) {
                builder = Health.down()
                    .withDetail("reason", "Application service unavailable");
            }
            
            return builder.build();
            
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### Circuit Breaker Metrics
```java
@Component
public class CircuitBreakerMetrics {
    
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void onCircuitBreakerStateChange(CircuitBreakerOnStateTransitionEvent event) {
        Gauge.builder("circuit.breaker.state")
            .tag("circuit.breaker", event.getCircuitBreakerName())
            .tag("state", event.getStateTransition().getToState().name())
            .register(meterRegistry, cb -> 1.0);
        
        Counter.builder("circuit.breaker.transitions")
            .tag("circuit.breaker", event.getCircuitBreakerName())
            .tag("from", event.getStateTransition().getFromState().name())
            .tag("to", event.getStateTransition().getToState().name())
            .register(meterRegistry)
            .increment();
    }
    
    @EventListener
    public void onCircuitBreakerError(CircuitBreakerOnErrorEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        sample.stop(Timer.builder("circuit.breaker.error.duration")
            .tag("circuit.breaker", event.getCircuitBreakerName())
            .tag("error.type", event.getThrowable().getClass().getSimpleName())
            .register(meterRegistry));
    }
}
```

## Event Observability

### Event Metrics Collector
```java
@Component
@RequiredArgsConstructor
public class EventMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void onEventPublished(EventPublishedEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        Counter.builder("events.published")
            .tag("event.type", event.getEventType())
            .tag("source.service", event.getSource())
            .tag("success", String.valueOf(event.isSuccess()))
            .register(meterRegistry)
            .increment();
            
        if (event.isSuccess()) {
            sample.stop(Timer.builder("event.publishing.duration")
                .tag("event.type", event.getEventType())
                .register(meterRegistry));
        }
    }
    
    @EventListener
    public void onEventProcessed(EventProcessedEvent event) {
        Gauge.builder("events.processing.lag")
            .tag("event.type", event.getEventType())
            .tag("consumer.service", event.getConsumerService())
            .register(meterRegistry, e -> event.getProcessingLagMillis());
        
        Counter.builder("events.processed")
            .tag("event.type", event.getEventType())
            .tag("consumer.service", event.getConsumerService())
            .tag("success", String.valueOf(event.isSuccess()))
            .register(meterRegistry)
            .increment();
    }
}
```

### Saga Observability
```java
@Component
public class SagaMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void onSagaStarted(SagaStartedEvent event) {
        Counter.builder("sagas.started")
            .tag("saga.type", event.getSagaType())
            .register(meterRegistry)
            .increment();
    }
    
    @EventListener
    public void onSagaCompleted(SagaCompletedEvent event) {
        Timer.builder("saga.duration")
            .tag("saga.type", event.getSagaType())
            .tag("outcome", event.getOutcome().name())
            .register(meterRegistry)
            .record(event.getDuration(), TimeUnit.MILLISECONDS);
            
        Counter.builder("sagas.completed")
            .tag("saga.type", event.getSagaType())
            .tag("outcome", event.getOutcome().name())
            .register(meterRegistry)
            .increment();
    }
    
    @EventListener
    public void onSagaStepExecuted(SagaStepExecutedEvent event) {
        Timer.builder("saga.step.duration")
            .tag("saga.type", event.getSagaType())
            .tag("step.name", event.getStepName())
            .tag("success", String.valueOf(event.isSuccess()))
            .register(meterRegistry)
            .record(event.getStepDuration(), TimeUnit.MILLISECONDS);
    }
}
```

## Alerting y Monitoring

### Prometheus Rules
```yaml
# prometheus-rules.yml
groups:
- name: mtn-application-alerts
  rules:
  - alert: HighPendingApplications
    expr: mtn_applications_by_status{status="PENDING"} > 50
    for: 5m
    labels:
      severity: warning
      service: application-service
    annotations:
      summary: "High number of pending applications"
      description: "{{ $value }} applications are pending processing"

  - alert: EvaluationSLABreach
    expr: increase(mtn_evaluation_sla_compliance{exceeded="true"}[1h]) > 5
    for: 2m
    labels:
      severity: critical
      service: evaluation-service
    annotations:
      summary: "Evaluation SLA breaches detected"
      description: "{{ $value }} evaluations exceeded SLA in the last hour"

  - alert: CircuitBreakerOpen
    expr: circuit_breaker_state{state="OPEN"} == 1
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "Circuit breaker {{ $labels.circuit_breaker }} is OPEN"
      description: "Service {{ $labels.circuit_breaker }} is unavailable"

- name: mtn-infrastructure-alerts
  rules:
  - alert: EventProcessingLag
    expr: events_processing_lag > 30000  # 30 seconds
    for: 2m
    labels:
      severity: warning
    annotations:
      summary: "High event processing lag"
      description: "Event processing lag is {{ $value }}ms for {{ $labels.event_type }}"

  - alert: OutboxEventsStuck
    expr: increase(outbox_events_pending[10m]) == 0 and outbox_events_pending > 0
    for: 5m
    labels:
      severity: critical
    annotations:
      summary: "Outbox events are stuck"
      description: "{{ $value }} outbox events haven't been processed in 10 minutes"
```

### Grafana Dashboards

#### Dashboard de Métricas de Negocio
```json
{
  "dashboard": {
    "title": "MTN - Business Metrics",
    "panels": [
      {
        "title": "Applications by Status",
        "type": "stat",
        "targets": [
          {
            "expr": "sum by (status) (mtn_applications_by_status)"
          }
        ]
      },
      {
        "title": "Daily Application Submissions",
        "type": "graph",
        "targets": [
          {
            "expr": "increase(mtn_applications_submitted_total[24h])"
          }
        ]
      },
      {
        "title": "Evaluation Completion Rate",
        "type": "singlestat",
        "targets": [
          {
            "expr": "rate(mtn_evaluations_completed[1h]) * 3600"
          }
        ]
      }
    ]
  }
}
```

#### Dashboard Técnico
```json
{
  "dashboard": {
    "title": "MTN - Technical Metrics", 
    "panels": [
      {
        "title": "Event Processing Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(events_processed[5m])"
          }
        ]
      },
      {
        "title": "Saga Success Rate",
        "type": "stat", 
        "targets": [
          {
            "expr": "rate(sagas_completed{outcome=\"SUCCESS\"}[1h]) / rate(sagas_completed[1h]) * 100"
          }
        ]
      },
      {
        "title": "Service Response Times",
        "type": "heatmap",
        "targets": [
          {
            "expr": "rate(http_request_duration_seconds_bucket[5m])"
          }
        ]
      }
    ]
  }
}
```

## Log Correlation

### Request ID Propagation
```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter implements Filter {
    
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestId = httpRequest.getHeader(REQUEST_ID_HEADER);
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        httpResponse.setHeader(REQUEST_ID_HEADER, requestId);
        
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }
}
```

## Performance Monitoring

### JVM y Application Metrics
```java
@Configuration
public class MetricsConfiguration {
    
    @Bean
    public MeterRegistryCustomizer<PrometheusMeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
            .commonTags("application", "evaluation-service")
            .commonTags("version", getClass().getPackage().getImplementationVersion());
    }
    
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
    
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        Gauge.builder("application.ready.time")
            .register(Metrics.globalRegistry, 
                () -> ManagementFactory.getRuntimeMXBean().getUptime());
    }
}
```

## Deployment y Operations

### Docker Health Check
```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1
```

### Kubernetes Liveness/Readiness
```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      containers:
      - name: evaluation-service
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness  
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
```

Esta estrategia de observabilidad proporciona visibilidad completa en:
- **Performance de negocio**: Métricas de aplicaciones, evaluaciones, notificaciones
- **Salud técnica**: Disponibilidad de servicios, latencia, errores  
- **Operaciones de eventos**: Throughput, lag, fallos de procesamiento
- **Transacciones distribuidas**: Trazabilidad end-to-end de Sagas
- **Diagnóstico**: Logs estructurados con correlación completa