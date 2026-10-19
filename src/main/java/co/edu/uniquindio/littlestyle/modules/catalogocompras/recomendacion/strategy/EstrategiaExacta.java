package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.RangoTalla;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.TablaTallas;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto.DatosBiometricos;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Sugiere solo una talla cuyos rangos contengan la estatura y el peso del niño. */
@Component
public class EstrategiaExacta implements EstrategiaTalla {

    /** Primera talla (de menor a mayor) que contiene estatura y peso, o vacío. */
    @Override
    public Optional<RangoTalla> sugerir(DatosBiometricos bio, TablaTallas tabla) {
        if (bio == null || tabla == null || tabla.estaVacia()) return Optional.empty();
        return tabla.rangos().stream().filter(r -> r.contieneEstatura(bio.estaturaCm()) && r.contienePeso(bio.pesoKg())).findFirst();
    }

    /** Nombre de configuración: {@code EXACTA}. */
    @Override
    public String nombre() {
        return "EXACTA";
    }
}
