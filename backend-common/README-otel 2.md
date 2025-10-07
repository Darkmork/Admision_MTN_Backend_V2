# OpenTelemetry Setup - MTN Admission System

## Overview

Este documento describe la configuración completa de OpenTelemetry para trazas distribuidas en el sistema de admisión MTN, incluyendo propagación de contexto HTTP y AMQP con correlación de IDs.

## Configuración Spring Boot

### Dependencies (Maven)

```xml
<dependencies>
    <!-- Spring Boot OpenTelemetry Auto-instrumentation -->
    <dependency>
        <groupId>io.opentelemetry.instrumentation</groupId>
        <artifactId>opentelemetry-spring-boot-starter</artifactId>
        <version>1.32.0-alpha</version>
    </dependency>
    
    <!-- Micrometer with OpenTelemetry Bridge -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-tracing-bridge-otel</artifactId>
    </dependency>
    
    <!-- Jaeger Exporter -->
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-exporter-jaeger</artifactId>
    </dependency>
    
    <!-- OTLP Exporter (alternativa más moderna) -->
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-exporter-otlp</artifactId>
    </dependency>
    
    <!-- Context Propagation -->
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-extension-trace-propagators</artifactId>
    </dependency>
</dependencies>
```

### Application Configuration (application.yml)

```yaml
# OpenTelemetry Configuration
management:
  otlp:
    tracing:
      endpoint: "http://jaeger:4318/v1/traces"  # OTLP endpoint
      # endpoint: "http://jaeger:14268/api/traces"  # Legacy Jaeger endpoint
      compression: gzip
      timeout: 10s
      headers:
        Authorization: "Bearer ${JAEGER_AUTH_TOKEN:}"
  tracing:
    enabled: true
    sampling:
      probability: 0.1  # 10% sampling for production, 1.0 for development
    propagation:
      type: w3c  # W3C Trace Context propagation
    baggage:
      enabled: true
      correlation:
        enabled: true
        fields:
          - correlation_id
          - user_id
          - session_id

# OpenTelemetry Service Configuration
otel:
  service:
    name: ${spring.application.name}
    version: ${BUILD_VERSION:unknown}
  resource:
    attributes:
      service.name: ${spring.application.name}
      service.version: ${BUILD_VERSION:unknown}
      service.instance.id: ${POD_NAME:${random.uuid}}
      service.namespace: mtn-admission
      deployment.environment: ${ENVIRONMENT:production}
      k8s.namespace.name: ${POD_NAMESPACE:production}
      k8s.pod.name: ${POD_NAME:unknown}
      k8s.node.name: ${NODE_NAME:unknown}
  exporter:
    otlp:
      endpoint: "http://jaeger:4318"
      protocol: http/protobuf
      compression: gzip
      timeout: 10000ms
      headers:
        Authorization: "Bearer ${JAEGER_AUTH_TOKEN:}"
  propagators: w3c,baggage,jaeger,b3
  traces:
    exporter: otlp
    sampler: traceidratio
    sampler.arg: 0.1

# Logging with Trace Correlation
logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-},%X{correlationId:-}]"
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [${spring.application.name:},%X{traceId:-},%X{spanId:-},%X{correlationId:-}] %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [${spring.application.name:},%X{traceId:-},%X{spanId:-},%X{correlationId:-}] %logger{36} - %msg%n"
  level:
    io.opentelemetry: INFO
    io.micrometer.tracing: INFO
```

### Environment Variables

```bash
# OpenTelemetry Configuration
export OTEL_SERVICE_NAME="user-service"
export OTEL_SERVICE_VERSION="2.1.0"
export OTEL_EXPORTER_OTLP_ENDPOINT="http://jaeger:4318"
export OTEL_EXPORTER_OTLP_PROTOCOL="http/protobuf"
export OTEL_TRACES_SAMPLER="traceidratio"
export OTEL_TRACES_SAMPLER_ARG="0.1"
export OTEL_PROPAGATORS="w3c,baggage,jaeger,b3"

# Chilean timezone
export TZ="America/Santiago"

# Jaeger specific (legacy)
export JAEGER_ENDPOINT="http://jaeger:14268/api/traces"
export JAEGER_SAMPLER_TYPE="probabilistic"
export JAEGER_SAMPLER_PARAM="0.1"
```

