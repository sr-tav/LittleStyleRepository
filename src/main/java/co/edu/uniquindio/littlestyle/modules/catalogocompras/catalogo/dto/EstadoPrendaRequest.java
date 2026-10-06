package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import jakarta.validation.constraints.NotNull;

public record EstadoPrendaRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoPrenda estado
) {
}
