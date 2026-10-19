package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.PrendaRecomendable;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.AlergiaTextil;

import java.util.Set;

/** Evalúa el riesgo de una prenda para un conjunto de alergias textiles. */
public interface FiltroAlergia {
    /** Nivel de riesgo de la prenda y los motivos que lo explican. */
    EvaluacionAlergia evaluar(Set<AlergiaTextil> alergias, PrendaRecomendable prenda);
}
