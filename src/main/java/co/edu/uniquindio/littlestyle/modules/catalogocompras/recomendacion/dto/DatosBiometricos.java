package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Medidas del niño para elegir talla: estatura, peso y fecha de la medición, más la holgura y la contextura
 * preferidas.
 */
public record DatosBiometricos(BigDecimal estaturaCm, BigDecimal pesoKg, LocalDate fechaMedicion, Holgura holgura, Contextura contextura) {
    /** Exige estatura, peso y fecha de medición. */
    public DatosBiometricos {
        if (estaturaCm == null || pesoKg == null || fechaMedicion == null) {
            throw new IllegalArgumentException("Estatura, peso y fecha son obligatorios");
        }
    }
}
