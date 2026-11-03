package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** Fila de la tabla de tallas. Los límites de medidas coinciden con los del perfil infantil (US-06). */
public record TallaRequest(
        @NotBlank(message = "La talla es obligatoria")
        @Size(max = 10, message = "La talla admite máximo 10 caracteres")
        @Pattern(regexp = "^[\\p{L}0-9 ./-]+$", message = "La talla solo admite letras, números, espacios, '.', '/' y '-'")
        String talla,

        @NotNull(message = "La estatura mínima es obligatoria")
        @DecimalMin(value = "40.0", message = "La estatura debe ser mínimo 40 cm")
        @DecimalMax(value = "200.0", message = "La estatura debe ser máximo 200 cm")
        BigDecimal estaturaMinCm,

        @NotNull(message = "La estatura máxima es obligatoria")
        @DecimalMin(value = "40.0", message = "La estatura debe ser mínimo 40 cm")
        @DecimalMax(value = "200.0", message = "La estatura debe ser máximo 200 cm")
        BigDecimal estaturaMaxCm,

        @NotNull(message = "El peso mínimo es obligatorio")
        @DecimalMin(value = "2.0", message = "El peso debe ser mínimo 2 kg")
        @DecimalMax(value = "120.0", message = "El peso debe ser máximo 120 kg")
        BigDecimal pesoMinKg,

        @NotNull(message = "El peso máximo es obligatorio")
        @DecimalMin(value = "2.0", message = "El peso debe ser mínimo 2 kg")
        @DecimalMax(value = "120.0", message = "El peso debe ser máximo 120 kg")
        BigDecimal pesoMaxKg,

        @Min(value = 0, message = "El stock no puede ser negativo")
        @Max(value = 100000, message = "El stock debe ser máximo 100000")
        Integer stock,

        /** Edad mínima en meses que cubre la talla; nula si no se declara. */
        @Min(value = 0, message = "La edad mínima no puede ser negativa")
        @Max(value = 216, message = "La edad máxima es 216 meses (18 años)")
        Integer edadMinMeses,

        /** Edad máxima en meses que cubre la talla; nula si no se declara. */
        @Min(value = 0, message = "La edad mínima no puede ser negativa")
        @Max(value = 216, message = "La edad máxima es 216 meses (18 años)")
        Integer edadMaxMeses,

        /** Mínimo de esta talla para alertas; nulo si la talla no alerta. */
        @Min(value = 0, message = "El stock mínimo no puede ser negativo")
        @Max(value = 10000, message = "El stock mínimo debe ser máximo 10000")
        Integer stockMinimo,

        /** Color de la variante. Opcional si la prenda tiene un solo color
         */
        ColorPreferido color
) {
        /** Variante sin color explícito: toma el único color de la prenda. */
        public TallaRequest(String talla, BigDecimal estaturaMinCm, BigDecimal estaturaMaxCm, BigDecimal pesoMinKg,
                            BigDecimal pesoMaxKg, Integer stock, Integer edadMinMeses, Integer edadMaxMeses,
                            Integer stockMinimo) {
                this(talla, estaturaMinCm, estaturaMaxCm, pesoMinKg, pesoMaxKg, stock, edadMinMeses, edadMaxMeses,
                        stockMinimo, null);
        }
}
