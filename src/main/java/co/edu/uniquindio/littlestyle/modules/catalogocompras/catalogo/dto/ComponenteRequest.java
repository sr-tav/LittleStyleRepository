package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.MaterialTextil;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ComponenteRequest(
        @NotNull(message = "El material es obligatorio")
        MaterialTextil material,

        @NotNull(message = "El porcentaje es obligatorio")
        @Min(value = 1, message = "El porcentaje debe ser mínimo 1")
        @Max(value = 100, message = "El porcentaje debe ser máximo 100")
        Integer porcentaje
) {
}
