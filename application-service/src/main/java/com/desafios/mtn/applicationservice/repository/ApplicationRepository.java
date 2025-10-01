package com.desafios.mtn.applicationservice.repository;

import com.desafios.mtn.applicationservice.domain.Application;
import com.desafios.mtn.applicationservice.domain.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para entidad Application con consultas especializadas
 */
@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    // ================================
    // CONSULTAS POR STATUS
    // ================================

    /**
     * Encuentra aplicaciones por estado
     */
    List<Application> findByStatus(ApplicationStatus status);

    /**
     * Encuentra aplicaciones por estado con paginación
     */
    Page<Application> findByStatus(ApplicationStatus status, Pageable pageable);

    /**
     * Encuentra aplicaciones en múltiples estados
     */
    List<Application> findByStatusIn(List<ApplicationStatus> statuses);

    /**
     * Cuenta aplicaciones por estado
     */
    long countByStatus(ApplicationStatus status);

    /**
     * Encuentra aplicaciones que requieren acción del apoderado
     */
    @Query("SELECT a FROM Application a WHERE a.status IN ('DOCUMENTS_REQUESTED', 'APPROVED')")
    List<Application> findApplicationsRequiringParentAction();

    /**
     * Encuentra aplicaciones que requieren acción administrativa
     */
    @Query("SELECT a FROM Application a WHERE a.status IN ('PENDING', 'UNDER_REVIEW', 'INTERVIEW_SCHEDULED', 'EXAM_SCHEDULED')")
    List<Application> findApplicationsRequiringAdminAction();

    // ================================
    // CONSULTAS POR USUARIO
    // ================================

    /**
     * Encuentra aplicaciones creadas por un usuario
     */
    List<Application> findByCreatedBy(String createdBy);

    /**
     * Encuentra aplicaciones creadas por un usuario con paginación
     */
    Page<Application> findByCreatedBy(String createdBy, Pageable pageable);

    /**
     * Encuentra aplicación activa de un usuario (no terminal)
     */
    @Query("SELECT a FROM Application a WHERE a.createdBy = :createdBy AND a.status NOT IN ('REJECTED', 'ENROLLED', 'EXPIRED') ORDER BY a.createdAt DESC")
    Optional<Application> findActiveApplicationByUser(@Param("createdBy") String createdBy);

    // ================================
    // CONSULTAS TEMPORALES
    // ================================

    /**
     * Encuentra aplicaciones creadas en un rango de fechas
     */
    List<Application> findByCreatedAtBetween(Instant startDate, Instant endDate);

    /**
     * Encuentra aplicaciones enviadas en un rango de fechas
     */
    List<Application> findBySubmittedAtBetween(Instant startDate, Instant endDate);

    /**
     * Encuentra aplicaciones aprobadas en un rango de fechas
     */
    List<Application> findByApprovedAtBetween(Instant startDate, Instant endDate);

    /**
     * Encuentra aplicaciones sin actividad reciente
     */
    @Query("SELECT a FROM Application a WHERE a.updatedAt < :cutoffDate AND a.status NOT IN ('REJECTED', 'ENROLLED', 'EXPIRED')")
    List<Application> findStaleApplications(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Encuentra aplicaciones que expiran pronto (en estado APPROVED)
     */
    @Query("SELECT a FROM Application a WHERE a.status = 'APPROVED' AND a.approvedAt < :cutoffDate")
    List<Application> findApplicationsExpiringApprovals(@Param("cutoffDate") Instant cutoffDate);

    // ================================
    // CONSULTAS POR GRADO
    // ================================

    /**
     * Encuentra aplicaciones por grado aplicado
     */
    List<Application> findByGradeApplied(String grade);

    /**
     * Cuenta aplicaciones por grado y estado
     */
    @Query("SELECT COUNT(a) FROM Application a WHERE a.gradeApplied = :grade AND a.status = :status")
    long countByGradeAndStatus(@Param("grade") String grade, @Param("status") ApplicationStatus status);

    /**
     * Encuentra aplicaciones por grado con estados activos
     */
    @Query("SELECT a FROM Application a WHERE a.gradeApplied = :grade AND a.status NOT IN ('REJECTED', 'ENROLLED', 'EXPIRED')")
    List<Application> findActiveApplicationsByGrade(@Param("grade") String grade);

    // ================================
    // CONSULTAS ESPECIALES
    // ================================

    /**
     * Encuentra aplicaciones con necesidades especiales
     */
    List<Application> findBySpecialNeedsTrue();

    /**
     * Encuentra aplicaciones por canal de origen
     */
    List<Application> findBySourceChannel(String sourceChannel);

    /**
     * Encuentra aplicaciones duplicadas por RUT del postulante
     */
    @Query(value = "SELECT a.* FROM applications a WHERE JSON_EXTRACT_PATH_TEXT(a.applicant, 'rut') = :rut AND a.id != :excludeId", 
           nativeQuery = true)
    List<Application> findDuplicateApplicationsByRut(@Param("rut") String rut, @Param("excludeId") UUID excludeId);

    /**
     * Busca aplicaciones por texto en datos del postulante (PostgreSQL JSONB)
     */
    @Query(value = "SELECT a.* FROM applications a WHERE a.applicant::text ILIKE %:searchText% OR a.family_context::text ILIKE %:searchText%", 
           nativeQuery = true)
    List<Application> searchApplicationsByText(@Param("searchText") String searchText);

    // ================================
    // ESTADÍSTICAS Y REPORTES
    // ================================

    /**
     * Obtiene estadísticas por estado
     */
    @Query("SELECT a.status, COUNT(a) FROM Application a GROUP BY a.status")
    List<Object[]> getStatusStatistics();

    /**
     * Obtiene estadísticas por grado
     */
    @Query("SELECT a.gradeApplied, COUNT(a) FROM Application a GROUP BY a.gradeApplied ORDER BY a.gradeApplied")
    List<Object[]> getGradeStatistics();

    /**
     * Obtiene estadísticas mensuales de creación
     */
    @Query(value = "SELECT DATE_TRUNC('month', created_at) as month, COUNT(*) FROM applications GROUP BY month ORDER BY month DESC LIMIT 12", 
           nativeQuery = true)
    List<Object[]> getMonthlyCreationStatistics();

    /**
     * Obtiene tiempo promedio por estado
     */
    @Query(value = """
        SELECT 
            status,
            AVG(EXTRACT(EPOCH FROM (updated_at - created_at))/3600) as avg_hours
        FROM applications 
        WHERE status IN ('APPROVED', 'REJECTED', 'ENROLLED') 
        GROUP BY status
        """, nativeQuery = true)
    List<Object[]> getAverageProcessingTimeByStatus();

    /**
     * Encuentra aplicaciones con carga de trabajo alta por grado
     */
    @Query("""
        SELECT a.gradeApplied, COUNT(a) as pending_count 
        FROM Application a 
        WHERE a.status IN ('PENDING', 'UNDER_REVIEW', 'INTERVIEW_SCHEDULED', 'EXAM_SCHEDULED') 
        GROUP BY a.gradeApplied 
        HAVING COUNT(a) > :threshold 
        ORDER BY COUNT(a) DESC
        """)
    List<Object[]> findHighWorkloadGrades(@Param("threshold") long threshold);

    // ================================
    // CONSULTAS DE OPTIMIZACIÓN
    // ================================

    /**
     * Verifica si existe una aplicación activa para un usuario
     */
    @Query("SELECT COUNT(a) > 0 FROM Application a WHERE a.createdBy = :userId AND a.status NOT IN ('REJECTED', 'ENROLLED', 'EXPIRED')")
    boolean hasActiveApplication(@Param("userId") String userId);

    /**
     * Obtiene las aplicaciones más recientes para un usuario
     */
    @Query("SELECT a FROM Application a WHERE a.createdBy = :userId ORDER BY a.createdAt DESC")
    List<Application> findUserApplicationsOrderedByDate(@Param("userId") String userId, Pageable pageable);

    /**
     * Cuenta aplicaciones pendientes de revisión por antigüedad
     */
    @Query("SELECT COUNT(a) FROM Application a WHERE a.status = 'PENDING' AND a.createdAt < :cutoffDate")
    long countOldPendingApplications(@Param("cutoffDate") Instant cutoffDate);

    // ================================
    // CONSULTAS PARA NOTIFICACIONES
    // ================================

    /**
     * Encuentra aplicaciones que necesitan notificación de vencimiento
     */
    @Query("SELECT a FROM Application a WHERE a.status = 'APPROVED' AND a.approvedAt BETWEEN :startDate AND :endDate")
    List<Application> findApplicationsForExpirationNotification(@Param("startDate") Instant startDate, 
                                                               @Param("endDate") Instant endDate);

    /**
     * Encuentra aplicaciones sin actividad reciente para seguimiento
     */
    @Query("SELECT a FROM Application a WHERE a.updatedAt < :cutoffDate AND a.status IN ('PENDING', 'DOCUMENTS_REQUESTED', 'UNDER_REVIEW') ORDER BY a.updatedAt ASC")
    List<Application> findApplicationsForFollowUp(@Param("cutoffDate") Instant cutoffDate, Pageable pageable);
}