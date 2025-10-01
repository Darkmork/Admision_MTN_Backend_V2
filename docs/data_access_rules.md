# Reglas de Acceso a Datos Cross-Service

## Principios Fundamentales

### 1. Prohibición Absoluta de Foreign Keys Cross-Database
```sql
-- ❌ PROHIBIDO: References cross-database
ALTER TABLE evaluations_db.evaluations 
ADD CONSTRAINT fk_application 
FOREIGN KEY (application_id) REFERENCES admision_mtn_db.applications(id);

-- ✅ PERMITIDO: Solo referencias internas
ALTER TABLE evaluations_db.evaluations 
ADD CONSTRAINT fk_outbox 
FOREIGN KEY (outbox_id) REFERENCES evaluations_db.outbox(id);
```

### 2. Acceso Cross-Service Únicamente por API
```java
// ❌ PROHIBIDO: Acceso directo a DB externa
@Query("SELECT a FROM Application a WHERE a.id = ?1", nativeQuery = true)
Application getApplicationFromExternalDB(UUID id);

// ✅ PERMITIDO: Acceso via client API
@Service
public class ApplicationClient {
    private final RestTemplate restTemplate;
    
    public ApplicationSummary getApplication(UUID id) {
        return restTemplate.getForObject(
            "http://application-service/api/applications/{id}/summary", 
            ApplicationSummary.class, 
            id
        );
    }
}
```

## Patrones de Acceso Permitidos

### 1. API Composition Pattern
Para consultas que requieren datos de múltiples servicios:

```java
@Service
@RequiredArgsConstructor
public class EvaluationCompositionService {
    
    private final EvaluationRepository evaluationRepository;
    private final ApplicationServiceClient applicationClient;
    private final UserServiceClient userClient;
    
    public CompleteEvaluationView getCompleteEvaluation(UUID evaluationId) {
        // 1. Datos propios (evaluation-service)
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
            .orElseThrow(() -> new EntityNotFoundException("Evaluation not found"));
        
        // 2. Datos externos via API clients
        ApplicationSummary application = applicationClient.getApplication(evaluation.getApplicationId());
        UserProfile evaluator = userClient.getUserProfile(evaluation.getEvaluatorId());
        
        // 3. Composición final
        return CompleteEvaluationView.builder()
            .evaluation(evaluation)
            .application(application)
            .evaluator(evaluator)
            .build();
    }
}
```

### 2. Event-Driven Projections (CQRS Ligero)
Para datos frecuentemente consultados:

```java
@Entity
@Table(name = "application_summaries")
public class ApplicationSummary {
    @Id
    private UUID applicationId;
    private String studentName;
    private String educationalLevel;
    private LocalDate submissionDate;
    private String currentStatus;
    private Instant lastUpdated;
    
    // Actualizada automáticamente via eventos
}

@EventListener
@Component
public class ApplicationSummaryProjector {
    
    @RabbitListener(queues = "evaluations.application-events.queue")
    public void on(ApplicationSubmittedEvent event) {
        ApplicationSummary summary = ApplicationSummary.builder()
            .applicationId(event.getApplicationId())
            .studentName(event.getStudentName())
            .educationalLevel(event.getEducationalLevel())
            .submissionDate(event.getSubmissionDate())
            .currentStatus(event.getStatus())
            .lastUpdated(Instant.now())
            .build();
            
        applicationSummaryRepository.save(summary);
    }
    
    @RabbitListener(queues = "evaluations.application-state-changed.queue")
    public void on(ApplicationStateChangedEvent event) {
        applicationSummaryRepository.findById(event.getApplicationId())
            .ifPresent(summary -> {
                summary.setCurrentStatus(event.getNewStatus());
                summary.setLastUpdated(Instant.now());
                applicationSummaryRepository.save(summary);
            });
    }
}
```

### 3. Caché con Invalidación Inteligente
Para datos semi-estáticos con invalidación por eventos:

