package com.desafios.mtn.applicationservice.web;

import com.desafios.mtn.applicationservice.domain.Application;
import com.desafios.mtn.applicationservice.domain.ApplicationStatus;
import com.desafios.mtn.applicationservice.service.ApplicationService;
import com.desafios.mtn.applicationservice.web.dto.ApplicationDto;
import com.desafios.mtn.applicationservice.web.dto.CreateApplicationRequest;
import com.desafios.mtn.applicationservice.web.dto.UpdateApplicationRequest;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * REST Controller for Application Management
 * Handles all application lifecycle operations in the microservices architecture
 */
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Validated
@Slf4j
@Timed(value = "application.controller", description = "Time taken for application controller operations")
public class ApplicationController {

    private final ApplicationService applicationService;

    /**
     * Submit a new application (Families/APODERADO role)
     */
    @PostMapping("/submit")
    @PreAuthorize("hasRole('APODERADO') or hasRole('ADMIN')")
    @Timed(value = "application.submit", description = "Time taken to submit application")
    public ResponseEntity<ApplicationDto> submitApplication(
            @Valid @RequestBody CreateApplicationRequest request) {
        
        log.info("Submitting new application for student: {}", request.getStudentName());
        
        Application application = applicationService.createApplication(request);
        ApplicationDto response = ApplicationDto.fromDomain(application);
        
        log.info("Application submitted successfully with ID: {}", application.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get application by ID (Owner or Admin/Staff)
     */
    @GetMapping("/{applicationId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COORDINATOR') or @applicationSecurityService.isOwner(#applicationId, authentication.name)")
    public ResponseEntity<ApplicationDto> getApplication(@PathVariable Long applicationId) {
        log.info("Fetching application with ID: {}", applicationId);
        
        Application application = applicationService.findById(applicationId);
        ApplicationDto response = ApplicationDto.fromDomain(application);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get applications by applicant (Family view)
     */
    @GetMapping("/my-applications")
    @PreAuthorize("hasRole('APODERADO')")
    public ResponseEntity<List<ApplicationDto>> getMyApplications() {
        String currentUserEmail = getCurrentUserEmail();
        log.info("Fetching applications for user: {}", currentUserEmail);
        
        List<Application> applications = applicationService.findByApplicantEmail(currentUserEmail);
        List<ApplicationDto> response = applications.stream()
                .map(ApplicationDto::fromDomain)
                .toList();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get all applications with pagination (Admin/Staff only)
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COORDINATOR')")
    public ResponseEntity<Page<ApplicationDto>> getAllApplications(
            Pageable pageable,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String search) {
        
        log.info("Fetching applications - Page: {}, Size: {}, Status: {}, Search: {}", 
                pageable.getPageNumber(), pageable.getPageSize(), status, search);
        
        Page<Application> applications = applicationService.findAllWithFilters(pageable, status, search);
        Page<ApplicationDto> response = applications.map(ApplicationDto::fromDomain);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Update application status (Admin/Coordinator only)
     */
    @PutMapping("/{applicationId}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COORDINATOR')")
    @Timed(value = "application.status.update", description = "Time taken to update application status")
    public ResponseEntity<ApplicationDto> updateApplicationStatus(
            @PathVariable Long applicationId,
            @RequestParam ApplicationStatus newStatus,
            @RequestParam(required = false) String reason) {
        
        log.info("Updating application {} status to: {} with reason: {}", applicationId, newStatus, reason);
        
        Application application = applicationService.updateStatus(applicationId, newStatus, reason, getCurrentUserEmail());
        ApplicationDto response = ApplicationDto.fromDomain(application);
        
        log.info("Application status updated successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Update application details (Owner or Admin)
     */
    @PutMapping("/{applicationId}")
    @PreAuthorize("hasRole('ADMIN') or @applicationSecurityService.isOwner(#applicationId, authentication.name)")
    public ResponseEntity<ApplicationDto> updateApplication(
            @PathVariable Long applicationId,
            @Valid @RequestBody UpdateApplicationRequest request) {
        
        log.info("Updating application with ID: {}", applicationId);
        
        Application application = applicationService.updateApplication(applicationId, request);
        ApplicationDto response = ApplicationDto.fromDomain(application);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Archive application (Admin only)
     */
    @PutMapping("/{applicationId}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApplicationDto> archiveApplication(@PathVariable Long applicationId) {
        log.info("Archiving application with ID: {}", applicationId);
        
        Application application = applicationService.archiveApplication(applicationId, getCurrentUserEmail());
        ApplicationDto response = ApplicationDto.fromDomain(application);
        
        log.info("Application archived successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Get application statistics (Admin/Coordinator)
     */
    @GetMapping("/admin/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COORDINATOR')")
    public ResponseEntity<ApplicationStatisticsDto> getApplicationStatistics() {
        log.info("Fetching application statistics");
        
        ApplicationStatisticsDto stats = applicationService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get applications by status (Public endpoint for basic info)
     */
    @GetMapping("/public/count-by-status")
    public ResponseEntity<ApplicationCountDto> getApplicationCountByStatus() {
        ApplicationCountDto count = applicationService.getApplicationCount();
        return ResponseEntity.ok(count);
    }

    /**
     * Bulk status update (Admin only)
     */
    @PutMapping("/admin/bulk-status-update")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BatchUpdateResultDto> bulkStatusUpdate(
            @Valid @RequestBody BulkStatusUpdateRequest request) {
        
        log.info("Bulk status update for {} applications to status: {}", 
                request.getApplicationIds().size(), request.getNewStatus());
        
        BatchUpdateResultDto result = applicationService.bulkStatusUpdate(
                request.getApplicationIds(), 
                request.getNewStatus(), 
                request.getReason(),
                getCurrentUserEmail()
        );
        
        log.info("Bulk update completed - Success: {}, Failed: {}", 
                result.getSuccessCount(), result.getFailedCount());
        
        return ResponseEntity.ok(result);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<HealthStatusDto> health() {
        HealthStatusDto health = applicationService.getHealthStatus();
        return ResponseEntity.ok(health);
    }

    // Helper method to get current user email from security context
    private String getCurrentUserEmail() {
        // TODO: Extract from JWT token in security context
        return "current-user@mtn.cl"; // Placeholder
    }
}