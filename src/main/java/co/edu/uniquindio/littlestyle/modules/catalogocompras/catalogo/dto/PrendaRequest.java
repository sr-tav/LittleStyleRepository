package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.GeneroPrenda;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Estampado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Datos de la prenda para crearla o editarla. El {@code stock} de cada talla solo se toma para tallas
 * nuevas; el stock de las tallas existentes se modifica desde el inventario. Las fotos viajan aparte
 * (parte multipart {@code archivos}) y son obligatorias al crear.
 */
public record PrendaRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 3, max = 120, message = "El nombre debe tener entre 3 y 120 caracteres")
        @Pattern(regexp = "^(?=.*\\p{L}).+$", message = "El nombre debe contener al menos una letra")
        String nombre,

        @NotNull(message = "La categoría es obligatoria")
        CategoriaPrenda categoria,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 2000, message = "La descripción admite máximo 2000 caracteres")
        String descripcion,

        /** Etiquetado obligatorio según la Resolución Andina 2109: lavado, secado, planchado, etc. */
        @NotBlank(message = "Indica las instrucciones de cuidado textil")
        @Size(max = 1000, message = "Las instrucciones de cuidado admiten máximo 1000 caracteres")
        String instruccionesCuidado,

        @Size(max = 80, message = "La marca admite máximo 80 caracteres")
        String marca,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "100", message = "El precio debe ser mínimo $100")
        @DecimalMax(value = "10000000", message = "El precio debe ser máximo $10.000.000")
        @Digits(integer = 10, fraction = 2, message = "El precio admite máximo dos decimales")
        BigDecimal precio,

        /** Colores que ofrece la prenda; cada uno debe tener al menos una variante en las tallas. */
        @NotNull(message = "Selecciona al menos un color")
        @Size(min = 1, max = 10, message = "Selecciona entre 1 y 10 colores")
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
        @Size(max = 60, message = "La tabla admite máximo 60 combinaciones de talla y color")
        List<@Valid @NotNull TallaRequest> tallas
) {
}