```java
@Service
public class UserProfileCacheService {
    
    private final UserServiceClient userServiceClient;
    private final Cache<String, UserProfile> userProfileCache;
    
    @Cacheable(value = "user-profiles", key = "#userId")
    public UserProfile getUserProfile(String userId) {
        return userServiceClient.getUserProfile(userId)
            .orElse(UserProfile.anonymous());
    }
    
    @EventListener
    @RabbitListener(queues = "evaluations.user-updated.queue")
    public void on(UserProfileUpdatedEvent event) {
        userProfileCache.evict(event.getUserId());
        log.info("Evicted user profile from cache: {}", event.getUserId());
    }
}
```

## Reglas por Tipo de Operación

### Cross-Service READS

#### ✅ Lectura Inmediata (API Calls)
```java
// Ejemplo: Obtener datos de aplicación para mostrar en evaluación
@GetMapping("/evaluations/{id}/with-application")
public EvaluationWithApplicationResponse getEvaluationWithApplication(@PathVariable UUID id) {
    Evaluation evaluation = evaluationService.getById(id);
    ApplicationSummary application = applicationClient.getApplication(evaluation.getApplicationId());
    
    return EvaluationWithApplicationResponse.combine(evaluation, application);
}
```

#### ✅ Lectura Diferida (Event Projections)
```java
// Ejemplo: Dashboard con estadísticas
@GetMapping("/dashboard/statistics")
public DashboardStatistics getDashboardStatistics() {
    // Usa proyecciones locales - no requiere llamadas API
    return DashboardStatistics.builder()
        .totalEvaluations(evaluationRepository.count())
        .pendingApplications(applicationSummaryRepository.countByStatus("PENDING"))
        .activeEvaluators(userProfileRepository.countByRoleAndActive("TEACHER", true))
        .build();
}
```

### Cross-Service WRITES

#### ✅ Escritura Asíncrona (Eventos)
```java
// Ejemplo: Completar evaluación actualiza estado de aplicación
@Transactional
public Evaluation completeEvaluation(UUID evaluationId, EvaluationResult result, String userId) {
    Evaluation evaluation = getEvaluationById(evaluationId);
    evaluation.complete(result, userId);
    
    evaluationRepository.save(evaluation);
    
    // Publicar evento para actualizar aplicación
    EvaluationCompletedEvent event = EvaluationCompletedEvent.builder()
        .evaluationId(evaluation.getId())
        .applicationId(evaluation.getApplicationId())
        .result(result)
        .completedBy(userId)
        .completedAt(Instant.now())
        .build();
    
    eventPublisher.publishEvent(event);
    return evaluation;
}
```

#### ❌ Escritura Directa Cross-DB
```java
// ❌ PROHIBIDO: Update directo en DB externa
@Query(value = "UPDATE admision_mtn_db.applications SET status = ?2 WHERE id = ?1", nativeQuery = true)
void updateApplicationStatusDirectly(UUID applicationId, String status);
```

### Escritura Síncrona (Sagas Orchestradas)
Para operaciones que requieren consistencia inmediata:

```java
@Component
public class AdmissionEvaluationSaga {
    
    @SagaOrchestrationStart
    public void handle(CompleteAdmissionEvaluationCommand command) {
        sagaManager.choreography()
            .step("complete-evaluation")
                .invoke(() -> evaluationService.completeEvaluation(command.getEvaluationId()))
                .compensate(() -> evaluationService.reopenEvaluation(command.getEvaluationId()))
            .step("update-application-status")
                .invoke(() -> applicationServiceClient.updateStatus(command.getApplicationId(), "EVALUATION_COMPLETED"))
                .compensate(() -> applicationServiceClient.updateStatus(command.getApplicationId(), "UNDER_REVIEW"))
            .step("send-notification")
                .invoke(() -> notificationServiceClient.sendEvaluationComplete(command.getApplicationId()))
                .compensate(() -> notificationServiceClient.cancelNotification(command.getNotificationId()))
            .execute();
    }
}
```