## Configuración Manual de Instrumentación

### Trace Configuration Bean

```java
@Configuration
@EnableConfigurationProperties(TracingProperties.class)
public class OpenTelemetryConfig {

    @Bean
    public OpenTelemetry openTelemetry(@Value("${otel.exporter.otlp.endpoint}") String otlpEndpoint,
                                       @Value("${spring.application.name}") String serviceName,
                                       @Value("${BUILD_VERSION:unknown}") String serviceVersion) {
        
        Resource resource = Resource.getDefault()
                .merge(Resource.builder()
                        .put(ResourceAttributes.SERVICE_NAME, serviceName)
                        .put(ResourceAttributes.SERVICE_VERSION, serviceVersion)
                        .put(ResourceAttributes.SERVICE_NAMESPACE, "mtn-admission")
                        .put("deployment.environment", getEnvironment())
                        .build());

        return OpenTelemetrySdk.builder()
                .setTracerProvider(
                    SdkTracerProvider.builder()
                            .addSpanProcessor(BatchSpanProcessor.builder(
                                    OtlpGrpcSpanExporter.builder()
                                            .setEndpoint(otlpEndpoint)
                                            .setCompression("gzip")
                                            .build())
                                    .build())
                            .setResource(resource)
                            .setSampler(Sampler.traceIdRatioBased(0.1))
                            .build())
                .setMeterProvider(
                    SdkMeterProvider.builder()
                            .setResource(resource)
                            .build())
                .buildAndRegisterGlobal();
    }

    @Bean
    public ContextPropagators contextPropagators() {
        return ContextPropagators.create(
            TextMapPropagator.composite(
                W3CTraceContextPropagator.getInstance(),
                W3CBaggagePropagator.getInstance(),
                JaegerPropagator.getInstance(),
                B3Propagator.injectingSingleHeader()
            )
        );
    }
}
```

## Propagación de Contexto HTTP

### Automatic Propagation (Spring Boot Auto-configuration)

La propagación automática está habilitada para:
- RestTemplate
- WebClient
- Feign
- HttpClient

### Manual Propagation Filter

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceContextFilter implements Filter {

    private final ContextPropagators contextPropagators;
    private final Tracer tracer;

    public TraceContextFilter(OpenTelemetry openTelemetry) {
        this.contextPropagators = openTelemetry.getPropagators();
        this.tracer = openTelemetry.getTracer("mtn-admission-gateway");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Extract trace context from incoming request
        Context parentContext = contextPropagators.getTextMapPropagator()
                .extract(Context.current(), httpRequest, HttpServletRequestGetter.INSTANCE);

        // Generate or extract correlation ID
        String correlationId = Optional.ofNullable(httpRequest.getHeader("X-Correlation-Id"))
                .orElse(generateCorrelationId());

        // Add correlation ID to baggage
        Context contextWithBaggage = parentContext.with(
                Baggage.current().toBuilder()
                        .put("correlation_id", correlationId)
                        .put("user_id", extractUserId(httpRequest))
                        .put("request_id", generateRequestId())
                        .build()
        );

        // Create span for the request
        Span span = tracer.spanBuilder("http_request")
                .setParent(contextWithBaggage)
                .setAttribute("http.method", httpRequest.getMethod())
                .setAttribute("http.url", httpRequest.getRequestURL().toString())
                .setAttribute("http.user_agent", httpRequest.getHeader("User-Agent"))
                .setAttribute("correlation.id", correlationId)
                .startSpan();

        // Add correlation ID to response headers
        httpResponse.setHeader("X-Correlation-Id", correlationId);
        httpResponse.setHeader("X-Trace-Id", span.getSpanContext().getTraceId());

        // Add to MDC for logging
        try (MDCCloseable mdcCloseable = MDC.putCloseable("correlationId", correlationId)) {
            try (Scope scope = span.makeCurrent()) {
                chain.doFilter(request, response);
                
                // Set response attributes
                span.setAttribute("http.status_code", httpResponse.getStatus());
                if (httpResponse.getStatus() >= 400) {
                    span.setStatus(StatusCode.ERROR, "HTTP " + httpResponse.getStatus());
                }
            }
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    private String generateCorrelationId() {
        return "mtn-" + System.currentTimeMillis() + "-" + 
               ThreadLocalRandom.current().nextInt(10000, 99999);
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString();
    }

    private String extractUserId(HttpServletRequest request) {
        // Extract from JWT token or session
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                // Parse JWT and extract user ID
                return JwtUtils.extractUserId(authorization.substring(7));
            } catch (Exception e) {
                return "anonymous";
            }
        }
        return "anonymous";
    }
}
```

### HTTP Client Instrumentation

```java
@Configuration
public class HttpClientConfig {

