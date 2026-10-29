package co.edu.uniquindio.littlestyle.config.security;

import org.springframework.security.access.AccessDeniedException;

/**
 * Resuelve el alcance de los datos analíticos a partir del usuario autenticado.
 * Un administrador ve toda la plataforma ({@code null}); un vendedor ve solo su tienda.
 */
public final class AlcanceDatos {

    private AlcanceDatos() {
    }

    /**
     * @return {@code null} para alcance global, o el id del vendedor cuando debe filtrarse por tienda.
     * @throws AccessDeniedException si el usuario es anónimo o es cliente.
     */
    public static Long vendedorId(AuthenticatedUser usuario) {
        if (usuario == null || usuario.rol() == null) {
            throw new AccessDeniedException("Usuario no autenticado");
        }
        return switch (usuario.rol()) {
            case VENDEDOR -> usuario.id();
            case ADMINISTRADOR -> null;
            case CLIENTE -> throw new AccessDeniedException("No tienes acceso a la información analítica");
        };
    }
}
