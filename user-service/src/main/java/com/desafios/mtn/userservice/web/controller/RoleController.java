// user-service/src/main/java/com/desafios/mtn/userservice/web/controller/RoleController.java

package com.desafios.mtn.userservice.web.controller;

import com.desafios.mtn.userservice.service.RoleService;
import com.desafios.mtn.userservice.web.dto.RoleDto;
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

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Tag(name = "Role Management", description = "API para gestión de roles del sistema")
public class RoleController {

    private final RoleService roleService;

    @Operation(
        summary = "Listar todos los roles",
        description = "Obtiene una lista paginada de todos los roles del sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Lista de roles obtenida exitosamente",
            content = @Content(schema = @Schema(implementation = RoleDto.RoleSummary.class))
        ),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<Page<RoleDto.RoleSummary>> getAllRoles(
            @Parameter(description = "Filtro por categoría de rol")
            @RequestParam(required = false) String category,
            
            @Parameter(description = "Filtro por estado habilitado")
            @RequestParam(required = false) Boolean enabled,
            
            @Parameter(description = "Incluir solo roles del sistema")
            @RequestParam(required = false) Boolean systemRole,
            
            @Parameter(description = "Parámetros de paginación")
            @PageableDefault(size = 50) Pageable pageable
    ) {
        log.info("Obteniendo lista de roles - category: {}, enabled: {}, systemRole: {}", 
                category, enabled, systemRole);
        
        Page<RoleDto.RoleSummary> roles = roleService.getAllRolesWithFilters(
            category, enabled, systemRole, pageable
        );
        
        return ResponseEntity.ok(roles);
    }

    @Operation(
        summary = "Obtener roles disponibles",
        description = "Obtiene lista simple de roles para selección en formularios"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Lista de roles disponibles",
            content = @Content(schema = @Schema(implementation = RoleDto.RoleSummary.class))
        ),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<List<RoleDto.RoleSummary>> getAvailableRoles(
            @Parameter(description = "Solo roles habilitados")
            @RequestParam(defaultValue = "true") boolean enabledOnly
    ) {
        log.info("Obteniendo roles disponibles - enabledOnly: {}", enabledOnly);
        
        List<RoleDto.RoleSummary> roles = roleService.getAvailableRoles(enabledOnly);
        
        return ResponseEntity.ok(roles);
    }

    @Operation(
        summary = "Obtener rol por ID",
        description = "Obtiene los detalles completos de un rol específico"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Rol encontrado",
            content = @Content(schema = @Schema(implementation = RoleDto.class))
        ),
        @ApiResponse(responseCode = "404", description = "Rol no encontrado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<RoleDto> getRoleById(
            @Parameter(description = "ID del rol")
            @PathVariable UUID id
    ) {
        log.info("Obteniendo rol por ID: {}", id);
        
        RoleDto role = roleService.getRoleById(id);
        return ResponseEntity.ok(role);
    }

    @Operation(
        summary = "Obtener detalles completos del rol",
        description = "Obtiene información detallada del rol incluyendo usuarios asignados"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Detalles del rol obtenidos",
            content = @Content(schema = @Schema(implementation = RoleDto.RoleDetails.class))
        ),
        @ApiResponse(responseCode = "404", description = "Rol no encontrado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @GetMapping("/{id}/details")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<RoleDto.RoleDetails> getRoleDetails(
            @Parameter(description = "ID del rol")
            @PathVariable UUID id
    ) {
        log.info("Obteniendo detalles del rol ID: {}", id);
        
        RoleDto.RoleDetails roleDetails = roleService.getRoleDetails(id);
        return ResponseEntity.ok(roleDetails);
    }

