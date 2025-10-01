package com.desafios.mtn.evaluationservice.controller;

import com.desafios.mtn.evaluationservice.domain.Interview;
import com.desafios.mtn.evaluationservice.domain.Interview.*;
import com.desafios.mtn.evaluationservice.service.InterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para la gestión de entrevistas de admisión
 */
@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Interviews", description = "API para gestión de entrevistas de admisión")
public class InterviewController {

    private final InterviewService interviewService;

    // ================================
    // INTERVIEW CRUD OPERATIONS
    // ================================

    @Operation(summary = "Programar nueva entrevista", description = "Programa una nueva entrevista para una aplicación")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Entrevista programada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "409", description = "Conflicto de horarios o entrevista duplicada")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'CYCLE_DIRECTOR')")
    public ResponseEntity<InterviewResponse> scheduleInterview(
            @Valid @RequestBody ScheduleInterviewRequest request) {
        
        log.info("Scheduling {} for application {} with interviewer {} at {}", 
                request.getType(), request.getApplicationId(), 
                request.getInterviewerId(), request.getScheduledAt());

        Interview interview = interviewService.scheduleInterview(
            request.getApplicationId(),
            request.getInterviewerId(),
            request.getType(),
            request.getScheduledAt(),
            request.getDurationMinutes(),
            request.getLocation(),
            getCurrentUser()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(InterviewResponse.fromInterview(interview));
    }

    @Operation(summary = "Obtener entrevista por ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'CYCLE_DIRECTOR', 'PSYCHOLOGIST')")
    public ResponseEntity<InterviewResponse> getInterview(
            @Parameter(description = "ID de la entrevista") @PathVariable UUID id) {
        
        Interview interview = interviewService.getInterviewById(id);
        return ResponseEntity.ok(InterviewResponse.fromInterview(interview));
    }

    @Operation(summary = "Obtener entrevistas por aplicación")
    @GetMapping("/application/{applicationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'CYCLE_DIRECTOR', 'PSYCHOLOGIST')")
    public ResponseEntity<List<InterviewResponse>> getInterviewsByApplication(
            @Parameter(description = "ID de la aplicación") @PathVariable UUID applicationId) {
        
        List<Interview> interviews = interviewService.getInterviewsByApplication(applicationId);
        List<InterviewResponse> responses = interviews.stream()
            .map(InterviewResponse::fromInterview)
            .toList();
        
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Obtener entrevistas asignadas al entrevistador actual")
    @GetMapping("/my-interviews")
    @PreAuthorize("hasAnyRole('CYCLE_DIRECTOR', 'PSYCHOLOGIST', 'TEACHER')")
    public ResponseEntity<List<InterviewResponse>> getMyInterviews() {
        
        String currentUser = getCurrentUser();
        List<Interview> interviews = interviewService.getInterviewsByInterviewer(currentUser);
        List<InterviewResponse> responses = interviews.stream()
            .map(InterviewResponse::fromInterview)
            .toList();
        
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Obtener próximas entrevistas del entrevistador actual")
    @GetMapping("/my-upcoming")
    @PreAuthorize("hasAnyRole('CYCLE_DIRECTOR', 'PSYCHOLOGIST', 'TEACHER')")
    public ResponseEntity<List<InterviewResponse>> getMyUpcomingInterviews() {
        
        String currentUser = getCurrentUser();
        List<Interview> interviews = interviewService.getUpcomingInterviews(currentUser);
        List<InterviewResponse> responses = interviews.stream()
            .map(InterviewResponse::fromInterview)
            .toList();
        
        return ResponseEntity.ok(responses);
    }

    // ================================
    // INTERVIEW STATE MANAGEMENT
    // ================================

    @Operation(summary = "Confirmar entrevista")
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'CYCLE_DIRECTOR', 'PSYCHOLOGIST')")
    public ResponseEntity<InterviewResponse> confirmInterview(@PathVariable UUID id) {
        
        log.info("Confirming interview {}", id);

        Interview interview = interviewService.confirmInterview(id, getCurrentUser());
        return ResponseEntity.ok(InterviewResponse.fromInterview(interview));
    }

    @Operation(summary = "Iniciar entrevista")
    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('CYCLE_DIRECTOR', 'PSYCHOLOGIST')")
    public ResponseEntity<InterviewResponse> startInterview(@PathVariable UUID id) {
        
        log.info("Starting interview {}", id);

        Interview interview = interviewService.startInterview(id, getCurrentUser());
        return ResponseEntity.ok(InterviewResponse.fromInterview(interview));
    }

    @Operation(summary = "Completar entrevista con resultados")
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('CYCLE_DIRECTOR', 'PSYCHOLOGIST')")
    public ResponseEntity<InterviewResponse> completeInterview(
            @PathVariable UUID id,
            @Valid @RequestBody CompleteInterviewRequest request) {
        
        log.info("Completing interview {} with rating {}", id, request.getOverallRating());

        InterviewService.InterviewResult result = InterviewService.InterviewResult.builder()
            .overallRating(request.getOverallRating())
            .recommendation(request.getRecommendation())
            .notes(request.getNotes())
            .communicationSkills(request.getCommunicationSkills())
            .academicInterest(request.getAcademicInterest())
            .familyAlignment(request.getFamilyAlignment())
            .specialConsiderations(request.getSpecialConsiderations())
            .strengths(request.getStrengths())
            .concerns(request.getConcerns())
            .recommendationsText(request.getRecommendationsText())
            .build();

        Interview interview = interviewService.completeInterview(id, result, getCurrentUser());
        return ResponseEntity.ok(InterviewResponse.fromInterview(interview));
    }

    @Operation(summary = "Reprogramar entrevista")
    @PostMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'CYCLE_DIRECTOR')")
    public ResponseEntity<InterviewResponse> rescheduleInterview(
            @PathVariable UUID id,
            @Valid @RequestBody RescheduleInterviewRequest request) {
        
        log.info("Rescheduling interview {} to {}: {}", 
                id, request.getNewScheduledAt(), request.getReason());

        Interview interview = interviewService.rescheduleInterview(
            id,
            request.getNewScheduledAt(),
            request.getReason(),
            getCurrentUser()
        );

        return ResponseEntity.ok(InterviewResponse.fromInterview(interview));
    }

    @Operation(summary = "Cancelar entrevista")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'CYCLE_DIRECTOR')")
    public ResponseEntity<InterviewResponse> cancelInterview(
            @PathVariable UUID id,
            @Valid @RequestBody CancelInterviewRequest request) {
        
        log.info("Cancelling interview {}: {}", id, request.getReason());

        Interview interview = interviewService.cancelInterview(
            id,
            request.getReason(),
            getCurrentUser()
        );

        return ResponseEntity.ok(InterviewResponse.fromInterview(interview));
    }

    @Operation(summary = "Marcar entrevista como no asistió")
    @PostMapping("/{id}/no-show")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'CYCLE_DIRECTOR', 'PSYCHOLOGIST')")
    public ResponseEntity<InterviewResponse> markNoShow(@PathVariable UUID id) {
        
        log.info("Marking interview {} as no-show", id);

        Interview interview = interviewService.markNoShow(id, getCurrentUser());
        return ResponseEntity.ok(InterviewResponse.fromInterview(interview));
    }

    // ================================
    // SCHEDULING AND AVAILABILITY
    // ================================

    @Operation(summary = "Obtener agenda del entrevistador para un día específico")
    @GetMapping("/schedule/{interviewerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<InterviewService.InterviewerSchedule> getInterviewerSchedule(
            @PathVariable String interviewerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        InterviewService.InterviewerSchedule schedule = 
            interviewService.getInterviewerScheduleForDay(interviewerId, date);
        
        return ResponseEntity.ok(schedule);
    }

    @Operation(summary = "Encontrar slots disponibles para programar entrevistas")
    @GetMapping("/available-slots/{interviewerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<List<InterviewService.AvailableSlot>> getAvailableSlots(
            @PathVariable String interviewerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "60") int durationMinutes) {
        
        List<InterviewService.AvailableSlot> slots = 
            interviewService.findAvailableSlots(interviewerId, date, durationMinutes);
        
        return ResponseEntity.ok(slots);
    }

    @Operation(summary = "Verificar conflictos de horario")
    @GetMapping("/check-conflicts/{interviewerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<List<InterviewResponse>> checkSchedulingConflicts(
            @PathVariable String interviewerId,
            @RequestParam Instant proposedStart,
            @RequestParam(defaultValue = "60") int durationMinutes) {
        
        List<Interview> conflicts = interviewService.checkSchedulingConflicts(
            interviewerId, proposedStart, durationMinutes);
        
        List<InterviewResponse> responses = conflicts.stream()
            .map(InterviewResponse::fromInterview)
            .toList();
        
        return ResponseEntity.ok(responses);
    }

    // ================================
    // QUERIES AND MONITORING
    // ================================

    @Operation(summary = "Obtener entrevistas por estado")
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<List<InterviewResponse>> getInterviewsByStatus(
            @PathVariable InterviewStatus status) {
        
        List<Interview> interviews = interviewService.getInterviewsByStatus(status);
        List<InterviewResponse> responses = interviews.stream()
            .map(InterviewResponse::fromInterview)
            .toList();
        
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Obtener entrevistas vencidas")
    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<List<InterviewResponse>> getOverdueInterviews() {
        
        List<Interview> interviews = interviewService.getOverdueInterviews();
        List<InterviewResponse> responses = interviews.stream()
            .map(InterviewResponse::fromInterview)
            .toList();
        
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Obtener entrevistas que necesitan recordatorio")
    @GetMapping("/need-reminder")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<List<InterviewResponse>> getInterviewsNeedingReminder() {
        
        List<Interview> interviews = interviewService.getInterviewsNeedingReminder();
        List<InterviewResponse> responses = interviews.stream()
            .map(InterviewResponse::fromInterview)
            .toList();
        
        return ResponseEntity.ok(responses);
    }

    // ================================
    // HELPER METHODS
    // ================================

    private String getCurrentUser() {
        // En una implementación real, esto extraería el usuario del SecurityContext
        return "current-user";
    }

    // ================================
    // DTOs
    // ================================

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ScheduleInterviewRequest {
        private UUID applicationId;
        private String interviewerId;
        private InterviewType type;
        private Instant scheduledAt;
        private Integer durationMinutes;
        private String location;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class CompleteInterviewRequest {
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
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class RescheduleInterviewRequest {
        private Instant newScheduledAt;
        private String reason;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class CancelInterviewRequest {
        private String reason;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class InterviewResponse {
        private UUID id;
        private UUID applicationId;
        private String interviewerId;
        private InterviewType type;
        private InterviewStatus status;
        private Instant scheduledAt;
        private Integer durationMinutes;
        private String location;
        private String meetingLink;
        private String confirmationCode;
        private Instant startedAt;
        private Instant completedAt;
        private Instant cancelledAt;
        private Instant rescheduledAt;
        private Integer overallRating;
        private Recommendation recommendation;
        private Integer communicationSkills;
        private Integer academicInterest;
        private Integer familyAlignment;
        private Integer specialConsiderations;
        private String notes;
        private String strengths;
        private String concerns;
        private String recommendationsText;
        private String rescheduleReason;
        private String cancellationReason;
        private Boolean reminderSent;
        private Boolean confirmationSent;
        private Integer priority;
        private Boolean urgentFlag;
        private Instant createdAt;
        private Instant updatedAt;
        private String createdBy;
        private String updatedBy;

        public static InterviewResponse fromInterview(Interview interview) {
            return InterviewResponse.builder()
                .id(interview.getId())
                .applicationId(interview.getApplicationId())
                .interviewerId(interview.getInterviewerId())
                .type(interview.getType())
                .status(interview.getStatus())
                .scheduledAt(interview.getScheduledAt())
                .durationMinutes(interview.getDurationMinutes())
                .location(interview.getLocation())
                .meetingLink(interview.getMeetingLink())
                .confirmationCode(interview.getConfirmationCode())
                .startedAt(interview.getStartedAt())
                .completedAt(interview.getCompletedAt())
                .cancelledAt(interview.getCancelledAt())
                .rescheduledAt(interview.getRescheduledAt())
                .overallRating(interview.getOverallRating())
                .recommendation(interview.getRecommendation())
                .communicationSkills(interview.getCommunicationSkills())
                .academicInterest(interview.getAcademicInterest())
                .familyAlignment(interview.getFamilyAlignment())
                .specialConsiderations(interview.getSpecialConsiderations())
                .notes(interview.getNotes())
                .strengths(interview.getStrengths())
                .concerns(interview.getConcerns())
                .recommendationsText(interview.getRecommendationsText())
                .rescheduleReason(interview.getRescheduleReason())
                .cancellationReason(interview.getCancellationReason())
                .reminderSent(interview.getReminderSent())
                .confirmationSent(interview.getConfirmationSent())
                .priority(interview.getPriority())
                .urgentFlag(interview.getUrgentFlag())
                .createdAt(interview.getCreatedAt())
                .updatedAt(interview.getUpdatedAt())
                .createdBy(interview.getCreatedBy())
                .updatedBy(interview.getUpdatedBy())
                .build();
        }
    }
}