package co.edu.uniquindio.littlestyle.modules.auth.dto;

import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;

/** Datos públicos de un usuario (sin contraseña ni estado). */
public record UsuarioResponse(
        Long id,
        String nombre,
        String apellido,
        String email,
        String telefono,
        String nombreTienda,
        Rol rol
) {
    /** Construye la respuesta a partir de la entidad. */
    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getNombreTienda(),
                usuario.getRol()
        );
    }
}
