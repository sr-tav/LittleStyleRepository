package co.edu.uniquindio.littlestyle.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Solicitud para fijar una nueva contraseña tras recibir el enlace de recuperación. */
public record RestablecerPasswordRequest(
        @NotBlank(message = "El token es obligatorio")
        String token,

        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres")
        String password,

        @NotBlank(message = "Debe confirmar la contraseña")
        String confirmarPassword
) {
}

