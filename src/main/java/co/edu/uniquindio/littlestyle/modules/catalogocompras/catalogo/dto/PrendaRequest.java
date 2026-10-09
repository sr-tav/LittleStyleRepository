package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.GeneroPrenda;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Estampado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Datos de la prenda para crearla o editarla. El {@code stock} de cada talla solo se toma para tallas
 * nuevas; el stock de las tallas existentes se modifica desde el inventario.
 */
public record PrendaRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 3, max = 120, message = "El nombre debe tener entre 3 y 120 caracteres")
        String nombre,

        @NotNull(message = "La categoría es obligatoria")
        CategoriaPrenda categoria,

        @Size(max = 2000, message = "La descripción admite máximo 2000 caracteres")
        String descripcion,

        @Size(max = 80, message = "La marca admite máximo 80 caracteres")
        String marca,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "100", message = "El precio debe ser mínimo $100")
        @DecimalMax(value = "10000000", message = "El precio debe ser máximo $10.000.000")
        @Digits(integer = 10, fraction = 2, message = "El precio admite máximo dos decimales")
        BigDecimal precio,

        @NotNull(message = "El stock mínimo es obligatorio")
        @Min(value = 0, message = "El stock mínimo no puede ser negativo")
        @Max(value = 10000, message = "El stock mínimo debe ser máximo 10000")
        Integer stockMinimo,

        @Size(max = 10, message = "Selecciona máximo 10 colores")
        Set<ColorPreferido> colores,

        Estampado estampado,

        /** Opcional; sin valor se trata como unisex. */
        GeneroPrenda genero,

        Boolean tintesSinteticos,
        Boolean brochesMetalicos,
        Boolean contieneNiquel,
        Boolean tratamientoFormaldehido,

        @NotEmpty(message = "Indica la composición textil")
        @Size(max = 6, message = "La composición admite máximo 6 materiales")
        List<@Valid @NotNull ComponenteRequest> composicion,

        @NotEmpty(message = "Agrega al menos una talla")
        @Size(max = 15, message = "La tabla de tallas admite máximo 15 tallas")
        List<@Valid @NotNull TallaRequest> tallas
) {
}
