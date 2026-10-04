package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MedicionRequest(
        @NotNull(message = "La fecha de medición es obligatoria")
        @PastOrPresent(message = "La fecha de medición no puede ser futura")
        LocalDate fechaMedicion,

        @NotNull(message = "La estatura es obligatoria")
        @DecimalMin(value = "40.0", message = "La estatura debe ser mínimo 40 cm")
        @DecimalMax(value = "200.0", message = "La estatura debe ser máximo 200 cm")
        BigDecimal estaturaCm,

        @NotNull(message = "El peso es obligatorio")
        @DecimalMin(value = "2.0", message = "El peso debe ser mínimo 2 kg")
        @DecimalMax(value = "120.0", message = "El peso debe ser máximo 120 kg")
        BigDecimal pesoKg
) {
}