## Implementación de Data Access Layer

### Client Service Template
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationServiceClient {
    
    private final RestTemplate restTemplate;
    private final CircuitBreaker circuitBreaker;
    
    @Value("${services.application.base-url:http://application-service}")
    private String baseUrl;
    
    public Optional<ApplicationSummary> getApplication(UUID applicationId) {
        return circuitBreaker.executeSupplier(() -> {
            try {
                ApplicationSummary summary = restTemplate.getForObject(
                    baseUrl + "/api/applications/{id}/summary",
                    ApplicationSummary.class,
                    applicationId
                );
                return Optional.ofNullable(summary);
            } catch (RestClientException e) {
                log.warn("Failed to fetch application {}: {}", applicationId, e.getMessage());
                return Optional.empty();
            }
        });
    }
    
    @Retryable(value = {RestClientException.class}, maxAttempts = 3)
    public void updateApplicationStatus(UUID applicationId, String newStatus) {
        UpdateApplicationStatusRequest request = UpdateApplicationStatusRequest.builder()
            .newStatus(newStatus)
            .updatedBy("evaluation-service")
            .build();
            
        restTemplate.put(
            baseUrl + "/api/applications/{id}/status",
            request,
            applicationId
        );
    }
}
```

### Repository Access Control
```java
@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {
    
    // ✅ PERMITIDO: Consultas internas
    @Query("SELECT e FROM Evaluation e WHERE e.status = :status")
    List<Evaluation> findByStatus(@Param("status") EvaluationStatus status);
    
    // ✅ PERMITIDO: Joins internos
    @Query("SELECT e FROM Evaluation e JOIN e.outboxEvents o WHERE o.status = 'PENDING'")
    List<Evaluation> findWithPendingEvents();
    
    // ❌ PROHIBIDO: Referencias a otras bases
    // @Query(value = "SELECT e.*, a.student_name FROM evaluations e " +
    //               "JOIN admision_mtn_db.applications a ON e.application_id = a.id", nativeQuery = true)
    // List<Object[]> findEvaluationsWithApplicationData();
}
```

## Validación en Tiempo de Compilación

### ArchUnit Rules
```java
@AnalyzeClasses(packages = "com.desafios.mtn.evaluationservice")
public class DataAccessArchitectureTest {
    
    @ArchTest
    static final ArchRule no_cross_database_access = 
        noClasses()
            .that().resideInAPackage("..repository..")
            .should().accessClassesThat().resideInAnyPackage(
                "com.desafios.mtn.applicationservice.entity..",
                "com.desafios.mtn.notificationservice.entity.."
            );
    
    @ArchTest
    static final ArchRule no_direct_entity_access =
        noClasses()
            .that().resideInAPackage("..service..")
            .should().accessClassesThat()
                .resideInAnyPackage("..external..entity..")
                .andShould().haveSimpleNameNotContaining("Client");
                
    @ArchTest
    static final ArchRule only_service_layer_calls_external_apis =
        classes()
            .that().haveSimpleNameEndingWith("Client")
            .should().onlyBeAccessed().byClassesThat()
                .resideInAnyPackage("..service..", "..saga..");
}
```

### Database Schema Validation
```java
@Component
public class DatabaseSchemaValidator {
    
    @Autowired
    private EntityManager entityManager;
    
