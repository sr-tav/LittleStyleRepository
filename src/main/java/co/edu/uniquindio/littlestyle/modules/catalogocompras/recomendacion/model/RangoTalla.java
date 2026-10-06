package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model;

import java.math.BigDecimal;

public record RangoTalla(String talla, BigDecimal estaturaMinCm, BigDecimal estaturaMaxCm, BigDecimal pesoMinKg, BigDecimal pesoMaxKg) {
    public RangoTalla {
        if (talla == null || talla.isBlank()) {
            throw new IllegalArgumentException("La talla es obligatoria");
        }
        if (estaturaMinCm == null || estaturaMaxCm == null || pesoMinKg == null || pesoMaxKg == null) {
            throw new IllegalArgumentException("Los rangos de estatura y peso son obligatorios");
        }
        if (estaturaMinCm.compareTo(estaturaMaxCm) > 0 || pesoMinKg.compareTo(pesoMaxKg) > 0) {
            throw new IllegalArgumentException("El mínimo no puede superar al máximo en la talla  " + talla );
        }
    }

    public boolean contieneEstatura(BigDecimal estaturaCm) {
        return estaturaCm.compareTo(estaturaMinCm) >= 0 && estaturaCm.compareTo(estaturaMaxCm) <= 0;
    }

    public boolean contienePeso(BigDecimal pesoKg) {
        return pesoKg.compareTo(pesoMinKg) >= 0 && pesoKg.compareTo(pesoMaxKg) <= 0;
    }
}
