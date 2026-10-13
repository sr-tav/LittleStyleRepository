package co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AgregarItemCarritoRequest(
        @NotNull(message = "La prenda es obligatoria")
        @Positive(message = "El identificador de la prenda debe ser positivo")
        Long prendaId,
        @NotBlank(message = "La talla es obligatoria")
        @Size(max = 10, message = "La talla no puede superar 10 caracteres")
        String talla,
        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor que cero")
        Integer cantidad
) {
}