    @Bean
    public RestTemplate restTemplate(OpenTelemetry openTelemetry) {
        RestTemplate restTemplate = new RestTemplate();
        
        // Add trace propagation interceptor
        restTemplate.getInterceptors().add(new TraceContextPropagationInterceptor(openTelemetry));
        
        return restTemplate;
    }

    @Bean
    public WebClient webClient(OpenTelemetry openTelemetry) {
        return WebClient.builder()
                .filter(new TraceContextWebClientFilter(openTelemetry))
                .build();
    }
}

// Interceptor for RestTemplate
public class TraceContextPropagationInterceptor implements ClientHttpRequestInterceptor {
    
    private final TextMapPropagator propagator;

    public TraceContextPropagationInterceptor(OpenTelemetry openTelemetry) {
        this.propagator = openTelemetry.getPropagators().getTextMapPropagator();
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                       ClientHttpRequestExecution execution) throws IOException {
        
        // Inject current trace context into outgoing request
        propagator.inject(Context.current(), request, HttpHeaderSetter.INSTANCE);
        
        // Add correlation ID if present in baggage
        Baggage.current().asMap().forEach((key, baggageEntry) -> {
            if ("correlation_id".equals(key)) {
                request.getHeaders().add("X-Correlation-Id", baggageEntry.getValue());
            }
        });

        return execution.execute(request, body);
    }
}
```

## Propagación AMQP (RabbitMQ)

### Message Producer with Trace Context

```java
@Component
public class TracedEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final TextMapPropagator propagator;
    private final Tracer tracer;

    public TracedEventPublisher(RabbitTemplate rabbitTemplate, OpenTelemetry openTelemetry) {
        this.rabbitTemplate = rabbitTemplate;
        this.propagator = openTelemetry.getPropagators().getTextMapPropagator();
        this.tracer = openTelemetry.getTracer("mtn-admission-messaging");
    }

    public void publishEvent(String exchange, String routingKey, Object event) {
        
        Span span = tracer.spanBuilder("message_publish")
                .setAttribute("messaging.system", "rabbitmq")
                .setAttribute("messaging.destination", exchange)
                .setAttribute("messaging.destination_kind", "topic")
                .setAttribute("messaging.rabbitmq.routing_key", routingKey)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            
            // Create message with trace context
            MessageProperties properties = new MessageProperties();
            
            // Inject trace context into AMQP headers
            propagator.inject(Context.current(), properties, new MessagePropertiesTextMapSetter());
            
            // Add correlation ID
            String correlationId = Baggage.current().getEntryValue("correlation_id");
            if (correlationId == null) {
                correlationId = generateCorrelationId();
            }
            properties.setCorrelationId(correlationId);
            properties.setMessageId(generateMessageId());
            properties.setTimestamp(new Date());
            
            // Add custom headers for better tracing
            properties.setHeader("trace_id", span.getSpanContext().getTraceId());
            properties.setHeader("span_id", span.getSpanContext().getSpanId());
            properties.setHeader("correlation_id", correlationId);
            properties.setHeader("source_service", getServiceName());
            properties.setHeader("event_type", event.getClass().getSimpleName());

            // Serialize event
            EventEnvelope envelope = new EventEnvelope(event, correlationId);
            String messageBody = objectMapper.writeValueAsString(envelope);

            Message message = new Message(messageBody.getBytes(), properties);
            
            span.setAttribute("messaging.message_id", properties.getMessageId());
            span.setAttribute("messaging.correlation_id", correlationId);
            
            rabbitTemplate.send(exchange, routingKey, message);
            
            log.info("Published event: {} to exchange: {} with correlation: {}", 
                    event.getClass().getSimpleName(), exchange, correlationId);
            
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw new EventPublishingException("Failed to publish event", e);
        } finally {
            span.end();
        }
    }
}

