package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import jakarta.validation.constraints.NotNull;

/** Nuevo estado de publicación de una prenda. */
public record EstadoPrendaRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoPrenda estado
) {
}
