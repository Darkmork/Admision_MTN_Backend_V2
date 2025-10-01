package com.desafios.mtn.applicationservice.web.dto;

import com.desafios.mtn.applicationservice.domain.ApplicationStatus;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Collection of DTOs for Application Service operations
 */

/**
 * Request DTO for updating existing applications
 */
@Data
@Builder
public class UpdateApplicationRequest {
    private String studentName;
    private String gradeLevel;
    private String targetSchool;
    private String previousSchool;
    private boolean hasSpecialNeeds;
    private String specialNeedsDescription;
    private String additionalComments;
    private String primaryPhone;
    private String secondaryPhone;
    private String address;
    private String city;
}

/**
 * Statistics DTO for admin dashboard
 */
@Value
@Builder
public class ApplicationStatisticsDto {
    long totalApplications;
    long pendingApplications;
    long underReviewApplications;
    long approvedApplications;
    long rejectedApplications;
    long waitlistApplications;
    long archivedApplications;
    
    Map<String, Long> applicationsByGrade;
    Map<String, Long> applicationsBySchool;
    Map<String, Long> applicationsByMonth;
    
    double averageProcessingTimeHours;
    long documentsUploaded;
    double documentCompletionRate;
    
    LocalDateTime lastUpdated;
}

/**
 * Application count DTO for public endpoints
 */
@Value
@Builder
public class ApplicationCountDto {
    long totalActiveApplications;
    long availableSlots;
    boolean admissionOpen;
    String admissionPeriodMessage;
}

/**
 * Bulk status update request DTO
 */
@Data
@Builder
public class BulkStatusUpdateRequest {
    @NotEmpty(message = "Application IDs list cannot be empty")
    private List<Long> applicationIds;
    
    @NotNull(message = "New status is required")
    private ApplicationStatus newStatus;
    
    private String reason;
}

/**
 * Batch update result DTO
 */
@Value
@Builder
public class BatchUpdateResultDto {
    int totalRequested;
    int successCount;
    int failedCount;
    List<String> errorMessages;
    Map<Long, String> individualResults;
    LocalDateTime processedAt;
}

/**
 * Health status DTO
 */
@Value
@Builder
public class HealthStatusDto {
    String status;
    boolean databaseConnected;
    boolean rabbitMqConnected;
    long totalApplications;
    long pendingOutboxEvents;
    String version;
    LocalDateTime timestamp;
    Map<String, Object> additionalInfo;
}

/**
 * Event DTO for inter-service communication
 */
@Value
@Builder
public class ApplicationEventDto {
    String eventId;
    String eventType;
    Long applicationId;
    String applicantEmail;
    ApplicationStatus currentStatus;
    ApplicationStatus previousStatus;
    String eventData;
    LocalDateTime eventTimestamp;
    String initiatedBy;
}

/**
 * Document upload result DTO
 */
@Value
@Builder
public class DocumentUploadResultDto {
    boolean success;
    String documentId;
    String fileName;
    String documentType;
    long fileSize;
    String uploadUrl;
    String message;
    LocalDateTime uploadedAt;
}

/**
 * Application search criteria DTO
 */
@Data
@Builder
public class ApplicationSearchCriteriaDto {
    private String studentName;
    private String applicantEmail;
    private ApplicationStatus status;
    private String gradeLevel;
    private String targetSchool;
    private Boolean isEmployeeChild;
    private Boolean isAlumniChild;
    private Boolean isInclusionStudent;
    private Boolean documentsComplete;
    private LocalDateTime submittedAfter;
    private LocalDateTime submittedBefore;
    private String lastModifiedBy;
}

/**
 * Validation result DTO
 */
@Value
@Builder
public class ValidationResultDto {
    boolean valid;
    List<String> errors;
    List<String> warnings;
    Map<String, Object> validationDetails;
}

/**
 * Status transition validation DTO
 */
@Value
@Builder
public class StatusTransitionValidationDto {
    ApplicationStatus fromStatus;
    ApplicationStatus toStatus;
    boolean allowed;
    String reason;
    List<String> requiredConditions;
    List<String> missingConditions;
}

/**
 * Application summary DTO for dashboard widgets
 */
@Value
@Builder
public class ApplicationSummaryDto {
    Long id;
    String studentName;
    ApplicationStatus status;
    String gradeLevel;
    String targetSchool;
    boolean documentsComplete;
    LocalDateTime submissionDate;
    int daysSinceSubmission;
    String nextActionRequired;
    String assignedTo;
}

/**
 * Notification request DTO for event publishing
 */
@Value
@Builder
public class NotificationRequestDto {
    String recipientEmail;
    String recipientName;
    String notificationType;
    String subject;
    String templateName;
    Map<String, Object> templateData;
    String priority;
    boolean immediate;
}

/**
 * Integration event DTO for external systems
 */
@Value
@Builder
public class IntegrationEventDto {
    String eventId;
    String source;
    String eventType;
    String aggregateId;
    String aggregateType;
    int version;
    String eventData;
    LocalDateTime occurredAt;
    Map<String, String> metadata;
}

/**
 * Audit log DTO
 */
@Value
@Builder
public class AuditLogDto {
    String action;
    String entityType;
    String entityId;
    String userId;
    String userEmail;
    String changes;
    String ipAddress;
    String userAgent;
    LocalDateTime timestamp;
    Map<String, Object> additionalData;
}