package com.desafios.mtn.evaluationservice.service;

import com.desafios.mtn.evaluationservice.domain.Interview;
import com.desafios.mtn.evaluationservice.domain.Interview.*;
import com.desafios.mtn.evaluationservice.repository.InterviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de dominio para la gestión de entrevistas de admisión
 * Implementa la lógica de negocio para el proceso completo de entrevistas
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final EvaluationEventPublisher evaluationEventPublisher;
    private final InterviewSchedulingService interviewSchedulingService;

    // ================================
    // CORE INTERVIEW OPERATIONS
    // ================================

    /**
     * Programa una nueva entrevista
     */
    public Interview scheduleInterview(UUID applicationId, String interviewerId, 
                                     InterviewType type, Instant scheduledAt, 
                                     Integer durationMinutes, String location, 
                                     String createdBy) {
        
        log.info("Scheduling {} for application {} with interviewer {} at {}", 
                type, applicationId, interviewerId, scheduledAt);

        // Validar que no exista una entrevista activa del mismo tipo para la aplicación
        if (hasActiveInterview(applicationId, type)) {
            throw new IllegalStateException(
                String.format("Active interview of type %s already exists for application %s", 
                             type, applicationId));
        }

        // Validar disponibilidad del entrevistador
        validateInterviewerAvailability(interviewerId, scheduledAt, 
                                      durationMinutes != null ? durationMinutes : type.getDefaultDuration());

        Interview interview = Interview.createScheduled(
            applicationId, interviewerId, type, scheduledAt, 
            durationMinutes, location, createdBy);

        interview = interviewRepository.save(interview);
        log.info("Scheduled interview: {}", interview.getSummary());

        // Publicar evento de entrevista programada
        evaluationEventPublisher.publishInterviewScheduled(interview);

        return interview;
    }

    /**
     * Confirma una entrevista
     */
    public Interview confirmInterview(UUID interviewId, String confirmedBy) {
        log.info("Confirming interview {}", interviewId);

        Interview interview = getInterviewById(interviewId);
        interview.confirm(confirmedBy);
        interview = interviewRepository.save(interview);

        log.info("Confirmed interview: {}", interview.getSummary());
        return interview;
    }

    /**
     * Inicia una entrevista
     */
    public Interview startInterview(UUID interviewId, String startedBy) {
        log.info("Starting interview {}", interviewId);

        Interview interview = getInterviewById(interviewId);
        interview.start(startedBy);
        interview = interviewRepository.save(interview);

        log.info("Started interview: {}", interview.getSummary());
        return interview;
    }

    /**
     * Completa una entrevista con resultados
     */
    public Interview completeInterview(UUID interviewId, InterviewResult result, String completedBy) {
        log.info("Completing interview {} with overall rating {}", 
                interviewId, result.getOverallRating());

        Interview interview = getInterviewById(interviewId);
        
        interview.complete(
            result.getOverallRating(),
            result.getRecommendation(),
            result.getNotes(),
            result.getCommunicationSkills(),
            result.getAcademicInterest(),
            result.getFamilyAlignment(),
            result.getSpecialConsiderations(),
            result.getStrengths(),
            result.getConcerns(),
            result.getRecommendationsText(),
            completedBy
        );

        interview = interviewRepository.save(interview);

        log.info("Completed interview: {} - Recommendation: {}, Rating: {}/10", 
                interview.getSummary(), 
                interview.getRecommendation(), 
                interview.getOverallRating());

        // Publicar evento de completación
        evaluationEventPublisher.publishInterviewCompleted(interview);

        return interview;
    }

    /**
     * Reprograma una entrevista
     */
    public Interview rescheduleInterview(UUID interviewId, Instant newScheduledAt, 
                                       String reason, String rescheduledBy) {
        log.info("Rescheduling interview {} to {}: {}", interviewId, newScheduledAt, reason);

        Interview interview = getInterviewById(interviewId);
        
        // Validar nueva disponibilidad
        validateInterviewerAvailability(interview.getInterviewerId(), newScheduledAt, 
                                      interview.getDurationMinutes());

        interview.reschedule(newScheduledAt, reason, rescheduledBy);
        interview = interviewRepository.save(interview);

        log.info("Rescheduled interview: {}", interview.getSummary());

        // Publicar evento de reprogramación
        evaluationEventPublisher.publishInterviewScheduled(interview);

        return interview;
    }

    /**
     * Cancela una entrevista
     */
    public Interview cancelInterview(UUID interviewId, String reason, String cancelledBy) {
        log.info("Cancelling interview {}: {}", interviewId, reason);

        Interview interview = getInterviewById(interviewId);
        interview.cancel(reason, cancelledBy);
        interview = interviewRepository.save(interview);

        log.info("Cancelled interview: {}", interview.getSummary());
        return interview;
    }

    /**
     * Marca una entrevista como no asistió
     */
    public Interview markNoShow(UUID interviewId, String updatedBy) {
        log.info("Marking interview {} as no-show", interviewId);

        Interview interview = getInterviewById(interviewId);
        interview.markNoShow(updatedBy);
        interview = interviewRepository.save(interview);

        log.info("Marked interview as no-show: {}", interview.getSummary());
        return interview;
    }

    // ================================
    // QUERY OPERATIONS
    // ================================

    /**
     * Obtiene una entrevista por ID
     */
    @Transactional(readOnly = true)
    public Interview getInterviewById(UUID interviewId) {
        return interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Interview not found with id: " + interviewId));
    }

    /**
     * Obtiene entrevistas de una aplicación
     */
    @Transactional(readOnly = true)
    public List<Interview> getInterviewsByApplication(UUID applicationId) {
        return interviewRepository.findByApplicationId(applicationId);
    }

    /**
     * Obtiene entrevistas asignadas a un entrevistador
     */
    @Transactional(readOnly = true)
    public List<Interview> getInterviewsByInterviewer(String interviewerId) {
        return interviewRepository.findByInterviewerId(interviewerId);
    }

    /**
     * Obtiene próximas entrevistas de un entrevistador
     */
    @Transactional(readOnly = true)
    public List<Interview> getUpcomingInterviews(String interviewerId) {
        return interviewRepository.findUpcomingInterviewsForInterviewer(interviewerId, Instant.now());
    }

    /**
     * Obtiene entrevistas por estado
     */
    @Transactional(readOnly = true)
    public List<Interview> getInterviewsByStatus(InterviewStatus status) {
        return interviewRepository.findByStatus(status);
    }

    /**
     * Obtiene entrevistas vencidas
     */
    @Transactional(readOnly = true)
    public List<Interview> getOverdueInterviews() {
        return interviewRepository.findOverdueInterviews(Instant.now());
    }

    /**
     * Obtiene entrevistas que necesitan recordatorio
     */
    @Transactional(readOnly = true)
    public List<Interview> getInterviewsNeedingReminder() {
        Instant now = Instant.now();
        Instant reminderWindow = now.plus(24, ChronoUnit.HOURS); // 24 horas de anticipación
        return interviewRepository.findInterviewsNeedingReminder(now, reminderWindow);
    }

    // ================================
    // BUSINESS LOGIC OPERATIONS
    // ================================

    /**
     * Verifica si existe una entrevista activa para una aplicación y tipo
     */
    @Transactional(readOnly = true)
    public boolean hasActiveInterview(UUID applicationId, InterviewType type) {
        return interviewRepository.existsActiveInterviewByApplicationAndType(applicationId, type);
    }

    /**
     * Obtiene la agenda de un entrevistador para un día específico
     */
    @Transactional(readOnly = true)
    public InterviewerSchedule getInterviewerScheduleForDay(String interviewerId, LocalDate date) {
        Instant dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<Interview> dayInterviews = interviewRepository
            .findByInterviewerAndDay(interviewerId, dayStart, dayEnd);

        return InterviewerSchedule.builder()
            .interviewerId(interviewerId)
            .date(date)
            .interviews(dayInterviews)
            .totalScheduledMinutes(dayInterviews.stream()
                .mapToInt(Interview::getDurationMinutes)
                .sum())
            .availableSlots(calculateAvailableSlots(dayInterviews, date))
            .build();
    }

    /**
     * Encuentra slots disponibles para programar entrevistas
     */
    @Transactional(readOnly = true)
    public List<AvailableSlot> findAvailableSlots(String interviewerId, LocalDate date, 
                                                 int durationMinutes) {
        InterviewerSchedule schedule = getInterviewerScheduleForDay(interviewerId, date);
        return interviewSchedulingService.findAvailableSlots(schedule, durationMinutes);
    }

    /**
     * Valida conflictos de horario para una entrevista
     */
    @Transactional(readOnly = true)
    public List<Interview> checkSchedulingConflicts(String interviewerId, Instant proposedStart, 
                                                   int durationMinutes) {
        Instant proposedEnd = proposedStart.plus(durationMinutes, ChronoUnit.MINUTES);
        return interviewRepository.findSchedulingConflicts(interviewerId, proposedStart, proposedEnd);
    }

    /**
     * Procesa recordatorios automáticos
     */
    @Transactional
    public int processAutomaticReminders() {
        log.info("Processing automatic interview reminders");

        List<Interview> interviewsNeedingReminder = getInterviewsNeedingReminder();
        int remindersSent = 0;

        for (Interview interview : interviewsNeedingReminder) {
            try {
                interview.sendReminder("SYSTEM");
                interviewRepository.save(interview);
                remindersSent++;

                log.debug("Sent reminder for interview: {}", interview.getSummary());
            } catch (Exception e) {
                log.warn("Failed to send reminder for interview {}: {}", 
                        interview.getId(), e.getMessage());
            }
        }

        log.info("Sent {} interview reminders", remindersSent);
        return remindersSent;
    }

    /**
     * Procesa entrevistas vencidas automáticamente
     */
    @Transactional
    public int processOverdueInterviews() {
        log.info("Processing overdue interviews");

        List<Interview> overdueInterviews = getOverdueInterviews();
        int processedCount = 0;

        for (Interview interview : overdueInterviews) {
            try {
                // Marcar como no asistió si está programada pero no se realizó
                if (interview.getStatus() == InterviewStatus.SCHEDULED || 
                    interview.getStatus() == InterviewStatus.CONFIRMED ||
                    interview.getStatus() == InterviewStatus.REMINDED) {
                    
                    interview.markNoShow("SYSTEM_AUTO");
                    interviewRepository.save(interview);
                    processedCount++;

                    log.debug("Marked overdue interview as no-show: {}", interview.getSummary());
                }
            } catch (Exception e) {
                log.warn("Failed to process overdue interview {}: {}", 
                        interview.getId(), e.getMessage());
            }
        }

        log.info("Processed {} overdue interviews", processedCount);
        return processedCount;
    }

    // ================================
    // HELPER METHODS
    // ================================

    /**
     * Valida la disponibilidad del entrevistador
     */
    private void validateInterviewerAvailability(String interviewerId, Instant scheduledAt, 
                                               int durationMinutes) {
        List<Interview> conflicts = checkSchedulingConflicts(interviewerId, scheduledAt, durationMinutes);
        
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException(
                String.format("Interviewer %s has scheduling conflicts at %s", 
                             interviewerId, scheduledAt));
        }

        // Validar horario laboral
        if (!isWithinBusinessHours(scheduledAt, durationMinutes)) {
            log.warn("Interview scheduled outside business hours for interviewer {}: {}", 
                    interviewerId, scheduledAt);
        }
    }

    /**
     * Verifica si el horario está dentro del horario laboral
     */
    private boolean isWithinBusinessHours(Instant scheduledAt, int durationMinutes) {
        LocalTime startTime = scheduledAt.atZone(ZoneId.systemDefault()).toLocalTime();
        LocalTime endTime = startTime.plusMinutes(durationMinutes);
        
        LocalTime businessStart = LocalTime.of(8, 0);   // 8:00 AM
        LocalTime businessEnd = LocalTime.of(18, 0);    // 6:00 PM
        
        return !startTime.isBefore(businessStart) && !endTime.isAfter(businessEnd);
    }

    /**
     * Calcula slots disponibles basado en entrevistas existentes
     */
    private List<AvailableSlot> calculateAvailableSlots(List<Interview> existingInterviews, LocalDate date) {
        // Implementación simplificada - en producción sería más compleja
        List<AvailableSlot> slots = new ArrayList<>();
        
        LocalTime workStart = LocalTime.of(8, 0);
        LocalTime workEnd = LocalTime.of(18, 0);
        
        // Obtener horarios ocupados
        List<TimeSlot> occupiedSlots = existingInterviews.stream()
            .filter(interview -> interview.getStatus() != InterviewStatus.CANCELLED)
            .map(interview -> {
                LocalTime start = interview.getScheduledAt().atZone(ZoneId.systemDefault()).toLocalTime();
                LocalTime end = start.plusMinutes(interview.getDurationMinutes());
                return new TimeSlot(start, end);
            })
            .sorted(Comparator.comparing(TimeSlot::getStart))
            .collect(Collectors.toList());

        // Calcular slots libres (implementación simplificada)
        LocalTime currentTime = workStart;
        for (TimeSlot occupied : occupiedSlots) {
            if (currentTime.isBefore(occupied.getStart())) {
                slots.add(AvailableSlot.builder()
                    .startTime(currentTime)
                    .endTime(occupied.getStart())
                    .durationMinutes((int) ChronoUnit.MINUTES.between(currentTime, occupied.getStart()))
                    .build());
            }
            currentTime = occupied.getEnd();
        }

        // Slot final hasta el final del día laboral
        if (currentTime.isBefore(workEnd)) {
            slots.add(AvailableSlot.builder()
                .startTime(currentTime)
                .endTime(workEnd)
                .durationMinutes((int) ChronoUnit.MINUTES.between(currentTime, workEnd))
                .build());
        }

        return slots;
    }

    // ================================
    // DATA TRANSFER OBJECTS
    // ================================

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class InterviewResult {
        private Integer overallRating;      // 1-10
        private Recommendation recommendation;
        private String notes;
        private Integer communicationSkills; // 1-10
        private Integer academicInterest;    // 1-10
        private Integer familyAlignment;     // 1-10
        private Integer specialConsiderations; // 1-10
        private String strengths;
        private String concerns;
        private String recommendationsText;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class InterviewerSchedule {
        private String interviewerId;
        private LocalDate date;
        private List<Interview> interviews;
        private int totalScheduledMinutes;
        private List<AvailableSlot> availableSlots;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class AvailableSlot {
        private LocalTime startTime;
        private LocalTime endTime;
        private int durationMinutes;
        private boolean isPreferred;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    private static class TimeSlot {
        private LocalTime start;
        private LocalTime end;
    }

    /**
     * Estadísticas de entrevistas
     */
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class InterviewStatistics {
        private int totalInterviews;
        private int scheduledInterviews;
        private int completedInterviews;
        private int cancelledInterviews;
        private int noShowInterviews;
        private int rescheduledInterviews;
        private Double averageRating;
        private Map<Recommendation, Long> recommendationDistribution;
        private Map<InterviewType, Long> typeDistribution;
        private Double averageDuration;
    }
}