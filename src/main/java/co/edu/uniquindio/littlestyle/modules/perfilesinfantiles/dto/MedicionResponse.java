package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.dto;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.MedicionCrecimiento;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MedicionResponse(
        Long id,
        LocalDate fechaMedicion,
        BigDecimal estaturaCm,
        BigDecimal pesoKg
) {
    public static MedicionResponse from(MedicionCrecimiento medicion) {
        return new MedicionResponse(medicion.getId(), medicion.getFechaMedicion(),
                medicion.getEstaturaCm(), medicion.getPesoKg());
    }
}
