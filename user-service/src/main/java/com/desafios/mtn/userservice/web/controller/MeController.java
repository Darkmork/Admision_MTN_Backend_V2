// user-service/src/main/java/com/desafios/mtn/userservice/web/controller/MeController.java

package com.desafios.mtn.userservice.web.controller;

import com.desafios.mtn.userservice.service.UserService;
import com.desafios.mtn.userservice.web.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "API para gestión del perfil del usuario autenticado")
public class MeController {

    private final UserService userService;

    @Operation(
        summary = "Obtener perfil propio",
        description = "Obtiene la información del perfil del usuario autenticado"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Perfil obtenido exitosamente",
            content = @Content(schema = @Schema(implementation = UserDto.UserProfile.class))
        ),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @GetMapping
    public ResponseEntity<UserDto.UserProfile> getMyProfile(Authentication authentication) {
        String userEmail = extractEmailFromAuthentication(authentication);
        log.info("Obteniendo perfil propio para usuario: {}", userEmail);
        
        UserDto.UserProfile profile = userService.getUserProfileByEmail(userEmail);
        
        return ResponseEntity.ok(profile);
    }

    @Operation(
        summary = "Actualizar perfil propio",
        description = "Actualiza la información personal del usuario autenticado"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Perfil actualizado exitosamente",
            content = @Content(schema = @Schema(implementation = UserDto.UserProfile.class))
        ),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PutMapping
    public ResponseEntity<UserDto.UserProfile> updateMyProfile(
            Authentication authentication,
            @Parameter(description = "Datos actualizados del perfil")
            @Valid @RequestBody UserDto.UpdateProfileRequest updateRequest
    ) {
        String userEmail = extractEmailFromAuthentication(authentication);
        log.info("Actualizando perfil propio para usuario: {}", userEmail);
        
        UserDto.UserProfile updatedProfile = userService.updateUserProfile(userEmail, updateRequest);
        
        return ResponseEntity.ok(updatedProfile);
    }

    @Operation(
        summary = "Cambiar contraseña",
        description = "Cambia la contraseña del usuario autenticado"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Contraseña cambiada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Contraseña actual incorrecta o nueva contraseña inválida"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PutMapping("/password")
    public ResponseEntity<Void> changeMyPassword(
            Authentication authentication,
            @Parameter(description = "Datos para cambio de contraseña")
            @Valid @RequestBody UserDto.ChangePasswordRequest passwordRequest
    ) {
        String userEmail = extractEmailFromAuthentication(authentication);
        log.info("Cambiando contraseña para usuario: {}", userEmail);
        
        userService.changeUserPassword(userEmail, passwordRequest);
        
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Verificar contraseña actual",
        description = "Verifica si la contraseña proporcionada es correcta"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Contraseña verificada"),
        @ApiResponse(responseCode = "400", description = "Contraseña incorrecta"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PostMapping("/verify-password")
    public ResponseEntity<Void> verifyCurrentPassword(
            Authentication authentication,
            @Parameter(description = "Contraseña a verificar")
            @Valid @RequestBody UserDto.VerifyPasswordRequest verifyRequest
    ) {
        String userEmail = extractEmailFromAuthentication(authentication);
        log.info("Verificando contraseña actual para usuario: {}", userEmail);
        
        boolean isValid = userService.verifyCurrentPassword(userEmail, verifyRequest.getCurrentPassword());
        
        if (isValid) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
        summary = "Obtener actividad reciente",
        description = "Obtiene el historial de actividad reciente del usuario"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Actividad obtenida exitosamente",
            content = @Content(schema = @Schema(implementation = UserDto.UserActivity.class))
        ),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @GetMapping("/activity")
    public ResponseEntity<UserDto.UserActivity> getMyActivity(
            Authentication authentication,
            @Parameter(description = "Número de días de historial")
            @RequestParam(defaultValue = "30") int days
    ) {
        String userEmail = extractEmailFromAuthentication(authentication);
        log.info("Obteniendo actividad reciente para usuario: {} (últimos {} días)", userEmail, days);
        
        UserDto.UserActivity activity = userService.getUserActivity(userEmail, days);
        
        return ResponseEntity.ok(activity);
    }

    @Operation(
        summary = "Actualizar configuraciones de usuario",
        description = "Actualiza las preferencias y configuraciones del usuario"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuraciones actualizadas exitosamente"),
        @ApiResponse(responseCode = "400", description = "Configuraciones inválidas"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PutMapping("/preferences")
    public ResponseEntity<Void> updateMyPreferences(
            Authentication authentication,
            @Parameter(description = "Nuevas configuraciones del usuario")
            @Valid @RequestBody UserDto.UserPreferences preferences
    ) {
        String userEmail = extractEmailFromAuthentication(authentication);
        log.info("Actualizando preferencias para usuario: {}", userEmail);
        
        userService.updateUserPreferences(userEmail, preferences);
        
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Obtener configuraciones de usuario",
        description = "Obtiene las preferencias y configuraciones actuales del usuario"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Configuraciones obtenidas exitosamente",
            content = @Content(schema = @Schema(implementation = UserDto.UserPreferences.class))
        ),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @GetMapping("/preferences")
    public ResponseEntity<UserDto.UserPreferences> getMyPreferences(Authentication authentication) {
        String userEmail = extractEmailFromAuthentication(authentication);
        log.info("Obteniendo preferencias para usuario: {}", userEmail);
        
        UserDto.UserPreferences preferences = userService.getUserPreferences(userEmail);
        
        return ResponseEntity.ok(preferences);
    }

    @Operation(
        summary = "Eliminar cuenta propia",
        description = "Solicita la eliminación de la cuenta del usuario autenticado"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Solicitud de eliminación registrada"),
        @ApiResponse(responseCode = "400", description = "No se puede eliminar la cuenta"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @DeleteMapping
    public ResponseEntity<Void> deleteMyAccount(
            Authentication authentication,
            @Parameter(description = "Confirmación de eliminación")
            @Valid @RequestBody UserDto.DeleteAccountRequest deleteRequest
    ) {
        String userEmail = extractEmailFromAuthentication(authentication);
        log.info("Solicitando eliminación de cuenta para usuario: {}", userEmail);
        
        userService.requestAccountDeletion(userEmail, deleteRequest);
        
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Registrar último login",
        description = "Actualiza la fecha y hora del último acceso del usuario"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Último login registrado"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PostMapping("/login")
    public ResponseEntity<Void> recordLastLogin(
            Authentication authentication,
            @Parameter(description = "Información adicional del login")
            @RequestBody(required = false) UserDto.LoginInfo loginInfo
    ) {
        String userEmail = extractEmailFromAuthentication(authentication);
        log.info("Registrando último login para usuario: {}", userEmail);
        
        userService.recordUserLogin(userEmail, loginInfo);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Extrae el email del usuario desde el token JWT
     */
    private String extractEmailFromAuthentication(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            // Intenta obtener el email desde diferentes claims posibles en Keycloak
            String email = jwt.getClaimAsString("email");
            if (email != null) {
                return email;
            }
            
            // Fallback a preferred_username si no hay email
            String username = jwt.getClaimAsString("preferred_username");
            if (username != null) {
                return username;
            }
            
            // Último fallback al subject
            return jwt.getSubject();
        }
        
        return authentication.getName();
    }
}