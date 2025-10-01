// src/main/java/com/desafios/mtn/userservice/outbox/EmailRequestedEvent.java

package com.desafios.mtn.userservice.outbox;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Evento EmailRequested.v1 para el patrón Outbox en user-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequestedEvent {

    @JsonProperty("schema")
    @Builder.Default
    private String schema = "notifications.email.requested.v1";

    @JsonProperty("message_id")
    @Builder.Default
    private String messageId = UUID.randomUUID().toString();

    @JsonProperty("idempotency_key")
    private String idempotencyKey;

    @JsonProperty("to")
    private List<String> to;

    @JsonProperty("cc")
    private List<String> cc;

    @JsonProperty("bcc")
    private List<String> bcc;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("template_id")
    private String templateId;

    @JsonProperty("variables")
    private Map<String, Object> variables;

    @JsonProperty("body_text")
    private String bodyText;

    @JsonProperty("body_html")
    private String bodyHtml;

    @JsonProperty("attachments")
    private List<Attachment> attachments;

    @JsonProperty("priority")
    @Builder.Default
    private String priority = "normal"; // normal | high

    @JsonProperty("correlation_id")
    private String correlationId;

    @JsonProperty("created_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", timezone = "UTC")
    @Builder.Default
    private Instant createdAt = Instant.now();

    // Campos adicionales para el contexto del agregado
    @JsonProperty("aggregate_type")
    @Builder.Default
    private String aggregateType = OutboxEntity.AggregateTypes.USER;

    @JsonProperty("aggregate_id")
    private String aggregateId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attachment {
        @JsonProperty("name")
        private String name;

        @JsonProperty("content_base64")
        private String contentBase64;

        @JsonProperty("content_type")
        private String contentType;
    }

    /**
     * Crea un evento de email de usuario creado
     */
    public static EmailRequestedEvent createUserCreatedEmail(
            String userEmail, String userFullName, String temporaryPassword, 
            UUID userId, String idempotencyKey) {
        
        Map<String, Object> variables = Map.of(
                "nombre", userFullName,
                "email", userEmail,
                "password_temporal", temporaryPassword,
                "url_login", "https://admision.mtn.cl/profesor/login",
                "url_cambiar_password", "https://admision.mtn.cl/cambiar-password"
        );
        
        return EmailRequestedEvent.builder()
                .to(List.of(userEmail))
                .subject("Cuenta creada - Sistema de Admisión MTN")
                .templateId("user_account_created")
                .variables(variables)
                .priority("high")
                .idempotencyKey(idempotencyKey)
                .correlationId(UUID.randomUUID().toString())
                .aggregateType(OutboxEntity.AggregateTypes.USER)
                .aggregateId(userId.toString())
                .build();
    }

    /**
     * Crea un evento de email de contraseña reseteada
     */
    public static EmailRequestedEvent createPasswordResetEmail(
            String userEmail, String userFullName, String temporaryPassword, 
            UUID userId, String idempotencyKey) {
        
        Map<String, Object> variables = Map.of(
                "nombre", userFullName,
                "password_temporal", temporaryPassword,
                "url_login", "https://admision.mtn.cl/profesor/login",
                "url_cambiar_password", "https://admision.mtn.cl/cambiar-password",
                "fecha_reset", Instant.now().toString()
        );
        
        return EmailRequestedEvent.builder()
                .to(List.of(userEmail))
                .subject("Contraseña reseteada - Sistema de Admisión MTN")
                .templateId("password_reset")
                .variables(variables)
                .priority("high")
                .idempotencyKey(idempotencyKey)
                .correlationId(UUID.randomUUID().toString())
                .aggregateType(OutboxEntity.AggregateTypes.USER)
                .aggregateId(userId.toString())
                .build();
    }

    /**
     * Crea un evento de email de verificación de email
     */
    public static EmailRequestedEvent createEmailVerificationEmail(
            String userEmail, String userFullName, String verificationToken, 
            UUID userId, String idempotencyKey) {
        
        Map<String, Object> variables = Map.of(
                "nombre", userFullName,
                "url_verificacion", "https://admision.mtn.cl/verificar-email?token=" + verificationToken,
                "token_expira_minutos", "15"
        );
        
        return EmailRequestedEvent.builder()
                .to(List.of(userEmail))
                .subject("Verificación de email - Sistema de Admisión MTN")
                .templateId("email_verification")
                .variables(variables)
                .priority("normal")
                .idempotencyKey(idempotencyKey)
                .correlationId(UUID.randomUUID().toString())
                .aggregateType(OutboxEntity.AggregateTypes.USER)
                .aggregateId(userId.toString())
                .build();
    }

    /**
     * Crea un evento de email de bienvenida para nuevo rol
     */
    public static EmailRequestedEvent createRoleAssignedEmail(
            String userEmail, String userFullName, List<String> newRoles, 
            String assignedByName, UUID userId, String idempotencyKey) {
        
        Map<String, Object> variables = Map.of(
                "nombre", userFullName,
                "roles", newRoles,
                "asignado_por", assignedByName,
                "fecha_asignacion", Instant.now().toString(),
                "url_dashboard", "https://admision.mtn.cl/profesor/dashboard"
        );
        
        return EmailRequestedEvent.builder()
                .to(List.of(userEmail))
                .subject("Nuevos roles asignados - Sistema de Admisión MTN")
                .templateId("roles_assigned")
                .variables(variables)
                .priority("normal")
                .idempotencyKey(idempotencyKey)
                .correlationId(UUID.randomUUID().toString())
                .aggregateType(OutboxEntity.AggregateTypes.USER)
                .aggregateId(userId.toString())
                .build();
    }

    /**
     * Crea un evento de email de cuenta deshabilitada
     */
    public static EmailRequestedEvent createAccountDisabledEmail(
            String userEmail, String userFullName, String reason, 
            String disabledByName, UUID userId, String idempotencyKey) {
        
        Map<String, Object> variables = Map.of(
                "nombre", userFullName,
                "razon", reason != null ? reason : "Por decisión administrativa",
                "deshabilitado_por", disabledByName,
                "fecha_deshabilitacion", Instant.now().toString(),
                "contacto_soporte", "soporte@mtn.cl"
        );
        
        return EmailRequestedEvent.builder()
                .to(List.of(userEmail))
                .subject("Cuenta deshabilitada - Sistema de Admisión MTN")
                .templateId("account_disabled")
                .variables(variables)
                .priority("high")
                .idempotencyKey(idempotencyKey)
                .correlationId(UUID.randomUUID().toString())
                .aggregateType(OutboxEntity.AggregateTypes.USER)
                .aggregateId(userId.toString())
                .build();
    }

    /**
     * Valida que el evento tenga los campos requeridos
     */
    public boolean isValid() {
        return to != null && !to.isEmpty() &&
               to.get(0) != null && to.get(0).contains("@") &&
               (subject != null && !subject.trim().isEmpty()) &&
               (templateId != null || bodyText != null || bodyHtml != null) &&
               idempotencyKey != null && !idempotencyKey.trim().isEmpty();
    }

    /**
     * Obtiene el primer destinatario
     */
    public String getPrimaryRecipient() {
        return to != null && !to.isEmpty() ? to.get(0) : null;
    }

    /**
     * Verifica si es un email de alta prioridad
     */
    public boolean isHighPriority() {
        return "high".equals(priority);
    }

    /**
     * Verifica si usa template
     */
    public boolean usesTemplate() {
        return templateId != null && !templateId.trim().isEmpty();
    }

    /**
     * Verifica si tiene contenido directo
     */
    public boolean hasDirectContent() {
        return (bodyText != null && !bodyText.trim().isEmpty()) ||
               (bodyHtml != null && !bodyHtml.trim().isEmpty());
    }

    /**
     * Verifica si tiene adjuntos
     */
    public boolean hasAttachments() {
        return attachments != null && !attachments.isEmpty();
    }
}