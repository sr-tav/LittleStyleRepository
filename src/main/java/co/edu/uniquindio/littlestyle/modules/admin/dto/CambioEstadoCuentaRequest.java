package co.edu.uniquindio.littlestyle.modules.admin.dto;

import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo del cambio de estado de cuenta decidido por el administrador (US-14): {@code estado} indica el
 * destino ({@code ACTIVO}, {@code SUSPENDIDO} o {@code BLOQUEADO}) y {@code motivo} queda trazado en
 * auditoria y en el asunto de la apelacion automatica.
 */
public record CambioEstadoCuentaRequest(
        @NotNull(message = "El nuevo estado es obligatorio")
        EstadoUsuario estado,
        @NotBlank(message = "El motivo es obligatorio")
        @Size(min = 10, max = 500, message = "El motivo debe tener entre 10 y 500 caracteres")
        String motivo) {
}
