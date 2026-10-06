package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto.DatosBiometricos;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.RangoTalla;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.TablaTallas;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Optional;

@Component
@Order(2)
public class EstrategiaProximidad implements EstrategiaTalla {

    @Override
    public Optional<RangoTalla> sugerir(DatosBiometricos bio, TablaTallas tabla) {
        if (bio == null || tabla == null || tabla.estaVacia()) return Optional.empty();
        return tabla.rangos().stream().min(Comparator.comparingDouble(r -> distanciaCuadratica(bio, r)));
    }

    private double distanciaCuadratica(DatosBiometricos bio, RangoTalla r) {
        double de = centro(r.estaturaMinCm(), r.estaturaMaxCm()).subtract(bio.estaturaCm()).doubleValue();
        double dp = centro(r.pesoMinKg(), r.pesoMaxKg()).subtract(bio.pesoKg()).doubleValue();
        return de * de + dp * dp;
    }

    private BigDecimal centro(BigDecimal min, BigDecimal max) {
        return min.add(max).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
    }

    @Override
    public String nombre() {
        return "PROXIMIDAD";
    }

}
