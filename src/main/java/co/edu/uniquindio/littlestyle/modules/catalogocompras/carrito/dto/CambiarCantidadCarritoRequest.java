package co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Nueva cantidad absoluta de una línea del carrito. */
public record CambiarCantidadCarritoRequest(
        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor que cero")
        Integer cantidad
) {
}
