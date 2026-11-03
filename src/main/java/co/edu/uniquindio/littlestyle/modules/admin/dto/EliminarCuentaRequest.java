package co.edu.uniquindio.littlestyle.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Motivo de la eliminación de una cuenta por administrador */
public record EliminarCuentaRequest(
    @NotBlank(message = "El motivo es obligatorio")
    @Size(min= 10, max = 500, message = "El motivo debe tener entre 10 y 500 caracteres")
    String motivo) {
}
