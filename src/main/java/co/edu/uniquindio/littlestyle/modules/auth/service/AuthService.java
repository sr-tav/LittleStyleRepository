package co.edu.uniquindio.littlestyle.modules.auth.service;

import co.edu.uniquindio.littlestyle.config.security.JwtService;
import co.edu.uniquindio.littlestyle.modules.auth.dto.ActualizarCuentaRequest;
import co.edu.uniquindio.littlestyle.modules.auth.dto.AuthResponse;
import co.edu.uniquindio.littlestyle.modules.auth.dto.LoginRequest;
import co.edu.uniquindio.littlestyle.modules.auth.dto.PasswordResetRequest;
import co.edu.uniquindio.littlestyle.modules.auth.dto.RegistroRequest;
import co.edu.uniquindio.littlestyle.modules.auth.dto.RestablecerPasswordRequest;
import co.edu.uniquindio.littlestyle.modules.auth.dto.UsuarioResponse;
import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.model.PasswordResetToken;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.auth.repository.PasswordResetTokenRepository;
import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import co.edu.uniquindio.littlestyle.shared.correo.CorreoProperties;
import co.edu.uniquindio.littlestyle.shared.correo.CorreoService;
import co.edu.uniquindio.littlestyle.shared.event.CuentaEliminadaEvent;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import co.edu.uniquindio.littlestyle.shared.exception.EmailYaRegistradoException;
import co.edu.uniquindio.littlestyle.shared.exception.LoginBloqueadoException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import static co.edu.uniquindio.littlestyle.shared.util.SeguridadLog.LOG;
import static co.edu.uniquindio.littlestyle.shared.util.SeguridadLog.enmascararEmail;

