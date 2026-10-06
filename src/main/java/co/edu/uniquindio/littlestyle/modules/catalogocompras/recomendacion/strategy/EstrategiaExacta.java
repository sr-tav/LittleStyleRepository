package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.RangoTalla;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.TablaTallas;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto.DatosBiometricos;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(1)
public class EstrategiaExacta implements EstrategiaTalla {

    @Override
    public Optional<RangoTalla> sugerir(DatosBiometricos bio, TablaTallas tabla) {
        if (bio == null || tabla == null || tabla.estaVacia()) return Optional.empty();
        return tabla.rangos().stream().filter(r -> r.contieneEstatura(bio.estaturaCm()) && r.contienePeso(bio.pesoKg())).findFirst();
    }

    @Override
    public String nombre() {
        return "EXACTA";
    }
}
