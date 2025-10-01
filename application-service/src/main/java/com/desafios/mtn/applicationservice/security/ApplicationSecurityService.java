package com.desafios.mtn.applicationservice.security;

import com.desafios.mtn.applicationservice.domain.Application;
import com.desafios.mtn.applicationservice.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Security service for application-specific authorization rules
 */
@Service("applicationSecurityService")
@RequiredArgsConstructor
@Slf4j
public class ApplicationSecurityService {

    private final ApplicationRepository applicationRepository;

    /**
     * Check if the current user is the owner of the application
     */
    public boolean isOwner(Long applicationId, String userEmail) {
        try {
            Application application = applicationRepository.findById(applicationId)
                    .orElse(null);
            
            if (application == null) {
                log.warn("Application not found for ownership check: {}", applicationId);
                return false;
            }

            boolean isOwner = application.getApplicantEmail().equals(userEmail);
            log.debug("Ownership check for application {}: user {} is owner: {}", 
                     applicationId, userEmail, isOwner);
            
            return isOwner;
        } catch (Exception e) {
            log.error("Error checking application ownership", e);
            return false;
        }
    }

    /**
     * Check if user can view application (owner or has role-based access)
     */
    public boolean canView(Long applicationId, String userEmail, String... roles) {
        // First check if user is owner
        if (isOwner(applicationId, userEmail)) {
            return true;
        }

        // Then check role-based access
        return hasAnyRole(roles);
    }

    /**
     * Check if user can modify application status
     */
    public boolean canModifyStatus(Long applicationId, String userEmail, String... roles) {
        // Only admin and coordinator roles can modify status
        return hasAnyRole("ADMIN", "COORDINATOR");
    }

    /**
     * Check if user can upload documents for this application
     */
    public boolean canUploadDocuments(Long applicationId, String userEmail) {
        // Only the owner can upload documents
        return isOwner(applicationId, userEmail);
    }

    /**
     * Check if user can archive applications
     */
    public boolean canArchive(String userEmail) {
        // Only admin can archive
        return hasRole("ADMIN");
    }

    /**
     * Check if user has specific role
     */
    private boolean hasRole(String role) {
        // TODO: Implement JWT token role extraction
        // This is a placeholder implementation
        return true; // Temporary for development
    }

    /**
     * Check if user has any of the specified roles
     */
    private boolean hasAnyRole(String... roles) {
        // TODO: Implement JWT token role extraction
        // This is a placeholder implementation
        return true; // Temporary for development
    }

    /**
     * Get current user email from security context
     */
    public String getCurrentUserEmail() {
        // TODO: Extract from JWT token in security context
        return "current-user@mtn.cl"; // Placeholder
    }

    /**
     * Get current user roles from security context
     */
    public String[] getCurrentUserRoles() {
        // TODO: Extract from JWT token in security context
        return new String[]{"ADMIN"}; // Placeholder
    }

    /**
     * Check if application is in a state that allows certain operations
     */
    public boolean isApplicationModifiable(Long applicationId) {
        try {
            Application application = applicationRepository.findById(applicationId)
                    .orElse(null);
            
            if (application == null) {
                return false;
            }

            // Applications can be modified unless they are archived, approved, or rejected
            return !application.getStatus().name().equals("ARCHIVED") &&
                   !application.getStatus().name().equals("APPROVED") &&
                   !application.getStatus().name().equals("REJECTED");
        } catch (Exception e) {
            log.error("Error checking application modifiable status", e);
            return false;
        }
    }

    /**
     * Validate that user can perform bulk operations
     */
    public boolean canPerformBulkOperations(String userEmail) {
        return hasRole("ADMIN");
    }
}