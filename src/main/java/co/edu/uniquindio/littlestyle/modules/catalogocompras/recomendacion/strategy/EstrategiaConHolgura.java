package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto.DatosBiometricos;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.RangoTalla;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.TablaTallas;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Primary
@RequiredArgsConstructor
public class EstrategiaConHolgura implements  EstrategiaTalla {
    private final EstrategiaExacta exacta;
    private final EstrategiaProximidad proximidad;

    @Override
    public Optional<RangoTalla> sugerir(DatosBiometricos bio, TablaTallas tabla) {
        if (bio == null || tabla == null || tabla.estaVacia()) return Optional.empty();
        Optional<RangoTalla> base = exacta.sugerir(bio, tabla);
        boolean fueExacta = base.isPresent();
        if (base.isEmpty()) base = proximidad.sugerir(bio, tabla);
        if (base.isEmpty()) return Optional.empty();
        if (bio.holgura() == null) return base;
        int i = tabla.rangos().indexOf(base.get());
        if (fueExacta && bio.holgura().name().equals("HOLGADA") && i < tabla.rangos().size() -1) {
            return Optional.of(tabla.rangos().get(i +1));
        }
        return base;
    }

    @Override
    public String nombre(){
        return "HOLGURA";
    }
}
