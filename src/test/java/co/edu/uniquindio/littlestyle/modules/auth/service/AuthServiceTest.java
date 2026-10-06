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
import co.edu.uniquindio.littlestyle.shared.exception.LoginBloqueadoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private IntentosLoginService intentosLoginService;

    @InjectMocks
    private AuthService authService;

    private static RegistroRequest registro(Rol rol, String password, String confirmar, String tienda) {
        return new RegistroRequest("María", "González", "  Maria@Correo.COM ", "3001234567",
                password, confirmar, rol, tienda, true);
    }

    private static Usuario usuario(Long id, String email, Rol rol, EstadoUsuario estado) {
        return Usuario.builder()
                .id(id).nombre("María").apellido("González").email(email)
                .telefono("3001234567").password("hash").rol(rol).estado(estado)
                .build();
    }

    @Nested
    @DisplayName("registrar")
    class Registrar {

        @Test
        @DisplayName("registra un cliente, cifra la contraseña, normaliza el correo y devuelve un token")
        void registraCliente() {
            when(usuarioRepository.existsByEmail("maria@correo.com")).thenReturn(false);
            when(passwordEncoder.encode("Clave1234")).thenReturn("$2a$hash");
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
                Usuario u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });
            when(jwtService.generarToken(any(Usuario.class)))
                    .thenReturn(new JwtService.TokenEmitido("jwt-token", 123L));

            AuthResponse response = authService.registrar(registro(Rol.CLIENTE, "Clave1234", "Clave1234", "Ignorada"));

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            Usuario guardado = captor.getValue();
            assertThat(guardado.getEmail()).isEqualTo("maria@correo.com");
            assertThat(guardado.getPassword()).isEqualTo("$2a$hash");
            assertThat(guardado.getRol()).isEqualTo(Rol.CLIENTE);
            assertThat(guardado.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
            assertThat(guardado.getNombreTienda()).as("los clientes no tienen tienda").isNull();
            assertThat(guardado.getVersionTerminos()).isEqualTo(AuthService.VERSION_TERMINOS_VIGENTE);
            assertThat(guardado.getFechaAceptacionTerminos()).isNotNull();

            assertThat(response.token()).isEqualTo("jwt-token");
            assertThat(response.tipo()).isEqualTo("Bearer");
            assertThat(response.expiraEn()).isEqualTo(123L);
            assertThat(response.usuario().id()).isEqualTo(1L);
            assertThat(response.usuario().rol()).isEqualTo(Rol.CLIENTE);
        }

        @Test
        @DisplayName("registra un vendedor guardando el nombre de la tienda")
        void registraVendedor() {
            when(usuarioRepository.existsByEmail("maria@correo.com")).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("$2a$hash");
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jwtService.generarToken(any(Usuario.class)))
                    .thenReturn(new JwtService.TokenEmitido("jwt-token", 123L));

            AuthResponse response = authService.registrar(
                    registro(Rol.VENDEDOR, "Clave1234", "Clave1234", "  Mundo Peque  "));

            assertThat(response.usuario().rol()).isEqualTo(Rol.VENDEDOR);
            assertThat(response.usuario().nombreTienda()).isEqualTo("Mundo Peque");
        }

        @Test
        @DisplayName("rechaza el auto-registro como administrador")
        void rechazaAdministrador() {
            assertThatThrownBy(() -> authService.registrar(
                    registro(Rol.ADMINISTRADOR, "Clave1234", "Clave1234", null)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(be.getCampo()).isEqualTo("rol");
                    });
            verifyNoInteractions(usuarioRepository, passwordEncoder, jwtService);
        }

        @Test
        @DisplayName("rechaza contraseñas que no coinciden")
        void rechazaPasswordsDistintas() {
            assertThatThrownBy(() -> authService.registrar(
                    registro(Rol.CLIENTE, "Clave1234", "Otra1234", null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Las contraseñas no coinciden")
                    .extracting("campo").isEqualTo("confirmarPassword");
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("exige nombre de tienda a los vendedores")
        void vendedorSinTienda() {
            assertThatThrownBy(() -> authService.registrar(
                    registro(Rol.VENDEDOR, "Clave1234", "Clave1234", "   ")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("campo").isEqualTo("nombreTienda");
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("rechaza un correo ya registrado con 409")
        void emailDuplicado() {
            when(usuarioRepository.existsByEmail("maria@correo.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.registrar(
                    registro(Rol.CLIENTE, "Clave1234", "Clave1234", null)))
                    .isInstanceOf(EmailYaRegistradoException.class)
                    .extracting("status").isEqualTo(HttpStatus.CONFLICT);
            verify(usuarioRepository, never()).save(any());
            verifyNoInteractions(passwordEncoder);
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        private static final String IP = "10.0.0.1";

        @Test
        @DisplayName("autentica con el AuthenticationManager y devuelve el token")
        void loginExitoso() {
            Usuario u = usuario(5L, "maria@correo.com", Rol.VENDEDOR, EstadoUsuario.ACTIVO);
            when(authenticationManager.authenticate(any(Authentication.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(usuarioRepository.findByEmail("maria@correo.com")).thenReturn(Optional.of(u));
            when(jwtService.generarToken(u)).thenReturn(new JwtService.TokenEmitido("jwt", 999L));

            AuthResponse response = authService.login(new LoginRequest(" MARIA@correo.com", "Clave1234"), IP);

            ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
            verify(authenticationManager).authenticate(captor.capture());
            assertThat(captor.getValue()).isInstanceOf(UsernamePasswordAuthenticationToken.class);
            assertThat(captor.getValue().getName()).isEqualTo("maria@correo.com");
            assertThat(captor.getValue().getCredentials()).isEqualTo("Clave1234");

            assertThat(response.token()).isEqualTo("jwt");
            assertThat(response.usuario().rol()).isEqualTo(Rol.VENDEDOR);
            verify(intentosLoginService).registrarExito("maria@correo.com", IP);
        }

        @Test
        @DisplayName("propaga BadCredentialsException con credenciales incorrectas y no emite token")
        void credencialesIncorrectas() {
            when(authenticationManager.authenticate(any(Authentication.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(new LoginRequest("maria@correo.com", "mala"), IP))
                    .isInstanceOf(BadCredentialsException.class);
            verify(intentosLoginService).registrarFallo("maria@correo.com", IP);
            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("propaga DisabledException si la cuenta está suspendida")
        void cuentaSuspendida() {
            when(authenticationManager.authenticate(any(Authentication.class)))
                    .thenThrow(new DisabledException("disabled"));

            assertThatThrownBy(() -> authService.login(new LoginRequest("maria@correo.com", "Clave1234"), IP))
                    .isInstanceOf(DisabledException.class);
            verify(intentosLoginService, never()).registrarFallo(any(), any());
            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("rechaza con 429 sin verificar credenciales si el correo + IP está bloqueado")
        void bloqueadoPorIntentos() {
            when(intentosLoginService.bloqueoRestante("maria@correo.com", IP))
                    .thenReturn(Optional.of(Duration.ofMinutes(10)));

            assertThatThrownBy(() -> authService.login(new LoginRequest("Maria@correo.com", "Clave1234"), IP))
                    .isInstanceOf(LoginBloqueadoException.class)
                    .extracting("status").isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            verifyNoInteractions(authenticationManager, jwtService);
        }
    }

    @Nested
    @DisplayName("obtenerPerfil")
    class ObtenerPerfil {

        @Test
        void devuelvePerfil() {
            Usuario u = usuario(7L, "maria@correo.com", Rol.CLIENTE, EstadoUsuario.ACTIVO);
            when(usuarioRepository.findByEmail("maria@correo.com")).thenReturn(Optional.of(u));

            UsuarioResponse perfil = authService.obtenerPerfil("maria@correo.com");

            assertThat(perfil.id()).isEqualTo(7L);
            assertThat(perfil.email()).isEqualTo("maria@correo.com");
        }

        @Test
        void usuarioInexistente() {
            when(usuarioRepository.findByEmail("x@correo.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.obtenerPerfil("x@correo.com"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
