package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto.DatosBiometricos;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.RangoTalla;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.TablaTallas;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.service.DistanciaBiometrica;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Optional;

/** Sugiere la talla más cercana a las medidas del niño, dentro de una distancia máxima. */
@Component
public class EstrategiaProximidad implements EstrategiaTalla {

    /**
     * Talla con menor distancia normalizada, o vacío si incluso la más cercana supera {@link
     * DistanciaBiometrica#DISTANCIA_MAXIMA}.
     */
    @Override
    public Optional<RangoTalla> sugerir(DatosBiometricos bio, TablaTallas tabla) {
        if (bio == null || tabla == null || tabla.estaVacia()) return Optional.empty();
        return tabla.rangos().stream().min(Comparator.comparingDouble(r -> DistanciaBiometrica.alRango(bio, r)))
                .filter(r -> DistanciaBiometrica.alRango(bio, r) <= DistanciaBiometrica.DISTANCIA_MAXIMA);
    }

    /** Nombre de configuración: {@code PROXIMIDAD}. */
    @Override
    public String nombre() {
        return "PROXIMIDAD";
    }

}
