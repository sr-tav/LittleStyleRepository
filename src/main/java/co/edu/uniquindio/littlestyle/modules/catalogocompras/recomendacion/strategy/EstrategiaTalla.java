package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto.DatosBiometricos;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.RangoTalla;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.TablaTallas;

import java.util.Optional;

/** Estrategia para elegir la talla de una prenda según las medidas del niño (ADR-12). */
public interface EstrategiaTalla {
    /** Talla sugerida de la tabla, o vacío si ninguna es compatible. */
    Optional<RangoTalla> sugerir(DatosBiometricos bio, TablaTallas tabla);
    /** Nombre con el que se selecciona la estrategia en la configuración. */
    String nombre();
}
