package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Id de la transacción de Wompi que devolvió el Widget. */
public record VerificarPagoRequest(
        @NotBlank(message = "El identificador de la transacción es obligatorio")
        @Size(max = 80, message = "El identificador de la transacción no es válido")
        String transaccionId
) {
}
