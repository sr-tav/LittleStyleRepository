package co.edu.uniquindio.littlestyle.config.security;

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
 * Lee el header {@code Authorization: Bearer <token>}, valida el JWT y, si es correcto,
 * autentica la petición. Si el token falta o es inválido la petición sigue sin autenticar
 * y Spring Security responde 401 en los endpoints protegidos.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String BEARER_PREFIX = "Bearer ";
    /** Atributo de request con la causa del fallo, leído por {@link RestAuthenticationEntryPoint}. */
    public static final String JWT_ERROR_ATTRIBUTE = "jwt_error";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
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
                UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                        user, null, List.of(new SimpleGrantedAuthority(user.rol().authority())));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (ExpiredJwtException ex) {
            request.setAttribute(JWT_ERROR_ATTRIBUTE, "La sesión ha expirado, inicie sesión nuevamente");
        } catch (JwtException | IllegalArgumentException ex) {
            request.setAttribute(JWT_ERROR_ATTRIBUTE, "Token de autenticación inválido");
        }

        chain.doFilter(request, response);
    }
}
