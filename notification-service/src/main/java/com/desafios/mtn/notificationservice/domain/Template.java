package com.desafios.mtn.notificationservice.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.List;

/**
 * Entidad Template para el manejo de plantillas de notificaciones
 */
@Entity
@Table(name = "templates", indexes = {
    @Index(name = "idx_templates_channel", columnList = "channel"),
    @Index(name = "idx_templates_active", columnList = "active"),
    @Index(name = "idx_templates_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Template {

    @Id
    @EqualsAndHashCode.Include
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(length = 500)
    private String subject; // solo para email

    @Column(name = "body_text", columnDefinition = "TEXT")
    private String bodyText; // email/sms

    @Column(name = "body_html", columnDefinition = "TEXT")  
    private String bodyHtml; // solo para email

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> variables; // variables requeridas

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    // ======================
    // ENUMS
    // ======================

    public enum NotificationChannel {
        email, sms
    }

    // ======================
    // BUSINESS METHODS
    // ======================

    /**
     * Verifica si la plantilla es de email
     */
    @JsonIgnore
    public boolean isEmailTemplate() {
        return channel == NotificationChannel.email;
    }

    /**
     * Verifica si la plantilla es de SMS
     */
    @JsonIgnore
    public boolean isSmsTemplate() {
        return channel == NotificationChannel.sms;
    }

    /**
     * Verifica si la plantilla tiene contenido HTML
     */
    @JsonIgnore
    public boolean hasHtmlContent() {
        return bodyHtml != null && !bodyHtml.trim().isEmpty();
    }

    /**
     * Verifica si la plantilla tiene contenido de texto
     */
    @JsonIgnore
    public boolean hasTextContent() {
        return bodyText != null && !bodyText.trim().isEmpty();
    }

    /**
     * Obtiene el contenido principal dependiendo del tipo
     */
    @JsonIgnore
    public String getMainContent() {
        if (isSmsTemplate()) {
            return bodyText;
        }
        
        // Para email, preferir HTML si existe, sino texto
        return hasHtmlContent() ? bodyHtml : bodyText;
    }

    /**
     * Verifica si tiene las variables requeridas mínimas
     */
    @JsonIgnore
    public boolean hasValidContent() {
        if (isSmsTemplate()) {
            return hasTextContent();
        }
        
        // Para email debe tener al menos uno de los dos contenidos
        return hasTextContent() || hasHtmlContent();
    }

    /**
     * Verifica si una variable es requerida por esta plantilla
     */
    public boolean requiresVariable(String variableName) {
        return variables != null && variables.contains(variableName);
    }

    /**
     * Obtiene el número de variables requeridas
     */
    @JsonIgnore
    public int getRequiredVariablesCount() {
        return variables != null ? variables.size() : 0;
    }

    /**
     * Verifica si la plantilla está lista para uso
     */
    @JsonIgnore
    public boolean isReadyForUse() {
        return active && hasValidContent() && id != null && !id.trim().isEmpty();
    }

    /**
     * Actualiza el timestamp de modificación
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Sanitiza el ID antes de persistir
     */
    @PrePersist
    @PreUpdate
    protected void sanitizeId() {
        if (id != null) {
            // Convertir a lowercase y reemplazar espacios con underscore
            id = id.trim().toLowerCase().replaceAll("\\s+", "_");
        }
    }

    // ======================
    // FACTORY METHODS
    // ======================

    /**
     * Crea una plantilla de email básica
     */
    public static Template createEmailTemplate(String id, String subject, 
            String bodyText, String bodyHtml, List<String> variables) {
        return Template.builder()
                .id(id)
                .channel(NotificationChannel.email)
                .subject(subject)
                .bodyText(bodyText)
                .bodyHtml(bodyHtml)
                .variables(variables)
                .createdBy("SYSTEM")
                .build();
    }

    /**
     * Crea una plantilla de SMS básica
     */
    public static Template createSmsTemplate(String id, String bodyText, List<String> variables) {
        return Template.builder()
                .id(id)
                .channel(NotificationChannel.sms)
                .bodyText(bodyText)
                .variables(variables)
                .createdBy("SYSTEM")
                .build();
    }

    // ======================
    // CONSTANTS
    // ======================

    public static final class TemplateIds {
        public static final String USER_ACCOUNT_CREATED = "user_account_created";
        public static final String PASSWORD_RESET = "password_reset";
        public static final String EMAIL_VERIFICATION = "email_verification";
        public static final String ROLES_ASSIGNED = "roles_assigned";
        public static final String ACCOUNT_DISABLED = "account_disabled";
        public static final String APPLICATION_RECEIVED = "application_received";
        public static final String APPLICATION_STATUS_CHANGED = "application_status_changed";
        public static final String INTERVIEW_SCHEDULED = "interview_scheduled";
        public static final String DOCUMENT_REMINDER = "document_reminder";
        
        // SMS Templates
        public static final String SMS_VERIFICATION_CODE = "sms_verification_code";
        public static final String SMS_PASSWORD_RESET = "sms_password_reset";
        public static final String SMS_APPLICATION_STATUS = "sms_application_status";
    }
}