package co.edu.uniquindio.littlestyle.config.security;

import co.edu.uniquindio.littlestyle.modules.auth.service.EstadoCuentaService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lee el header {@code Authorization: Bearer <token>}, valida el JWT y, si es correcto y la cuenta
 * sigue activa, autentica la petición. Si el token falta, es inválido o pertenece a una cuenta
 * suspendida, la petición sigue sin autenticar y Spring Security responde 401 en los endpoints protegidos.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String BEARER_PREFIX = "Bearer ";
    /** Atributo de request con la causa del fallo, leído por {@link RestAuthenticationEntryPoint}. */
    public static final String JWT_ERROR_ATTRIBUTE = "jwt_error";

    static final String CUENTA_NO_ACTIVA = "La cuenta no está activa";

    private final JwtService jwtService;
    private final EstadoCuentaService estadoCuentaService;

    public JwtAuthenticationFilter(JwtService jwtService, EstadoCuentaService estadoCuentaService) {
        this.jwtService = jwtService;
        this.estadoCuentaService = estadoCuentaService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            Claims claims = jwtService.validarToken(token);
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                AuthenticatedUser user = jwtService.toAuthenticatedUser(claims);
                // La firma no basta: una cuenta suspendida no debe seguir operando con tokens ya emitidos
                if (estadoCuentaService.estaActiva(user.id())) {
                    UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                            user, null, List.of(new SimpleGrantedAuthority(user.rol().authority())));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    request.setAttribute(JWT_ERROR_ATTRIBUTE, CUENTA_NO_ACTIVA);
                }
            }
        } catch (ExpiredJwtException ex) {
            request.setAttribute(JWT_ERROR_ATTRIBUTE, "La sesión ha expirado, inicie sesión nuevamente");
        } catch (JwtException | IllegalArgumentException ex) {
            request.setAttribute(JWT_ERROR_ATTRIBUTE, "Token de autenticación inválido");
        }

        chain.doFilter(request, response);
    }
}