    @PostConstruct
    public void validateNoCrossDatabaseForeignKeys() {
        Query query = entityManager.createNativeQuery(
            "SELECT tc.constraint_name, tc.table_name, kcu.column_name, " +
            "ccu.table_name AS foreign_table_name " +
            "FROM information_schema.table_constraints AS tc " +
            "JOIN information_schema.key_column_usage AS kcu " +
            "ON tc.constraint_name = kcu.constraint_name " +
            "JOIN information_schema.constraint_column_usage AS ccu " +
            "ON ccu.constraint_name = tc.constraint_name " +
            "WHERE constraint_type = 'FOREIGN KEY' " +
            "AND ccu.table_schema != current_schema()"
        );
        
        List<Object[]> crossDbForeignKeys = query.getResultList();
        
        if (!crossDbForeignKeys.isEmpty()) {
            throw new IllegalStateException(
                "Found cross-database foreign keys: " + crossDbForeignKeys
            );
        }
    }
}
```

## Métricas y Monitoreo

### Métricas de Acceso Cross-Service
```java
@Component
@RequiredArgsConstructor
public class DataAccessMetrics {
    
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void onCrossServiceCall(CrossServiceCallEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        sample.stop(Timer.builder("cross.service.call.duration")
            .tag("source.service", event.getSourceService())
            .tag("target.service", event.getTargetService())
            .tag("operation", event.getOperation())
            .tag("success", String.valueOf(event.isSuccess()))
            .register(meterRegistry));
    }
    
    @EventListener 
    public void onProjectionUpdate(ProjectionUpdateEvent event) {
        Counter.builder("projection.updates")
            .tag("projection.type", event.getProjectionType())
            .tag("source.event", event.getSourceEvent())
            .register(meterRegistry)
            .increment();
    }
}
```

## Casos de Uso Específicos

### Caso 1: Dashboard de Evaluaciones Completo
```java
@GetMapping("/dashboard")
public EvaluationDashboardResponse getEvaluationDashboard() {
    // Datos locales (rápidos)
    List<Evaluation> myEvaluations = evaluationService.getMyEvaluations();
    
    // Datos compuestos (API calls en paralelo)
    CompletableFuture<Map<UUID, ApplicationSummary>> applicationsFuture = 
        CompletableFuture.supplyAsync(() -> 
            applicationClient.getApplicationSummaries(
                myEvaluations.stream().map(Evaluation::getApplicationId).toList()
            )
        );
    
    // Datos proyectados (locales pero actualizados por eventos)
    List<UserProfile> evaluatorProfiles = userProfileRepository.findAll();
    
    return EvaluationDashboardResponse.builder()
        .evaluations(myEvaluations)
        .applications(applicationsFuture.join())
        .evaluators(evaluatorProfiles)
        .build();
}
```

### Caso 2: Asignación Automática de Evaluaciones
```java
@Transactional
public List<Evaluation> assignPendingEvaluationsAutomatically() {
    List<Evaluation> pendingEvaluations = evaluationRepository.findByStatus(PENDING);
    
    // Usar proyecciones locales para carga de evaluadores
    Map<String, Integer> evaluatorWorkload = userProfileRepository.getEvaluatorWorkload();
    
    List<Evaluation> assignedEvaluations = new ArrayList<>();
    
    for (Evaluation evaluation : pendingEvaluations) {
        String bestEvaluator = findBestEvaluatorFor(evaluation.getSubject(), evaluation.getLevel(), evaluatorWorkload);
        
        if (bestEvaluator != null) {
            evaluation.assign(bestEvaluator, AssignmentReason.AUTOMATIC, "system");
            evaluationRepository.save(evaluation);
            assignedEvaluations.add(evaluation);
            
            // Actualizar carga local
            evaluatorWorkload.merge(bestEvaluator, 1, Integer::sum);
        }
    }
    
    return assignedEvaluations;
}
```

### Caso 3: Sincronización de Estados via Eventos
```java
@RabbitListener(queues = "evaluations.application-state-changed.queue")
public void onApplicationStateChanged(ApplicationStateChangedEvent event) {
    if (event.getNewStatus().equals("CANCELLED")) {
        // Cancelar evaluaciones relacionadas
        List<Evaluation> relatedEvaluations = 
            evaluationRepository.findByApplicationIdAndStatusIn(
                event.getApplicationId(),
                List.of(PENDING, ASSIGNED, IN_PROGRESS)
            );
        
        for (Evaluation evaluation : relatedEvaluations) {
            evaluation.cancel("Application cancelled", "system");
            evaluationRepository.save(evaluation);
        }
        
        log.info("Cancelled {} evaluations for application {}", 
                relatedEvaluations.size(), event.getApplicationId());
    }
}
```

## Troubleshooting y Debugging

### Herramientas de Diagnóstico
```java
@RestController
@RequestMapping("/api/diagnostics")
@PreAuthorize("hasRole('ADMIN')")
public class DataAccessDiagnosticsController {
    
