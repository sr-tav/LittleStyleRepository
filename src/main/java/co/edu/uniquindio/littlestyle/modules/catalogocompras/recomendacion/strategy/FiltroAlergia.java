package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.PrendaRecomendable;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.AlergiaTextil;

import java.util.Set;

public interface FiltroAlergia {
    EvaluacionAlergia evaluar(Set<AlergiaTextil> alergias, PrendaRecomendable prenda);
}
