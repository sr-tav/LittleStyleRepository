package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy;

import java.util.List;

/** Resultado de evaluar una prenda contra las alergias: nivel de riesgo y motivos. */
public record EvaluacionAlergia(NivelRiesgo nivel, List<String> motivos) {
    /** Guarda una copia inmutable de los motivos. */
    public EvaluacionAlergia {
        motivos = motivos == null ? List.of() : List.copyOf(motivos);
    }

    /** Evaluación sin riesgo ni motivos. */
    public static EvaluacionAlergia apta() {
        return new EvaluacionAlergia(NivelRiesgo.APTA, List.of());
    }

    /** Indica si la prenda no debe recomendarse ni venderse para el perfil. */
    public boolean excluye() {
        return nivel == NivelRiesgo.EXCLUIDA;
    }
}