    @GetMapping("/cross-service-health")
    public Map<String, Object> getCrossServiceHealth() {
        Map<String, Object> health = new HashMap<>();
        
        // Test application service connectivity
        try {
            applicationClient.healthCheck();
            health.put("application-service", "UP");
        } catch (Exception e) {
            health.put("application-service", "DOWN: " + e.getMessage());
        }
        
        // Test projection freshness
        Instant oldestProjection = applicationSummaryRepository.findOldestLastUpdated();
        health.put("projection-freshness", Duration.between(oldestProjection, Instant.now()).toMinutes() + " minutes");
        
        return health;
    }
    
    @GetMapping("/orphaned-data")
    public List<String> findOrphanedData() {
        // Buscar evaluaciones con application_id que no existe en proyecciones
        List<UUID> evaluationAppIds = evaluationRepository.findAllApplicationIds();
        List<UUID> projectionAppIds = applicationSummaryRepository.findAllApplicationIds();
        
        List<UUID> orphaned = evaluationAppIds.stream()
            .filter(id -> !projectionAppIds.contains(id))
            .toList();
        
        return orphaned.stream()
            .map(UUID::toString)
            .toList();
    }
}
```

### Logs Estructurados para Cross-Service Calls
```java
@Aspect
@Component
@Slf4j
public class CrossServiceCallLoggingAspect {
    
    @Around("@annotation(crossServiceCall)")
    public Object logCrossServiceCall(ProceedingJoinPoint joinPoint, CrossServiceCall crossServiceCall) throws Throwable {
        String targetService = crossServiceCall.targetService();
        String operation = crossServiceCall.operation();
        
        MDC.put("cross_service_target", targetService);
        MDC.put("cross_service_operation", operation);
        
        Instant start = Instant.now();
        
        try {
            Object result = joinPoint.proceed();
            
            Duration duration = Duration.between(start, Instant.now());
            log.info("Cross-service call successful: {} -> {} [{}ms]", 
                    "evaluation-service", targetService, duration.toMillis());
            
            return result;
        } catch (Exception e) {
            Duration duration = Duration.between(start, Instant.now());
            log.error("Cross-service call failed: {} -> {} [{}ms]: {}", 
                     "evaluation-service", targetService, duration.toMillis(), e.getMessage());
            throw e;
        } finally {
            MDC.remove("cross_service_target");
            MDC.remove("cross_service_operation");
        }
    }
}
```

## Checklist de Implementación

### ✅ Validación de Reglas
- [ ] No foreign keys cross-database
- [ ] Solo acceso via API clients
- [ ] Proyecciones actualizadas por eventos
- [ ] Caché invalidado correctamente
- [ ] Sagas para operaciones críticas
- [ ] Métricas de cross-service calls
- [ ] Health checks para servicios externos
- [ ] ArchUnit tests implementados

### ✅ Performance
- [ ] Proyecciones para datos frecuentes
- [ ] Caché para datos semi-estáticos
- [ ] Circuit breakers en API clients
- [ ] Llamadas asíncronas cuando sea posible
- [ ] Batch APIs para múltiples requests

### ✅ Resiliencia
- [ ] Retry policies configuradas
- [ ] Timeouts apropiados
- [ ] Fallbacks para servicios críticos
- [ ] Dead letter queues para eventos fallidos
- [ ] Monitoreo de proyecciones obsoletas