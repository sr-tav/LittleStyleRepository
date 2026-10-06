package co.edu.uniquindio.littlestyle.shared.util;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Acceso al usuario autenticado desde servicios de cualquier módulo.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<AuthenticatedUser> usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    /** Identificación del usuario autenticado para logs de seguridad, sin datos personales. */
    public static String describirUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            return "usuario=anónimo";
        }
        if (auth.getPrincipal() instanceof AuthenticatedUser user) {
            return "usuarioId=" + user.id() + " rol=" + user.rol();
        }
        return "autoridades=" + auth.getAuthorities();
    }
}
