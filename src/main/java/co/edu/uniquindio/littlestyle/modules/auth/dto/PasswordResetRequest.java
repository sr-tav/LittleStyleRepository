package co.edu.uniquindio.littlestyle.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Solicitud para iniciar la recuperación de la contraseña. */
public record PasswordResetRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        String email
) {
}