    @Operation(
        summary = "Crear nuevo rol",
        description = "Crea un nuevo rol personalizado en el sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", 
            description = "Rol creado exitosamente",
            content = @Content(schema = @Schema(implementation = RoleDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "409", description = "Rol ya existe"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoleDto> createRole(
            @Parameter(description = "Datos del nuevo rol")
            @Valid @RequestBody RoleDto.CreateRoleRequest createRequest,
            
            @Parameter(description = "Clave de idempotencia")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        log.info("Creando nuevo rol con nombre: {} (Idempotency-Key: {})", 
                createRequest.getName(), idempotencyKey);
        
        RoleDto createdRole = roleService.createRole(createRequest, idempotencyKey);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRole);
    }

    @Operation(
        summary = "Actualizar rol",
        description = "Actualiza los datos de un rol existente (solo roles personalizados)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Rol actualizado exitosamente",
            content = @Content(schema = @Schema(implementation = RoleDto.class))
        ),
        @ApiResponse(responseCode = "404", description = "Rol no encontrado"),
        @ApiResponse(responseCode = "400", description = "No se puede modificar rol del sistema"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoleDto> updateRole(
            @Parameter(description = "ID del rol")
            @PathVariable UUID id,
            
            @Parameter(description = "Datos actualizados del rol")
            @Valid @RequestBody RoleDto.UpdateRoleRequest updateRequest
    ) {
        log.info("Actualizando rol con ID: {}", id);
        
        RoleDto updatedRole = roleService.updateRole(id, updateRequest);
        
        return ResponseEntity.ok(updatedRole);
    }

    @Operation(
        summary = "Cambiar estado del rol",
        description = "Habilita o deshabilita un rol del sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Estado del rol actualizado exitosamente",
            content = @Content(schema = @Schema(implementation = RoleDto.class))
        ),
        @ApiResponse(responseCode = "404", description = "Rol no encontrado"),
        @ApiResponse(responseCode = "400", description = "No se puede deshabilitar rol con usuarios asignados"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoleDto> changeRoleStatus(
            @Parameter(description = "ID del rol")
            @PathVariable UUID id,
            
            @Parameter(description = "Nuevo estado del rol")
            @Valid @RequestBody RoleDto.ChangeRoleStatusRequest statusRequest
    ) {
        log.info("Cambiando estado del rol ID: {} - Habilitado: {} - Razón: {}", 
                id, statusRequest.getEnabled(), statusRequest.getReason());
        
        RoleDto updatedRole = roleService.changeRoleStatus(id, statusRequest);
        
        return ResponseEntity.ok(updatedRole);
    }

    @Operation(
        summary = "Eliminar rol",
        description = "Elimina un rol personalizado del sistema (solo si no tiene usuarios asignados)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Rol eliminado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Rol no encontrado"),
        @ApiResponse(responseCode = "400", description = "No se puede eliminar rol del sistema o con usuarios asignados"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRole(
            @Parameter(description = "ID del rol")
            @PathVariable UUID id,
            
            @Parameter(description = "Forzar eliminación (reasignar usuarios a rol por defecto)")
            @RequestParam(defaultValue = "false") boolean force
    ) {
        log.info("Eliminando rol ID: {} - Forzar: {}", id, force);
        
        roleService.deleteRole(id, force);
        
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Obtener estadísticas de roles",
        description = "Obtiene estadísticas generales del sistema de roles"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Estadísticas obtenidas exitosamente",
            content = @Content(schema = @Schema(implementation = RoleDto.RoleStatistics.class))
        ),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoleDto.RoleStatistics> getRoleStatistics() {
        log.info("Obteniendo estadísticas de roles");
        
        RoleDto.RoleStatistics statistics = roleService.getRoleStatistics();
        
        return ResponseEntity.ok(statistics);
    }

    @Operation(
        summary = "Obtener roles por categoría",
        description = "Obtiene todos los roles de una categoría específica"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Roles de la categoría obtenidos",
            content = @Content(schema = @Schema(implementation = RoleDto.RoleSummary.class))
        ),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<List<RoleDto.RoleSummary>> getRolesByCategory(
            @Parameter(description = "Categoría del rol")
            @PathVariable String category,
            
            @Parameter(description = "Solo roles habilitados")
            @RequestParam(defaultValue = "true") boolean enabledOnly
    ) {
        log.info("Obteniendo roles por categoría: {} - enabledOnly: {}", category, enabledOnly);
        
        List<RoleDto.RoleSummary> roles = roleService.getRolesByCategory(category, enabledOnly);
        
        return ResponseEntity.ok(roles);
    }

    @Operation(
        summary = "Validar nombre de rol",
        description = "Valida si un nombre de rol está disponible"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Nombre validado"),
        @ApiResponse(responseCode = "409", description = "Nombre ya existe"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @GetMapping("/validate-name")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> validateRoleName(
            @Parameter(description = "Nombre del rol a validar")
            @RequestParam String name,
            
            @Parameter(description = "ID del rol a excluir (para actualizaciones)")
            @RequestParam(required = false) UUID excludeId
    ) {
        log.info("Validando nombre de rol: {} - Excluir ID: {}", name, excludeId);
        
        boolean isAvailable = roleService.isRoleNameAvailable(name, excludeId);
        
        if (isAvailable) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}