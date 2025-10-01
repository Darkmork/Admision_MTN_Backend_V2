package com.desafios.mtn.applicationservice.web.dto;

import com.desafios.mtn.applicationservice.domain.Application;
import com.desafios.mtn.applicationservice.domain.ApplicationStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Application Data Transfer Object for API responses
 */
@Value
@Builder
public class ApplicationDto {
    Long id;
    String studentName;
    String applicantEmail;
    ApplicationStatus status;
    String gradeLevel;
    String targetSchool;
    LocalDateTime submissionDate;
    LocalDateTime lastModified;
    String lastModifiedBy;
    
    // Document status
    boolean documentsComplete;
    List<DocumentStatusDto> documents;
    
    // Special categories
    boolean isEmployeeChild;
    boolean isAlumniChild;
    boolean isInclusionStudent;
    String specialNotes;
    
    // Contact information
    ContactInfoDto contactInfo;
    
    // Status history
    List<StatusTransitionDto> statusHistory;

    /**
     * Factory method to create DTO from domain entity
     */
    public static ApplicationDto fromDomain(Application application) {
        return ApplicationDto.builder()
                .id(application.getId())
                .studentName(application.getStudentName())
                .applicantEmail(application.getApplicantEmail())
                .status(application.getStatus())
                .gradeLevel(application.getGradeLevel())
                .targetSchool(application.getTargetSchool())
                .submissionDate(application.getSubmissionDate())
                .lastModified(application.getLastModified())
                .lastModifiedBy(application.getLastModifiedBy())
                .documentsComplete(application.isDocumentsComplete())
                .documents(mapDocuments(application.getDocuments()))
                .isEmployeeChild(application.isEmployeeChild())
                .isAlumniChild(application.isAlumniChild())
                .isInclusionStudent(application.isInclusionStudent())
                .specialNotes(application.getSpecialNotes())
                .contactInfo(ContactInfoDto.fromDomain(application.getContactInfo()))
                .statusHistory(mapStatusHistory(application.getStatusHistory()))
                .build();
    }
    
    private static List<DocumentStatusDto> mapDocuments(List<com.desafios.mtn.applicationservice.domain.AppDocument> documents) {
        if (documents == null) return List.of();
        return documents.stream()
                .map(DocumentStatusDto::fromDomain)
                .toList();
    }
    
    private static List<StatusTransitionDto> mapStatusHistory(List<com.desafios.mtn.applicationservice.domain.TransitionLog> history) {
        if (history == null) return List.of();
        return history.stream()
                .map(StatusTransitionDto::fromDomain)
                .toList();
    }
}

/**
 * Document status information
 */
@Value
@Builder
class DocumentStatusDto {
    String documentType;
    boolean uploaded;
    LocalDateTime uploadDate;
    String fileName;
    String status;
    
    static DocumentStatusDto fromDomain(com.desafios.mtn.applicationservice.domain.AppDocument document) {
        return DocumentStatusDto.builder()
                .documentType(document.getDocumentType())
                .uploaded(document.isUploaded())
                .uploadDate(document.getUploadDate())
                .fileName(document.getFileName())
                .status(document.getStatus())
                .build();
    }
}

/**
 * Contact information DTO
 */
@Value
@Builder
class ContactInfoDto {
    String primaryPhone;
    String secondaryPhone;
    String emergencyContact;
    String address;
    String city;
    
    static ContactInfoDto fromDomain(Object contactInfo) {
        // TODO: Implement when contact info domain object is defined
        return ContactInfoDto.builder()
                .primaryPhone("placeholder")
                .secondaryPhone("placeholder")
                .emergencyContact("placeholder")
                .address("placeholder")
                .city("placeholder")
                .build();
    }
}

/**
 * Status transition DTO
 */
@Value
@Builder
class StatusTransitionDto {
    ApplicationStatus fromStatus;
    ApplicationStatus toStatus;
    String reason;
    String changedBy;
    LocalDateTime changeDate;
    
    static StatusTransitionDto fromDomain(com.desafios.mtn.applicationservice.domain.TransitionLog transition) {
        return StatusTransitionDto.builder()
                .fromStatus(transition.getFromStatus())
                .toStatus(transition.getToStatus())
                .reason(transition.getReason())
                .changedBy(transition.getChangedBy())
                .changeDate(transition.getChangeDate())
                .build();
    }
}