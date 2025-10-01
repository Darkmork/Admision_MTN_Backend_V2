// user-service/src/main/java/com/desafios/mtn/userservice/web/controller/UserController.java

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "API para gestión de usuarios del sistema")
public class UserController {

    private final UserService userService;

    @Operation(
        summary = "Listar usuarios con filtros",
        description = "Obtiene una lista paginada de usuarios con filtros opcionales"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Lista de usuarios obtenida exitosamente",
            content = @Content(schema = @Schema(implementation = UserDto.UserSummary.class))
        ),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<Page<UserDto.UserSummary>> getAllUsers(
            @Parameter(description = "Filtro por email")
            @RequestParam(required = false) String email,
            
            @Parameter(description = "Filtro por nombre")
            @RequestParam(required = false) String firstName,
            
            @Parameter(description = "Filtro por apellido")
            @RequestParam(required = false) String lastName,
            
            @Parameter(description = "Filtro por rol")
            @RequestParam(required = false) String role,
            
            @Parameter(description = "Filtro por estado activo")
            @RequestParam(required = false) Boolean enabled,
            
            @Parameter(description = "Parámetros de paginación")
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("Obteniendo lista de usuarios con filtros - email: {}, firstName: {}, lastName: {}, role: {}, enabled: {}",
                email, firstName, lastName, role, enabled);
        
        Page<UserDto.UserSummary> users = userService.getAllUsersWithFilters(
            email, firstName, lastName, role, enabled, pageable
        );
        
        return ResponseEntity.ok(users);
    }

    @Operation(
        summary = "Obtener usuario por ID",
        description = "Obtiene los detalles completos de un usuario específico"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Usuario encontrado",
            content = @Content(schema = @Schema(implementation = UserDto.class))
        ),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<UserDto> getUserById(
            @Parameter(description = "ID del usuario")
            @PathVariable UUID id
    ) {
        log.info("Obteniendo usuario por ID: {}", id);
        
        UserDto user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @Operation(
        summary = "Crear nuevo usuario",
        description = "Crea un nuevo usuario en el sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", 
            description = "Usuario creado exitosamente",
            content = @Content(schema = @Schema(implementation = UserDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "409", description = "Usuario ya existe"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> createUser(
            @Parameter(description = "Datos del nuevo usuario")
            @Valid @RequestBody UserDto.CreateUserRequest createRequest,
            
            @Parameter(description = "Clave de idempotencia")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        log.info("Creando nuevo usuario con email: {} (Idempotency-Key: {})", 
                createRequest.getEmail(), idempotencyKey);
        
        UserDto createdUser = userService.createUser(createRequest, idempotencyKey);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @Operation(
        summary = "Actualizar usuario",
        description = "Actualiza los datos de un usuario existente"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Usuario actualizado exitosamente",
            content = @Content(schema = @Schema(implementation = UserDto.class))
        ),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateUser(
            @Parameter(description = "ID del usuario")
            @PathVariable UUID id,
            
            @Parameter(description = "Datos actualizados del usuario")
            @Valid @RequestBody UserDto.UpdateUserRequest updateRequest
    ) {
        log.info("Actualizando usuario con ID: {}", id);
        
        UserDto updatedUser = userService.updateUser(id, updateRequest);
        
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(
        summary = "Cambiar roles de usuario",
        description = "Asigna, remueve o reemplaza los roles de un usuario"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Roles actualizados exitosamente",
            content = @Content(schema = @Schema(implementation = UserDto.class))
        ),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
        @ApiResponse(responseCode = "400", description = "Roles inválidos"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> changeUserRoles(
            @Parameter(description = "ID del usuario")
            @PathVariable UUID id,
            
            @Parameter(description = "Configuración de roles")
            @Valid @RequestBody UserDto.ChangeRolesRequest rolesRequest
    ) {
        log.info("Cambiando roles del usuario ID: {} - Operación: {} - Roles: {}", 
                id, rolesRequest.getOperation(), rolesRequest.getRoles());
        
        UserDto updatedUser = userService.changeUserRoles(id, rolesRequest);
        
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(
        summary = "Cambiar estado de usuario",
        description = "Activa o desactiva un usuario del sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Estado actualizado exitosamente",
            content = @Content(schema = @Schema(implementation = UserDto.class))
        ),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> changeUserStatus(
            @Parameter(description = "ID del usuario")
            @PathVariable UUID id,
            
            @Parameter(description = "Nuevo estado del usuario")
            @Valid @RequestBody UserDto.ChangeStatusRequest statusRequest
    ) {
        log.info("Cambiando estado del usuario ID: {} - Habilitado: {} - Razón: {}", 
                id, statusRequest.getEnabled(), statusRequest.getReason());
        
        UserDto updatedUser = userService.changeUserStatus(id, statusRequest);
        
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(
        summary = "Resetear contraseña",
        description = "Genera una nueva contraseña temporal para el usuario"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Contraseña reseteada exitosamente"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resetPassword(
            @Parameter(description = "ID del usuario")
            @PathVariable UUID id,
            
            @Parameter(description = "Enviar contraseña por email")
            @RequestParam(defaultValue = "true") boolean sendEmail
    ) {
        log.info("Reseteando contraseña para usuario ID: {} - Enviar email: {}", id, sendEmail);
        
        userService.resetPassword(id, sendEmail);
        
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Eliminar usuario",
        description = "Elimina permanentemente un usuario del sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Usuario eliminado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
        @ApiResponse(responseCode = "400", description = "Usuario no se puede eliminar"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID del usuario")
            @PathVariable UUID id,
            
            @Parameter(description = "Forzar eliminación")
            @RequestParam(defaultValue = "false") boolean force
    ) {
        log.info("Eliminando usuario ID: {} - Forzar: {}", id, force);
        
        userService.deleteUser(id, force);
        
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Obtener estadísticas de usuarios",
        description = "Obtiene estadísticas generales del sistema de usuarios"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Estadísticas obtenidas exitosamente",
            content = @Content(schema = @Schema(implementation = UserDto.UserStatistics.class))
        ),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto.UserStatistics> getUserStatistics() {
        log.info("Obteniendo estadísticas de usuarios");
        
        UserDto.UserStatistics statistics = userService.getUserStatistics();
        
        return ResponseEntity.ok(statistics);
    }

    @Operation(
        summary = "Verificar email de usuario",
        description = "Marca el email de un usuario como verificado"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Email verificado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PostMapping("/{id}/verify-email")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> verifyEmail(
            @Parameter(description = "ID del usuario")
            @PathVariable UUID id
    ) {
        log.info("Verificando email para usuario ID: {}", id);
        
        userService.verifyUserEmail(id);
        
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Buscar usuarios",
        description = "Búsqueda avanzada de usuarios por múltiples criterios"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Resultados de búsqueda obtenidos",
            content = @Content(schema = @Schema(implementation = UserDto.UserSummary.class))
        ),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<Page<UserDto.UserSummary>> searchUsers(
            @Parameter(description = "Término de búsqueda general")
            @RequestParam String query,
            
            @Parameter(description = "Filtros específicos")
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Boolean emailVerified,
            
            @Parameter(description = "Parámetros de paginación")
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("Buscando usuarios con query: '{}', role: {}, enabled: {}, emailVerified: {}", 
                query, role, enabled, emailVerified);
        
        Page<UserDto.UserSummary> results = userService.searchUsers(
            query, role, enabled, emailVerified, pageable
        );
        
        return ResponseEntity.ok(results);
    }
}