package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy;

import java.util.List;

public record EvaluacionAlergia(NivelRiesgo nivel, List<String> motivos) {
    public EvaluacionAlergia {
        motivos = motivos == null ? List.of() : List.copyOf(motivos);
    }

    public static EvaluacionAlergia apta() {
        return new EvaluacionAlergia(NivelRiesgo.APTA, List.of());
    }

    public boolean excluye() {
        return nivel == NivelRiesgo.EXCLUIDA;
    }
}
