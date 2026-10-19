package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model;

import java.math.BigDecimal;

/** Rangos de estatura y peso que cubre una talla. */
public record RangoTalla(String talla, BigDecimal estaturaMinCm, BigDecimal estaturaMaxCm, BigDecimal pesoMinKg, BigDecimal pesoMaxKg) {
    /** Exige el nombre de la talla y rangos completos con el mínimo menor o igual al máximo. */
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

    /** Indica si la estatura está dentro del rango (límites incluidos). */
    public boolean contieneEstatura(BigDecimal estaturaCm) {
        return estaturaCm.compareTo(estaturaMinCm) >= 0 && estaturaCm.compareTo(estaturaMaxCm) <= 0;
    }

    /** Indica si el peso está dentro del rango (límites incluidos). */
    public boolean contienePeso(BigDecimal pesoKg) {
        return pesoKg.compareTo(pesoMinKg) >= 0 && pesoKg.compareTo(pesoMaxKg) <= 0;
    }
}
