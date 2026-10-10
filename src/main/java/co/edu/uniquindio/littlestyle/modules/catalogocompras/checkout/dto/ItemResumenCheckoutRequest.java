package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemResumenCheckoutRequest(
        @NotNull(message = "La prenda es obligatoria")
        @Positive(message = "El identificador de la prenda debe ser positivo")
        Long prendaId,
        @NotBlank(message = "La talla es obligatoria")
        String talla,
        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor que cero")
        Integer cantidad
) {
}
