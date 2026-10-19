package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Perfil infantil y dirección con los que se calcula el resumen de compra. */
public record ResumenCheckoutRequest(
        @NotNull(message = "El perfil infantil es obligatorio")
        @Positive(message = "El identificador del perfil debe ser positivo")
        Long perfilInfantilId,
        @NotNull(message = "La dirección de entrega es obligatoria para calcular el envío")
        @Valid DireccionEnvioRequest direccion
) {
}
