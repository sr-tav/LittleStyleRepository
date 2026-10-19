package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto.DatosBiometricos;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.PrendaRecomendable;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.RangoTalla;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy.EvaluacionAlergia;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy.NivelRiesgo;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Estampado;

import java.util.Set;

/** Cálculo del puntaje de compatibilidad y de la afinidad de estilo. */
public final class PuntajeRecomendacion {
    /** Clase de utilidades; no se instancia. */
    private PuntajeRecomendacion(){}

    /** Orden de prioridad US-07: alergias y talla son filtros duros (van
     * antes, en el motor); aquí solo puntúan talla y seguridad. */
    public static final int PESO_ADVERTENCIA = 30;
    public static final int PESO_STOCK = 5;
    /** Afinidad: +5 color, +5 estampado, con tope. Campo aparte del puntaje. */
    public static final int AFINIDAD_COLOR = 5;
    public static final int AFINIDAD_ESTAMPADO = 5;
    public static final int AFINIDAD_MAXIMA = 10;

    /** Puntos de penalización por la distancia de las medidas al centro de la talla. */
    public static int distancia(DatosBiometricos bio, RangoTalla rango) {
        // Distancia normalizada (5 cm = 1 kg) reescalada a puntos: conserva la
        // sensibilidad histórica (~1 punto por cm de desvío en estatura).
        return (int) Math.round(DistanciaBiometrica.alCentro(bio, rango)
                * DistanciaBiometrica.TOLERANCIA_ESTATURA_CM);
    }

    /**
     * Compatibilidad 0-100: SOLO talla y seguridad. Las preferencias van en
     * {@link #afinidad} y nunca compensan una advertencia.
     */
    public static int puntaje(DatosBiometricos bio, RangoTalla talla, EvaluacionAlergia evaluacion, boolean tieneStock) {
        int p = 100 - distancia(bio, talla) - (evaluacion.nivel() == NivelRiesgo.ADVERTENCIA ? PESO_ADVERTENCIA : 0)
                + (tieneStock ? PESO_STOCK : 0);
        return Math.min(100, Math.max(0, p));
    }

    /**
     * Afinidad de estilo 0-10, campo aparte: +5 si algún color de la prenda
     * está entre los preferidos, +5 si coincide el estampado, con tope.
     * Solo reordena prendas ya seguras y de talla correcta; nunca excluye.
     * La contextura se captura en el perfil pero no puntúa.
     */
    public static int afinidad(Set<ColorPreferido> coloresPreferidos, Set<Estampado> estampadosPreferidos,
                               PrendaRecomendable prenda) {
        if (prenda == null) return 0;
        int a = 0;
        if (coloresPreferidos != null && !coloresPreferidos.isEmpty() && prenda.colores() != null
                && prenda.colores().stream().anyMatch(coloresPreferidos::contains)) {
            a += AFINIDAD_COLOR;
        }
        if (estampadosPreferidos != null && !estampadosPreferidos.isEmpty() && prenda.estampado() != null
                && estampadosPreferidos.contains(prenda.estampado())) {
            a += AFINIDAD_ESTAMPADO;
        }
        return Math.min(AFINIDAD_MAXIMA, a);
    }
}
