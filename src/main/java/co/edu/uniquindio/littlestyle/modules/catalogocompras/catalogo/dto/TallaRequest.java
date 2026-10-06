package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
        Integer stock
) {
}
