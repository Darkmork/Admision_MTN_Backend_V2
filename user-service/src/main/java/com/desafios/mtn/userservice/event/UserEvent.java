// user-service/src/main/java/com/desafios/mtn/userservice/event/UserEvent.java

package com.desafios.mtn.userservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Eventos relacionados con usuarios
 */
public class UserEvent {

    /**
     * Evento base para todos los eventos de usuario
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public abstract static class BaseUserEvent {
        protected UUID userId;
        protected String userEmail;
        protected String userFullName;
        protected Instant timestamp;
        protected UUID correlationId;
        protected Map<String, Object> metadata;

        public BaseUserEvent(UUID userId, String userEmail, String userFullName) {
            this.userId = userId;
            this.userEmail = userEmail;
            this.userFullName = userFullName;
            this.timestamp = Instant.now();
            this.correlationId = UUID.randomUUID();
        }
    }

    /**
     * Evento de usuario creado
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserCreated extends BaseUserEvent {
        private UUID userId;
        private String userEmail;
        private String userFullName;
        private String firstName;
        private String lastName;
        private String rut;
        private String phone;
        private String educationalLevel;
        private String subject;
        private Set<String> roles;
        private Boolean enabled;
        private Boolean emailVerified;
        private UUID createdBy;
        private Instant timestamp;
        private UUID correlationId;
        private Map<String, Object> metadata;

        public static UserCreated of(UUID userId, String email, String firstName, String lastName, 
                                   String rut, Set<String> roles, UUID createdBy) {
            return UserCreated.builder()
                    .userId(userId)
                    .userEmail(email)
                    .userFullName(firstName + " " + lastName)
                    .firstName(firstName)
                    .lastName(lastName)
                    .rut(rut)
                    .roles(roles)
                    .createdBy(createdBy)
                    .timestamp(Instant.now())
                    .correlationId(UUID.randomUUID())
                    .build();
        }
    }

    /**
     * Evento de usuario actualizado
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserUpdated extends BaseUserEvent {
        private UUID userId;
        private String userEmail;
        private String userFullName;
        private Map<String, Object> changedFields;
        private Map<String, Object> previousValues;
        private UUID updatedBy;
        private Instant timestamp;
        private UUID correlationId;
        private Map<String, Object> metadata;

        public static UserUpdated of(UUID userId, String email, String fullName, 
                                   Map<String, Object> changes, UUID updatedBy) {
            return UserUpdated.builder()
                    .userId(userId)
                    .userEmail(email)
                    .userFullName(fullName)
                    .changedFields(changes)
                    .updatedBy(updatedBy)
                    .timestamp(Instant.now())
                    .correlationId(UUID.randomUUID())
                    .build();
        }
    }

    /**
     * Evento de usuario eliminado
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDeleted extends BaseUserEvent {
        private UUID userId;
        private String userEmail;
        private String userFullName;
        private String deletionReason;
        private Boolean softDelete;
        private UUID deletedBy;
        private Instant timestamp;
        private UUID correlationId;
        private Map<String, Object> metadata;

        public static UserDeleted of(UUID userId, String email, String fullName, 
                                   String reason, UUID deletedBy) {
            return UserDeleted.builder()
                    .userId(userId)
                    .userEmail(email)
                    .userFullName(fullName)
                    .deletionReason(reason)
                    .softDelete(true)
                    .deletedBy(deletedBy)
                    .timestamp(Instant.now())
                    .correlationId(UUID.randomUUID())
                    .build();
        }
    }

    /**
     * Evento de roles de usuario cambiados
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRolesChanged extends BaseUserEvent {
        private UUID userId;
        private String userEmail;
        private String userFullName;
        private Set<String> previousRoles;
        private Set<String> newRoles;
        private String operation; // ASSIGN, REMOVE, REPLACE
        private UUID changedBy;
        private String comments;
        private Instant timestamp;
        private UUID correlationId;
        private Map<String, Object> metadata;

        public static UserRolesChanged of(UUID userId, String email, String fullName,
                                        Set<String> previousRoles, Set<String> newRoles,
                                        String operation, UUID changedBy) {
            return UserRolesChanged.builder()
                    .userId(userId)
                    .userEmail(email)
                    .userFullName(fullName)
                    .previousRoles(previousRoles)
                    .newRoles(newRoles)
                    .operation(operation)
                    .changedBy(changedBy)
                    .timestamp(Instant.now())
                    .correlationId(UUID.randomUUID())
                    .build();
        }
    }

    /**
     * Evento de estado de usuario cambiado
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStatusChanged extends BaseUserEvent {
        private UUID userId;
        private String userEmail;
        private String userFullName;
        private Boolean previousEnabled;
        private Boolean newEnabled;
        private String reason;
        private UUID changedBy;
        private Instant timestamp;
        private UUID correlationId;
        private Map<String, Object> metadata;

        public static UserStatusChanged of(UUID userId, String email, String fullName,
                                         Boolean previousEnabled, Boolean newEnabled,
                                         String reason, UUID changedBy) {
            return UserStatusChanged.builder()
                    .userId(userId)
                    .userEmail(email)
                    .userFullName(fullName)
                    .previousEnabled(previousEnabled)
                    .newEnabled(newEnabled)
                    .reason(reason)
                    .changedBy(changedBy)
                    .timestamp(Instant.now())
                    .correlationId(UUID.randomUUID())
                    .build();
        }
    }

    /**
     * Evento de login de usuario
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserLoggedIn extends BaseUserEvent {
        private UUID userId;
        private String userEmail;
        private String userFullName;
        private String ipAddress;
        private String userAgent;
        private String location;
        private Instant timestamp;
        private UUID correlationId;
        private Map<String, Object> metadata;

        public static UserLoggedIn of(UUID userId, String email, String fullName,
                                    String ipAddress, String userAgent) {
            return UserLoggedIn.builder()
                    .userId(userId)
                    .userEmail(email)
                    .userFullName(fullName)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .timestamp(Instant.now())
                    .correlationId(UUID.randomUUID())
                    .build();
        }
    }

    /**
     * Evento de contraseña cambiada
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserPasswordChanged extends BaseUserEvent {
        private UUID userId;
        private String userEmail;
        private String userFullName;
        private Boolean wasReset; // true si fue reset por admin, false si cambio voluntario
        private UUID changedBy; // null si fue el propio usuario
        private Instant timestamp;
        private UUID correlationId;
        private Map<String, Object> metadata;

        public static UserPasswordChanged of(UUID userId, String email, String fullName,
                                           Boolean wasReset, UUID changedBy) {
            return UserPasswordChanged.builder()
                    .userId(userId)
                    .userEmail(email)
                    .userFullName(fullName)
                    .wasReset(wasReset)
                    .changedBy(changedBy)
                    .timestamp(Instant.now())
                    .correlationId(UUID.randomUUID())
                    .build();
        }
    }

    /**
     * Evento de email verificado
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserEmailVerified extends BaseUserEvent {
        private UUID userId;
        private String userEmail;
        private String userFullName;
        private String verificationMethod; // EMAIL_LINK, ADMIN_OVERRIDE, etc.
        private UUID verifiedBy;
        private Instant timestamp;
        private UUID correlationId;
        private Map<String, Object> metadata;

        public static UserEmailVerified of(UUID userId, String email, String fullName,
                                         String method, UUID verifiedBy) {
            return UserEmailVerified.builder()
                    .userId(userId)
                    .userEmail(email)
                    .userFullName(fullName)
                    .verificationMethod(method)
                    .verifiedBy(verifiedBy)
                    .timestamp(Instant.now())
                    .correlationId(UUID.randomUUID())
                    .build();
        }
    }

    /**
     * Constantes para tipos de eventos
     */
    public static class EventTypes {
        public static final String USER_CREATED = "USER_CREATED";
        public static final String USER_UPDATED = "USER_UPDATED";
        public static final String USER_DELETED = "USER_DELETED";
        public static final String USER_ROLES_CHANGED = "USER_ROLES_CHANGED";
        public static final String USER_STATUS_CHANGED = "USER_STATUS_CHANGED";
        public static final String USER_LOGGED_IN = "USER_LOGGED_IN";
        public static final String USER_PASSWORD_CHANGED = "USER_PASSWORD_CHANGED";
        public static final String USER_EMAIL_VERIFIED = "USER_EMAIL_VERIFIED";
    }

    /**
     * Constantes para tipos de agregado
     */
    public static class AggregateTypes {
        public static final String USER = "USER";
        public static final String USER_ROLE = "USER_ROLE";
    }
}