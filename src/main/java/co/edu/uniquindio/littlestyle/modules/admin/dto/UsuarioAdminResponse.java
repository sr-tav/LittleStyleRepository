package co.edu.uniquindio.littlestyle.modules.admin.dto;

import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;

import java.time.LocalDateTime;

public record UsuarioAdminResponse(
        Long id,
        String nombre,
        String apellido,
        String email,
        String telefono,
        String nombreTienda,
        Rol rol,
        EstadoUsuario estado,
        LocalDateTime fechaRegistro) {

    /** Copia del usuario con solo los campos que el panel necesita. */
    public static UsuarioAdminResponse from(Usuario usuario) {
        return new UsuarioAdminResponse(
                usuario.getId(), usuario.getNombre(), usuario.getApellido(), usuario.getEmail(),
                usuario.getTelefono(), usuario.getNombreTienda(), usuario.getRol(),
                usuario.getEstado(), usuario.getFechaRegistro());
    }
}
