package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto.DatosBiometricos;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.RangoTalla;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Distancias biométricas normalizadas (US-07, RNF-11).
 * Mezclar centímetros y kilogramos en una euclídea cruda subestima el peso
 * (1 kg pesa lo mismo que 1 cm); por eso cada eje se normaliza con una
 * tolerancia propia: 5 cm de estatura equivalen a 1 kg de peso.
 */
public final class DistanciaBiometrica {

    /** Desborde en estatura que equivale a una unidad de distancia. */
    public static final double TOLERANCIA_ESTATURA_CM = 5.0;
    /** Desborde en peso que equivale a una unidad de distancia. */
    public static final double TOLERANCIA_PESO_KG = 1.0;
    /**
     * Distancia máxima aceptada para sugerir por proximidad. Por encima se
     * devuelve "sin talla compatible" en lugar de una talla absurda (RNF-11):
     * por ejemplo 150 cm / 40 kg frente a un máximo de 122 cm / 26 kg da
     * ~15 unidades, muy por encima del umbral.
     */
    public static final double DISTANCIA_MAXIMA = 6.0;

    /** Clase de utilidades; no se instancia. */
    private DistanciaBiometrica() {
    }

    /** Cuánto queda fuera del rango (0 si el valor cabe dentro). */
    public static double desborde(BigDecimal valor, BigDecimal min, BigDecimal max) {
        if (valor.compareTo(min) < 0) return min.subtract(valor).doubleValue();
        if (valor.compareTo(max) > 0) return valor.subtract(max).doubleValue();
        return 0.0;
    }

    /** Distancia normalizada al rango: 0 si el niño cabe en la talla. */
    public static double alRango(DatosBiometricos bio, RangoTalla r) {
        double de = desborde(bio.estaturaCm(), r.estaturaMinCm(), r.estaturaMaxCm()) / TOLERANCIA_ESTATURA_CM;
        double dp = desborde(bio.pesoKg(), r.pesoMinKg(), r.pesoMaxKg()) / TOLERANCIA_PESO_KG;
        return Math.sqrt(de * de + dp * dp);
    }

    /** Distancia normalizada al centro del rango (para el puntaje fino). */
    public static double alCentro(DatosBiometricos bio, RangoTalla r) {
        double de = centro(r.estaturaMinCm(), r.estaturaMaxCm()).subtract(bio.estaturaCm()).doubleValue()
                / TOLERANCIA_ESTATURA_CM;
        double dp = centro(r.pesoMinKg(), r.pesoMaxKg()).subtract(bio.pesoKg()).doubleValue()
                / TOLERANCIA_PESO_KG;
        return Math.sqrt(de * de + dp * dp);
    }

    /** Punto medio del rango. */
    private static BigDecimal centro(BigDecimal min, BigDecimal max) {
        return min.add(max).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
    }
}
