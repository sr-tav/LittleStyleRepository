package co.edu.uniquindio.littlestyle.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActualizarCuentaRequest(
        @Size(min = 2, max = 60, message = "El nombre debe tener entre 2 y 60 caracteres")
        @Pattern(regexp = "^[\\p{L} ]+$", message = "El nombre solo puede contener letras")
        String nombre,

        @Size(min = 2, max = 60, message = "El apellido debe tener entre 2 y 60 caracteres")
        @Pattern(regexp = "^[\\p{L} ]+$", message = "El apellido solo puede contener letras")
        String apellido,

        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 120, message = "El correo no puede superar 120 caracteres")
        String email,

        @Pattern(regexp = "^$|^3\\d{9}$",
                message = "El teléfono debe ser un celular colombiano de 10 dígitos")
        String telefono,

        @Size(max = 100, message = "El nombre de la tienda no puede superar 100 caracteres")
        String nombreTienda,

        String passwordActual,

        @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "La contraseña debe contener al menos una letra y un número")
        String nuevaPassword,

        String confirmarNuevaPassword
) {
}
