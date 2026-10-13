package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CrearPedidoRequest(
        @NotNull(message = "El perfil infantil es obligatorio")
        @Positive(message = "El identificador del perfil debe ser positivo")
        Long perfilInfantilId,
        @NotNull(message = "La dirección de envío es obligatoria")
        @Valid DireccionEnvioRequest direccion
) {
}
