package co.edu.uniquindio.littlestyle.modules.auth.service;

import co.edu.uniquindio.littlestyle.config.security.JwtService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Registra un cliente o vendedor y devuelve un token para que quede autenticado de inmediato.
     * Los administradores no pueden auto-registrarse.
     */
    @Transactional
    public AuthResponse registrar(RegistroRequest request) {
        if (request.rol() == Rol.ADMINISTRADOR) {
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
                .build();

        return construirRespuesta(usuarioRepository.save(usuario));
    }

    /**
     * Verifica credenciales con el AuthenticationManager (BCrypt + estado de la cuenta) y emite el JWT.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizarEmail(request.email());
        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        return construirRespuesta(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPerfil(String email) {
        return usuarioRepository.findByEmail(email)
                .map(UsuarioResponse::from)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private AuthResponse construirRespuesta(Usuario usuario) {
        JwtService.TokenEmitido token = jwtService.generarToken(usuario);
        return AuthResponse.bearer(token.token(), token.expiraEn(), UsuarioResponse.from(usuario));
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
