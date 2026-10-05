package co.edu.uniquindio.littlestyle.config.security;

import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;

/**
 * Principal que queda en el SecurityContext tras validar el JWT.
 * Se construye con los claims del token; de la base de datos solo se consulta el estado de la cuenta
 * (ver {@link JwtAuthenticationFilter}).
 */
public record AuthenticatedUser(Long id, String email, String nombre, Rol rol) {
}
