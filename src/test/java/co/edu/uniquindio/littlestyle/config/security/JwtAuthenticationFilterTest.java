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

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, estadoCuentaService);
        request = new MockHttpServletRequest("GET", "/api/auth/me");
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sinHeaderContinuaSinAutenticar() throws Exception {
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtService, estadoCuentaService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void headerSinBearerSeIgnora() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

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
                .isEqualTo(JwtAuthenticationFilter.CUENTA_SUSPENDIDA);
        verify(chain).doFilter(request, response);
    }

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
