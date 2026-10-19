package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.port;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.PrendaRecomendable;

import java.util.List;
import java.util.Optional;

/** Puerto del motor hacia el catálogo, para no depender de sus entidades. */
public interface CatalogoRecomendacionPort {

    /** Prendas publicadas y activas. */
    List<PrendaRecomendable> listarPrendasPublicadas();
    /** Prenda publicada por id, o vacío si no existe. */
    Optional<PrendaRecomendable> buscarPrendaPublicada(Long prendaId);
}
