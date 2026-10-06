package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.port;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.PrendaRecomendable;

import java.util.List;
import java.util.Optional;

public interface CatalogoRecomendacionPort {

    /** Prendas publicadas y activas. */
    List<PrendaRecomendable> listarPrendasPublicadas();
    Optional<PrendaRecomendable> buscarPrendaPublicada(Long prendaId);
}
