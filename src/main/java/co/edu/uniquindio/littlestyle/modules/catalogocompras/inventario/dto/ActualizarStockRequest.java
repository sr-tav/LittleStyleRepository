package co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Fija las existencias de las tallas indicadas (valor absoluto, no incremento). Las tallas que no se
 * envían conservan su stock. Si {@code stockMinimo} es nulo, el mínimo de la prenda no cambia.
 */
public record ActualizarStockRequest(
        @NotEmpty(message = "Indica el stock de al menos una talla")
        @Size(max = 15, message = "Se admiten máximo 15 tallas")
        List<@Valid @NotNull StockTalla> tallas,

        @Min(value = 0, message = "El stock mínimo no puede ser negativo")
        @Max(value = 10000, message = "El stock mínimo debe ser máximo 10000")
        Integer stockMinimo
) {

    public record StockTalla(
            @NotBlank(message = "La talla es obligatoria")
            String talla,

            @NotNull(message = "El stock es obligatorio")
            @Min(value = 0, message = "El stock no puede ser negativo")
            @Max(value = 100000, message = "El stock debe ser máximo 100000")
            Integer stock
    ) {
    }
}
