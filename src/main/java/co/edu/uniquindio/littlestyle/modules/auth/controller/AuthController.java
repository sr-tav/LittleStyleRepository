package co.edu.uniquindio.littlestyle.modules.auth.controller;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.auth.dto.ActualizarCuentaRequest;
import co.edu.uniquindio.littlestyle.modules.auth.dto.AuthResponse;
import co.edu.uniquindio.littlestyle.modules.auth.dto.LoginRequest;
import co.edu.uniquindio.littlestyle.modules.auth.dto.RegistroRequest;
import co.edu.uniquindio.littlestyle.modules.auth.dto.UsuarioResponse;
import co.edu.uniquindio.littlestyle.modules.auth.dto.PasswordResetRequest;
import co.edu.uniquindio.littlestyle.modules.auth.dto.RestablecerPasswordRequest;
import co.edu.uniquindio.littlestyle.modules.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

/** Endpoints de autenticación y gestión de la cuenta propia ({@code /api/auth}). */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Registra un cliente o vendedor y responde 201 con el token para que quede autenticado de inmediato. */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    /** Inicia sesión con correo y contraseña. La IP del cliente se usa para limitar los intentos fallidos. */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, httpRequest.getRemoteAddr()));
    }

    /** Devuelve los datos del usuario autenticado. */
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> perfil(@AuthenticationPrincipal AuthenticatedUser usuario) {
        return ResponseEntity.ok(authService.obtenerPerfil(usuario.email()));
    }

    /**
     * Actualiza los datos de la cuenta propia (y la contraseña si se envía la actual). Responde con un token
     * nuevo porque el correo o el nombre pueden haber cambiado.
     */
    @PutMapping("/me")
    public ResponseEntity<AuthResponse> actualizarCuenta(
            @AuthenticationPrincipal AuthenticatedUser usuario,
            @Valid @RequestBody ActualizarCuentaRequest request) {
        return ResponseEntity.ok(authService.actualizarCuenta(usuario.id(), request));
    }

    /** Elimina la cuenta propia anonimizando sus datos personales. Responde 204. */
    @DeleteMapping("/me")
    public ResponseEntity<Void> desactivarCuenta(@AuthenticationPrincipal AuthenticatedUser usuario) {
        authService.desactivarCuenta(usuario.id());
        return ResponseEntity.noContent().build();
    }

    /** Recupera la contraseña del usuario. */
    @GetMapping("/me/reset-password")
    public ResponseEntity<PasswordResetRequest> recuperarContraseña(@AuthenticationPrincipal AuthenticatedUser usuario) {
        return ResponseEntity.ok(authService.recuperarContraseña(usuario.email()));
    }

    /** Restablece la contraseña del usuario. */
    @PostMapping("/me/reset-password")
    public ResponseEntity<AuthResponse> restablecerContraseña(@Valid @RequestBody RestablecerPasswordRequest request,
                                                              @AuthenticationPrincipal AuthenticatedUser usuario) {
        return ResponseEntity.ok(authService.restablecerContraseña(usuario.id(), request));
    }

    /** Solicita el envío del enlace de recuperación al correo del usuario. */
    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> solicitarRecuperacion(@Valid @RequestBody PasswordResetRequest request) {
        authService.solicitarRecuperacion(request);
        return ResponseEntity.accepted().build();
    }

    /** Restablece la contraseña con el token recibido por correo. */
    @PostMapping("/password-reset/reset")
    public ResponseEntity<Void> restablecerPassword(@Valid @RequestBody RestablecerPasswordRequest request) {
        authService.restablecerPassword(request);
        return ResponseEntity.noContent().build();
    }
}
