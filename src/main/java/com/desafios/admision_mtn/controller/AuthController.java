package com.desafios.admision_mtn.controller;

import com.desafios.admision_mtn.dto.AuthResponse;
import com.desafios.admision_mtn.dto.LoginRequest;
import com.desafios.admision_mtn.dto.RegisterRequest;
import com.desafios.admision_mtn.entity.User;
import com.desafios.admision_mtn.service.UserService;
import com.desafios.admision_mtn.security.RateLimitingService;
import com.desafios.admision_mtn.security.SecurityValidationService;
import com.desafios.admision_mtn.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Endpoints de autenticación y autorización para el sistema de admisión")
// 🔒 SEGURIDAD: Sin @CrossOrigin - usa configuración global de SecurityConfig
public class AuthController {
    
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;
    private final RateLimitingService rateLimitingService;
    private final SecurityValidationService securityValidationService;
    
    @Operation(
        summary = "Iniciar sesión en el sistema", 
        description = "Autentica un usuario (administrador o apoderado) y retorna un token JWT para acceder a los endpoints protegidos."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Autenticación exitosa",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class),
                examples = @ExampleObject(value = """
                    {
                        "token": "eyJhbGciOiJIUzUxMiJ9...",
                        "email": "admin@mtn.cl",
                        "firstName": "Administrador",
                        "lastName": "Sistema",
                        "role": "ADMIN",
                        "success": true,
                        "message": "Autenticación exitosa"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Credenciales inválidas",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class),
                examples = @ExampleObject(value = """
                    {
                        "success": false,
                        "message": "Credenciales inválidas"
                    }
                    """)
            )
        )
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
        @Parameter(
            description = "Credenciales de login del usuario",
            required = true,
            schema = @Schema(implementation = LoginRequest.class)
        )
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpRequest) {
        
        String clientIp = getClientIpAddress(httpRequest);
        
        // Verificar rate limiting para intentos de login
        if (!rateLimitingService.isLoginAllowed(clientIp)) {
            log.warn("🚨 Login bloqueado por rate limiting para IP: {}", clientIp);
            return ResponseEntity.status(429)
                .body(AuthResponse.error("Demasiados intentos de login. Intente nuevamente más tarde."));
        }
        
        // Validaciones de seguridad en los datos de entrada
        if (securityValidationService.containsSqlInjection(request.getEmail()) ||
            securityValidationService.containsXss(request.getEmail())) {
            log.warn("🚨 Intento de ataque detectado en login desde IP: {}", clientIp);
            rateLimitingService.recordFailedLogin(clientIp);
            return ResponseEntity.badRequest()
                .body(AuthResponse.error("Datos de entrada no válidos"));
        }
        
        try {
            // Usar nuestro servicio de autenticación
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            
            // Buscar el usuario después de la autenticación exitosa
            User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            String token = jwtService.generateToken(user);
            
            log.info("✅ Login exitoso para usuario: {} desde IP: {}", user.getEmail(), clientIp);
            
            return ResponseEntity.ok(AuthResponse.success(
                token, 
                user.getEmail(), 
                user.getFirstName(), 
                user.getLastName(), 
                user.getRole().name()
            ));
            
        } catch (Exception e) {
            log.error("Login failed for email: {} from IP: {}", request.getEmail(), clientIp, e);
            
            // Registrar intento fallido para rate limiting
            rateLimitingService.recordFailedLogin(clientIp);
            return ResponseEntity.badRequest().body(
                AuthResponse.error("Credenciales inválidas")
            );
        }
    }
    
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                               HttpServletRequest httpRequest) {
        
        String clientIp = getClientIpAddress(httpRequest);
        
        // Validaciones de seguridad adicionales
        SecurityValidationService.EmailValidationResult emailResult = 
            securityValidationService.validateEmail(request.getEmail());
        
        if (!emailResult.isValid()) {
            log.warn("🚨 Intento de registro con email inválido desde IP: {}", clientIp);
            return ResponseEntity.badRequest()
                .body(AuthResponse.error("Email no válido: " + String.join(", ", emailResult.violations())));
        }
        
        SecurityValidationService.PasswordValidationResult passwordResult = 
            securityValidationService.validatePasswordStrength(request.getPassword());
        
        if (!passwordResult.isValid()) {
            return ResponseEntity.badRequest()
                .body(AuthResponse.error("Contraseña no segura: " + String.join(", ", passwordResult.violations())));
        }
        
        // Validar datos de entrada contra XSS/SQL injection
        if (securityValidationService.containsSqlInjection(request.getFirstName()) ||
            securityValidationService.containsSqlInjection(request.getLastName()) ||
            securityValidationService.containsXss(request.getFirstName()) ||
            securityValidationService.containsXss(request.getLastName())) {
            
            log.warn("🚨 Intento de ataque en registro desde IP: {}", clientIp);
            return ResponseEntity.badRequest()
                .body(AuthResponse.error("Datos de entrada contienen caracteres no válidos"));
        }
        
        try {
            User user = userService.registerUser(request);
            String token = jwtService.generateToken(user);
            
            log.info("✅ Registro exitoso para usuario: {} desde IP: {}", user.getEmail(), clientIp);
            
            return ResponseEntity.ok(AuthResponse.success(
                token, 
                user.getEmail(), 
                user.getFirstName(), 
                user.getLastName(), 
                user.getRole().name()
            ));
            
        } catch (Exception e) {
            log.error("Registration failed for email: {} from IP: {}", request.getEmail(), clientIp, e);
            return ResponseEntity.badRequest().body(
                AuthResponse.error(e.getMessage())
            );
        }
    }
    
    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmailExists(@RequestParam String email) {
        // Validar email antes de verificar existencia
        if (securityValidationService.containsSqlInjection(email) ||
            securityValidationService.containsXss(email)) {
            return ResponseEntity.badRequest().build();
        }
        
        boolean exists = userService.existsByEmail(email);
        return ResponseEntity.ok(exists);
    }
    
    // Endpoint temporal de debug para diagnosticar problema de BD
    @GetMapping("/debug/user")
    public ResponseEntity<String> debugUser(@RequestParam String email) {
        try {
            log.info("🔍 DEBUG: Buscando usuario: {}", email);
            java.util.Optional<com.desafios.admision_mtn.entity.User> user = userService.findByEmail(email);
            if (user.isPresent()) {
                com.desafios.admision_mtn.entity.User u = user.get();
                String result = String.format("✅ Usuario encontrado: ID=%d, Email=%s, Active=%s, EmailVerified=%s", 
                    u.getId(), u.getEmail(), u.getActive(), u.getEmailVerified());
                log.info(result);
                return ResponseEntity.ok(result);
            } else {
                String result = "❌ Usuario NO encontrado: " + email;
                log.error(result);
                return ResponseEntity.ok(result);
            }
        } catch (Exception e) {
            String error = "💥 Error en debug: " + e.getMessage();
            log.error(error, e);
            return ResponseEntity.ok(error);
        }
    }
    
    /**
     * Extrae la IP real del cliente considerando proxies y load balancers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader != null && !xForwardedForHeader.isEmpty()) {
            return xForwardedForHeader.split(",")[0].trim();
        }
        
        String xRealIpHeader = request.getHeader("X-Real-IP");
        if (xRealIpHeader != null && !xRealIpHeader.isEmpty()) {
            return xRealIpHeader;
        }
        
        return request.getRemoteAddr();
    }
}