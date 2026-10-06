package co.edu.uniquindio.littlestyle.modules.auth.controller;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.auth.dto.ActualizarCuentaRequest;
import co.edu.uniquindio.littlestyle.modules.auth.dto.AuthResponse;
import co.edu.uniquindio.littlestyle.modules.auth.dto.LoginRequest;
import co.edu.uniquindio.littlestyle.modules.auth.dto.RegistroRequest;
import co.edu.uniquindio.littlestyle.modules.auth.dto.UsuarioResponse;
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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> perfil(@AuthenticationPrincipal AuthenticatedUser usuario) {
        return ResponseEntity.ok(authService.obtenerPerfil(usuario.email()));
    }

    @PutMapping("/me")
    public ResponseEntity<AuthResponse> actualizarCuenta(
            @AuthenticationPrincipal AuthenticatedUser usuario,
            @Valid @RequestBody ActualizarCuentaRequest request) {
        return ResponseEntity.ok(authService.actualizarCuenta(usuario.id(), request));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> desactivarCuenta(@AuthenticationPrincipal AuthenticatedUser usuario) {
        authService.desactivarCuenta(usuario.id());
        return ResponseEntity.noContent().build();
    }
}
