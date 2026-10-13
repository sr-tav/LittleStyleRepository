package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DireccionEnvioRequest(
        @NotBlank(message = "El nombre del destinatario es obligatorio")
        @Size(max = 120, message = "El destinatario no puede superar 120 caracteres")
        String destinatario,
        @NotBlank(message = "La dirección de entrega es obligatoria")
        @Size(max = 200, message = "La dirección no puede superar 200 caracteres")
        String direccion,
        @Size(max = 120, message = "El complemento no puede superar 120 caracteres")
        String complemento,
        @NotBlank(message = "El código postal es obligatorio")
        @Pattern(regexp = "^\\d{6}$", message = "El código postal debe tener 6 dígitos")
        String codigoPostal,
        @NotBlank(message = "El departamento es obligatorio")
        @Size(max = 80, message = "El departamento no puede superar 80 caracteres")
        String departamento,
        @NotBlank(message = "El municipio es obligatorio")
        @Size(max = 80, message = "El municipio no puede superar 80 caracteres")
        String municipio,
        @NotBlank(message = "El celular de contacto es obligatorio")
        @Pattern(regexp = "^3\\d{9}$", message = "El celular debe tener 10 dígitos e iniciar por 3")
        String telefono
) {
}