// Text map setter for AMQP message properties
public static class MessagePropertiesTextMapSetter implements TextMapSetter<MessageProperties> {
    @Override
    public void set(MessageProperties carrier, String key, String value) {
        if (carrier != null) {
            carrier.setHeader(key, value);
        }
    }
}
```

### Message Consumer with Trace Context

```java
@RabbitListener(queues = "admissions.queue")
@Component
public class TracedEventConsumer {

    private final TextMapPropagator propagator;
    private final Tracer tracer;

    public TracedEventConsumer(OpenTelemetry openTelemetry) {
        this.propagator = openTelemetry.getPropagators().getTextMapPropagator();
        this.tracer = openTelemetry.getTracer("mtn-admission-messaging");
    }

    @RabbitHandler
    public void handleApplicationEvent(Message message) {
        
        // Extract trace context from AMQP message
        Context parentContext = propagator.extract(Context.current(), 
                message.getMessageProperties(), new MessagePropertiesTextMapGetter());

        String correlationId = message.getMessageProperties().getCorrelationId();
        String messageId = message.getMessageProperties().getMessageId();

        Span span = tracer.spanBuilder("message_consume")
                .setParent(parentContext)
                .setAttribute("messaging.system", "rabbitmq")
                .setAttribute("messaging.operation", "receive")
                .setAttribute("messaging.message_id", messageId)
                .setAttribute("messaging.correlation_id", correlationId)
                .startSpan();

        // Add correlation ID to baggage for downstream operations
        Context contextWithBaggage = Context.current().with(
                Baggage.current().toBuilder()
                        .put("correlation_id", correlationId)
                        .build()
        );

        try (MDCCloseable mdcCloseable = MDC.putCloseable("correlationId", correlationId)) {
            try (Scope scope = span.makeCurrent()) {
                
                // Process the message
                String messageBody = new String(message.getBody());
                EventEnvelope envelope = objectMapper.readValue(messageBody, EventEnvelope.class);
                
                span.setAttribute("event.type", envelope.getEventType());
                span.setAttribute("event.correlation_id", envelope.getCorrelationId());
                
                // Process the event
                processEvent(envelope);
                
                log.info("Processed event: {} with correlation: {}", 
                        envelope.getEventType(), correlationId);
                
            } catch (Exception e) {
                span.recordException(e);
                span.setStatus(StatusCode.ERROR, e.getMessage());
                
                // Send to DLQ or retry logic
                handleProcessingError(message, e);
                
                throw new EventProcessingException("Failed to process event", e);
            }
        } finally {
            span.end();
        }
    }

    // Text map getter for AMQP message properties
    public static class MessagePropertiesTextMapGetter implements TextMapGetter<MessageProperties> {
        @Override
        public Iterable<String> keys(MessageProperties carrier) {
            return carrier.getHeaders().keySet().stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }

        @Override
        public String get(MessageProperties carrier, String key) {
            Object value = carrier.getHeaders().get(key);
            return value != null ? value.toString() : null;
        }
    }
}
```

### Event Envelope for Correlation

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventEnvelope {
    
    private String eventId;
    private String eventType;
    private String correlationId;
    private String traceId;
    private String spanId;
    private Instant timestamp;
    private String sourceService;
    private Object payload;
    
    public EventEnvelope(Object payload, String correlationId) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = payload.getClass().getSimpleName();
        this.correlationId = correlationId;
        this.timestamp = Instant.now();
        this.sourceService = getServiceName();
        this.payload = payload;
        
        // Extract current trace context
        SpanContext spanContext = Span.current().getSpanContext();
        if (spanContext.isValid()) {
            this.traceId = spanContext.getTraceId();
            this.spanId = spanContext.getSpanId();
        }
    }
}
```