/** Registro, inicio de sesión y gestión de la cuenta propia de los usuarios. */
@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String VERSION_TERMINOS_VIGENTE = "1.1";

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final IntentosLoginService intentosLoginService;
    private final CorreoService correoService;
    private final CorreoProperties correoProperties;

    @Transactional
    public AuthResponse registrar(RegistroRequest request) {
        if (request.rol() == Rol.ADMINISTRADOR) {
            LOG.warn("Registro rechazado: intento de auto-registro como administrador email={}",
                    enmascararEmail(request.email()));
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Solo es posible registrarse como cliente o vendedor", "rol");
        }
        if (!request.password().equals(request.confirmarPassword())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Las contraseñas no coinciden", "confirmarPassword");
        }

        boolean esVendedor = request.rol() == Rol.VENDEDOR;
        if (esVendedor && (request.nombreTienda() == null || request.nombreTienda().isBlank())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "El nombre de la tienda es obligatorio para vendedores", "nombreTienda");
        }

        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new EmailYaRegistradoException(email);
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.nombre().trim())
                .apellido(request.apellido().trim())
                .email(email)
                .telefono(request.telefono())
                .password(passwordEncoder.encode(request.password()))
                .rol(request.rol())
                .nombreTienda(esVendedor ? request.nombreTienda().trim() : null)
                .estado(EstadoUsuario.ACTIVO)
                .versionTerminos(VERSION_TERMINOS_VIGENTE)
                .fechaAceptacionTerminos(LocalDateTime.now())
                .build();

        return construirRespuesta(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request, String ip) {
        String email = normalizarEmail(request.email());
        intentosLoginService.bloqueoRestante(email, ip).ifPresent(restante -> {
            LOG.warn("Login rechazado: bloqueado por intentos fallidos email={} ip={}", enmascararEmail(email), ip);
            throw new LoginBloqueadoException(restante);
        });

        try {
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
        } catch (BadCredentialsException ex) {
            boolean bloqueado = intentosLoginService.registrarFallo(email, ip);
            LOG.warn("Login rechazado: credenciales inválidas email={} ip={}{}", enmascararEmail(email), ip,
                    bloqueado ? " (límite de intentos alcanzado, se bloquea temporalmente)" : "");
            throw ex;
        } catch (DisabledException ex) {
            EstadoUsuario estado = usuarioRepository.findByEmail(email)
                    .map(Usuario::getEstado)
                    .orElse(null);
            String mensaje = switch (estado == null ? EstadoUsuario.ELIMINADO : estado) {
                case SUSPENDIDO -> "Su cuenta está suspendida; se abrió una apelación y recibirá la respuesta por correo.";
                case BLOQUEADO -> "Su cuenta está bloqueada; se abrió una apelación y recibirá la respuesta por correo.";
                case ELIMINADO -> "La cuenta fue eliminada y no puede iniciar sesión.";
                case ACTIVO -> "No fue posible iniciar sesión con esta cuenta.";
            };
            LOG.warn("Login rechazado: cuenta deshabilitada estado={} email={} ip={}",
                    estado, enmascararEmail(email), ip);
            throw new BusinessException(HttpStatus.FORBIDDEN, mensaje);
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        intentosLoginService.registrarExito(email, ip);
        return construirRespuesta(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPerfil(String email) {
        return usuarioRepository.findByEmail(email)
                .map(UsuarioResponse::from)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    @Transactional
    public AuthResponse actualizarCuenta(Long id, ActualizarCuentaRequest request) {
        Usuario usuario = usuarioRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "La cuenta no está activa");
        }

        String email = request.email() == null || request.email().isBlank()
                ? usuario.getEmail()
                : normalizarEmail(request.email());
        if (!email.equals(usuario.getEmail()) && usuarioRepository.existsByEmail(email)) {
            throw new EmailYaRegistradoException(email);
        }

        boolean cambiarPassword = request.nuevaPassword() != null && !request.nuevaPassword().isBlank();
        boolean envioPasswordActual = request.passwordActual() != null && !request.passwordActual().isBlank();
        boolean envioConfirmacion = request.confirmarNuevaPassword() != null
                && !request.confirmarNuevaPassword().isBlank();
        if (cambiarPassword) {
            if (!envioPasswordActual || !passwordEncoder.matches(request.passwordActual(), usuario.getPassword())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "La contraseña actual no es correcta", "passwordActual");
            }
            if (!request.nuevaPassword().equals(request.confirmarNuevaPassword())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "Las contraseñas no coinciden", "confirmarNuevaPassword");
            }
        } else if (envioPasswordActual || envioConfirmacion) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Para cambiar la contraseña indique la nueva contraseña", "nuevaPassword");
        }

        if (request.nombre() != null && !request.nombre().isBlank()) {
            usuario.setNombre(request.nombre().trim());
        }
        if (request.apellido() != null && !request.apellido().isBlank()) {
            usuario.setApellido(request.apellido().trim());
        }
        usuario.setEmail(email);
        if (request.telefono() != null) {
            usuario.setTelefono(request.telefono().isBlank() ? null : request.telefono().trim());
        }
        if (usuario.getRol() == Rol.VENDEDOR && request.nombreTienda() != null) {
            if (request.nombreTienda().isBlank()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "El nombre de la tienda no puede estar vacío", "nombreTienda");
            }
            usuario.setNombreTienda(request.nombreTienda().trim());
        }
        if (cambiarPassword) {
            usuario.setPassword(passwordEncoder.encode(request.nuevaPassword()));
        }
        return construirRespuesta(usuarioRepository.save(usuario));
    }

    @Transactional
    public void desactivarCuenta(Long id) {
        Usuario usuario = usuarioRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (usuario.getRol() == Rol.ADMINISTRADOR) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "La cuenta administradora no se puede eliminar desde la aplicación");
        }
        eventPublisher.publishEvent(new CuentaEliminadaEvent(id));
        usuario.setNombre("Cuenta");
        usuario.setApellido("eliminada");
        usuario.setEmail("eliminada+" + UUID.randomUUID() + "@invalid.local");
        usuario.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        usuario.setTelefono(null);
        usuario.setNombreTienda(null);
        usuario.setFechaRegistro(LocalDateTime.of(1970, 1, 1, 0, 0));
        usuario.setVersionTerminos(null);
        usuario.setFechaAceptacionTerminos(null);
        usuario.setEstado(EstadoUsuario.ELIMINADO);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void solicitarRecuperacion(PasswordResetRequest request) {
        String email = normalizarEmail(request.email());
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "No existe una cuenta con ese correo"));

        passwordResetTokenRepository.deleteByEmail(email);
        String token = UUID.randomUUID().toString();
        LocalDateTime ahora = LocalDateTime.now();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .email(email)
                .createdAt(ahora)
                .expiresAt(ahora.plusMinutes(30))
                .build();
        passwordResetTokenRepository.save(resetToken);

        String base = correoProperties.resetUrlBase() == null || correoProperties.resetUrlBase().isBlank()
                ? "http://localhost:4200/auth/restablecer"
                : correoProperties.resetUrlBase().replaceAll("/+$", "");
        String url = base + "?token=" + token;
        correoService.enviarRestablecerPassword(usuario.getEmail(), nombreCompleto(usuario), url);
    }

    @Transactional(readOnly = true)
    public PasswordResetRequest recuperarContraseña(String email) {
        String correo = normalizarEmail(email);
        usuarioRepository.findByEmail(correo)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        return new PasswordResetRequest(correo);
    }

    @Transactional
    public AuthResponse restablecerContraseña(Long id, RestablecerPasswordRequest request) {
        if (!request.password().equals(request.confirmarPassword())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Las contraseñas no coinciden", "confirmarPassword");
        }

        Usuario usuario = usuarioRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuarioRepository.save(usuario);
        return construirRespuesta(usuario);
    }

    @Transactional
    public void restablecerPassword(RestablecerPasswordRequest request) {
        if (!request.password().equals(request.confirmarPassword())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Las contraseñas no coinciden", "confirmarPassword");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,
                        "El enlace de recuperación es inválido o ya no existe"));

        if (resetToken.getUsedAt() != null || LocalDateTime.now().isAfter(resetToken.getExpiresAt())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "El enlace de recuperación ha expirado o ya fue usado");
        }

        Usuario usuario = usuarioRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuarioRepository.save(usuario);
        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);
    }

    private AuthResponse construirRespuesta(Usuario usuario) {
        JwtService.TokenEmitido token = jwtService.generarToken(usuario);
        return AuthResponse.bearer(token.token(), token.expiraEn(), UsuarioResponse.from(usuario));
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String nombreCompleto(Usuario usuario) {
        String nombre = usuario.getNombre() == null ? "" : usuario.getNombre().trim();
        String apellido = usuario.getApellido() == null ? "" : usuario.getApellido().trim();
        return (nombre + " " + apellido).trim();
    }
}
