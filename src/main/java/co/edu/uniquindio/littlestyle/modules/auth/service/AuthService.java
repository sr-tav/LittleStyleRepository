package co.edu.uniquindio.littlestyle.modules.auth.service;

import co.edu.uniquindio.littlestyle.config.security.JwtService;
import co.edu.uniquindio.littlestyle.modules.auth.dto.ActualizarCuentaRequest;
import co.edu.uniquindio.littlestyle.modules.auth.dto.AuthResponse;
import co.edu.uniquindio.littlestyle.modules.auth.dto.LoginRequest;
import co.edu.uniquindio.littlestyle.modules.auth.dto.RegistroRequest;
import co.edu.uniquindio.littlestyle.modules.auth.dto.UsuarioResponse;
import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import co.edu.uniquindio.littlestyle.shared.exception.EmailYaRegistradoException;
import co.edu.uniquindio.littlestyle.shared.exception.LoginBloqueadoException;
import lombok.RequiredArgsConstructor;
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

import static co.edu.uniquindio.littlestyle.shared.util.SeguridadLog.LOG;
import static co.edu.uniquindio.littlestyle.shared.util.SeguridadLog.enmascararEmail;

@Service
@RequiredArgsConstructor
public class AuthService {

    /**
     * Versión de los términos y la política de tratamiento de datos que se muestran en el registro.
     * Debe coincidir con {@code VERSION_TERMINOS} en el frontend (terminos-contenido.ts).
     */
    public static final String VERSION_TERMINOS_VIGENTE = "1.0";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final IntentosLoginService intentosLoginService;

    /**
     * Registra un cliente o vendedor y devuelve un token para que quede autenticado de inmediato.
     * Los administradores no pueden auto-registrarse.
     */
    @Transactional
    public AuthResponse registrar(RegistroRequest request) {
        if (request.rol() == Rol.ADMINISTRADOR) {
            LOG.warn("Registro rechazado: intento de auto-registro como administrador email={}",
                    enmascararEmail(request.email()));
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Solo es posible registrarse como cliente o vendedor", "rol");
        }
        if (!request.password().equals(request.confirmarPassword())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Las contraseñas no coinciden", "confirmarPassword");
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

    /**
     * Verifica credenciales con el AuthenticationManager (BCrypt + estado de la cuenta) y emite el JWT.
     * Los fallos de contraseña cuentan para el límite de intentos por correo + IP.
     */
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
            LOG.warn("Login rechazado: cuenta suspendida email={} ip={}", enmascararEmail(email), ip);
            throw ex;
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
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

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
            if (!envioPasswordActual
                    || !passwordEncoder.matches(request.passwordActual(), usuario.getPassword())) {
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
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        usuario.setEstado(EstadoUsuario.SUSPENDIDO);
        usuarioRepository.save(usuario);
    }

    private AuthResponse construirRespuesta(Usuario usuario) {
        JwtService.TokenEmitido token = jwtService.generarToken(usuario);
        return AuthResponse.bearer(token.token(), token.expiraEn(), UsuarioResponse.from(usuario));
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