## Instrumentación Personalizada

### Manual Span Creation

```java
@Service
public class ApplicationService {

    private final Tracer tracer;

    public ApplicationService(OpenTelemetry openTelemetry) {
        this.tracer = openTelemetry.getTracer("mtn-application-service");
    }

    @TraceAsync
    public CompletableFuture<Application> processApplication(ApplicationRequest request) {
        
        Span span = tracer.spanBuilder("process_application")
                .setAttribute("application.id", request.getId())
                .setAttribute("application.type", request.getType())
                .setAttribute("student.grade", request.getStudentGrade())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            
            String correlationId = Baggage.current().getEntryValue("correlation_id");
            span.setAttribute("correlation.id", correlationId);

            // Add application-specific attributes
            span.setAttribute("student.rut", maskRut(request.getStudentRut()));
            span.setAttribute("school.target", request.getTargetSchool());
            
            // Process application steps with child spans
            validateApplication(request);
            checkDocuments(request);
            scheduleEvaluation(request);
            
            Application result = createApplication(request);
            
            span.setAttribute("application.result.id", result.getId());
            span.setStatus(StatusCode.OK);
            
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    private void validateApplication(ApplicationRequest request) {
        Span childSpan = tracer.spanBuilder("validate_application")
                .startSpan();
        
        try (Scope scope = childSpan.makeCurrent()) {
            // Validation logic
            childSpan.setAttribute("validation.result", "passed");
        } catch (ValidationException e) {
            childSpan.recordException(e);
            childSpan.setStatus(StatusCode.ERROR, "Validation failed");
            throw e;
        } finally {
            childSpan.end();
        }
    }
}
```

## Testing de Trazas

### Test Configuration

```java
@TestConfiguration
public class TestTracingConfig {

    @Bean
    @Primary
    public OpenTelemetry testOpenTelemetry() {
        return OpenTelemetry.noop();
    }
}

@Test
@ExtendWith(MockitoExtension.class)
class TracingIntegrationTest {

    @Test
    void shouldPropagateTraceContext() {
        // Given
        SpanBuilder spanBuilder = mock(SpanBuilder.class);
        Span span = mock(Span.class);
        
        when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
        
        // When
        applicationService.processApplication(request);
        
        // Then
        verify(span).setAttribute("application.id", request.getId());
        verify(span).end();
    }
}
```

## Monitoring y Troubleshooting

### Trace Sampling Strategies

```yaml
# Dynamic sampling based on service health
otel:
  traces:
    sampler: composite
    samplers:
      - name: error_sampler
        type: traceidratio
        arg: 1.0  # Sample all error traces
        condition: "span.status == ERROR"
      
      - name: slow_request_sampler
        type: traceidratio
        arg: 1.0  # Sample all slow requests
        condition: "span.duration > 2s"
      
      - name: default_sampler
        type: traceidratio
        arg: 0.1  # 10% sampling for normal requests
```

### Health Checks for Tracing

```java
@Component
@ConditionalOnProperty("management.tracing.enabled")
public class TracingHealthIndicator implements HealthIndicator {

    private final OpenTelemetry openTelemetry;
    
    @Override
    public Health health() {
        try {
            // Test trace creation
            Span testSpan = openTelemetry.getTracer("health-check")
                    .spanBuilder("health_check")
                    .startSpan();
            testSpan.end();
            
            return Health.up()
                    .withDetail("tracing", "active")
                    .withDetail("exporter", "connected")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("tracing", "failed")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
```

Esta configuración proporciona trazabilidad completa end-to-end para el sistema de admisión MTN con propagación de contexto tanto en HTTP como en AMQP, incluyendo correlación de IDs para facilitar la resolución de problemas y el monitoreo.