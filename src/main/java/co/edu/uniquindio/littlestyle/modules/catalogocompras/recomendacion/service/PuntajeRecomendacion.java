package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto.DatosBiometricos;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.RangoTalla;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy.EvaluacionAlergia;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy.NivelRiesgo;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PuntajeRecomendacion {
    private PuntajeRecomendacion(){}

    public static int distancia(DatosBiometricos bio, RangoTalla rango) {
        BigDecimal ce = rango.estaturaMinCm().add(rango.estaturaMaxCm()).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
        BigDecimal cp = rango.pesoMinKg().add(rango.pesoMaxKg()).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
        double de = ce.subtract(bio.estaturaCm()).doubleValue();
        double dp = cp.subtract(bio.pesoKg()).doubleValue();
        return (int) Math.sqrt(de *de + dp * dp);
    }

    public static int puntaje (DatosBiometricos bio, RangoTalla talla, EvaluacionAlergia evaluacion, boolean tieneStock) {
        int p = 100 - distancia(bio, talla) - (evaluacion.nivel() == NivelRiesgo.ADVERTENCIA ? 30 : 0) + (tieneStock ? 5 : 0);
        return Math.max(0, p);
    }
}
