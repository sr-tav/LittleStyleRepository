package co.edu.uniquindio.littlestyle.config.security;

import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.service.EstadoCuentaService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias del filtro de autenticación JWT con mocks: cubre la
 * creación del SecurityContext, la verificación de cuenta activa y los
 * errores de token (ausente, inválido o expirado) que se reportan
 * al frontend vía atributo de la request.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private EstadoCuentaService estadoCuentaService;
    @Mock
    private FilterChain chain;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    // Prepara el filtro con los mocks y deja el SecurityContext limpio
    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, estadoCuentaService);
        request = new MockHttpServletRequest("GET", "/api/auth/me");
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    // Evita que la autenticación simulada se filtre a otros tests
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // Sin header Authorization la cadena sigue normal y nada se autentica
    @Test
    void sinHeaderContinuaSinAutenticar() throws Exception {
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtService, estadoCuentaService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // Solo se acepta el esquema Bearer: cualquier otro header se ignora
    @Test
    void headerSinBearerSeIgnora() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    // Token válido y cuenta activa: se guarda el usuario con su rol del token
    @Test
    void tokenValidoAutenticaConElRolDelToken() throws Exception {
        Claims claims = mock(Claims.class);
        AuthenticatedUser user = new AuthenticatedUser(3L, "ana@correo.com", "Ana", Rol.CLIENTE);
        request.addHeader("Authorization", "Bearer token-valido");
        when(jwtService.validarToken("token-valido")).thenReturn(claims);
        when(jwtService.toAuthenticatedUser(claims)).thenReturn(user);
        when(estadoCuentaService.estaActiva(3L)).thenReturn(true);

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getPrincipal()).isEqualTo(user);
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_CLIENTE");
        verify(chain).doFilter(request, response);
    }

    // El token puede ser válido, pero si la cuenta ya no está activa no se
    // autentica y se marca la causa por si el frontend necesita mostrarla
    @Test
    void tokenValidoDeCuentaSuspendidaNoAutentica() throws Exception {
        Claims claims = mock(Claims.class);
        AuthenticatedUser user = new AuthenticatedUser(3L, "ana@correo.com", "Ana", Rol.CLIENTE);
        request.addHeader("Authorization", "Bearer token-valido");
        when(jwtService.validarToken("token-valido")).thenReturn(claims);
        when(jwtService.toAuthenticatedUser(claims)).thenReturn(user);
        when(estadoCuentaService.estaActiva(3L)).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.JWT_ERROR_ATTRIBUTE))
                .isEqualTo(JwtAuthenticationFilter.CUENTA_NO_ACTIVA);
        verify(chain).doFilter(request, response);
    }

    // Un token expirado bloquea la autenticación pero dejo pasar la cadena,
    // con el motivo en un atributo para que sepa cuál fue el problema
    @Test
    void tokenExpiradoNoAutenticaYMarcaLaCausa() throws Exception {
        request.addHeader("Authorization", "Bearer token-expirado");
        when(jwtService.validarToken("token-expirado"))
                .thenThrow(new ExpiredJwtException(null, null, "expired"));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.JWT_ERROR_ATTRIBUTE).toString())
                .contains("expirado");
        verify(chain).doFilter(request, response);
    }

    // Token mal formado o inyectado: tampoco se autentica y se marca el error
    @Test
    void tokenInvalidoNoAutenticaYMarcaLaCausa() throws Exception {
        request.addHeader("Authorization", "Bearer basura");
        when(jwtService.validarToken("basura")).thenThrow(new MalformedJwtException("malformed"));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.JWT_ERROR_ATTRIBUTE))
                .isEqualTo("Token de autenticación inválido");
        verify(chain).doFilter(request, response);
    }
}
