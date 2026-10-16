package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto.DatosBiometricos;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.RangoTalla;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.TablaTallas;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Holgura;
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
        if (bio.holgura() == null || !fueExacta) return base;
        // El ajuste por holgura solo aplica sobre una coincidencia exacta y
        // nunca se sale de la tabla. El stock de la talla resultante no se
        // decide aquí (la estrategia no conoce inventario): el motor lo informa
        // con tieneStock y el checkout lo valida antes de vender (RNF-03).
        int i = tabla.rangos().indexOf(base.get());
        if (bio.holgura() == Holgura.HOLGADA && i < tabla.rangos().size() - 1) {
            return Optional.of(tabla.rangos().get(i + 1));
        }
        if (bio.holgura() == Holgura.AJUSTADA && i > 0) {
            return Optional.of(tabla.rangos().get(i - 1));
        }
        return base;
    }

    @Override
    public String nombre(){
        return "HOLGURA";
    }
}
