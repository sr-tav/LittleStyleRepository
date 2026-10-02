package co.edu.uniquindio.littlestyle.config.security;

import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;

/**
 * Principal que queda en el SecurityContext tras validar el JWT.
 * Se construye solo con los claims del token, sin consultar la base de datos (ADR-04).
 */
public record AuthenticatedUser(Long id, String email, String nombre, Rol rol) {
}
