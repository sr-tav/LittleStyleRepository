package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.service;

import co.edu.uniquindio.littlestyle.config.RecomendacionProperties;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.PrendaRecomendable;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.RangoTalla;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto.*;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.port.CatalogoRecomendacionPort;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy.*;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.AlergiaTextil;
import java.math.BigDecimal; import java.math.RoundingMode;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MotorRecomendacionService {
    private final CatalogoRecomendacionPort catalogo;
    private final EstrategiaTalla estrategia;
    private final FiltroAlergia filtro;
    private final RecomendacionProperties props;

    public List<RecomendacionItem> recomendar(DatosBiometricos bio, Set<AlergiaTextil> alergias) {
        Set<AlergiaTextil> al = alergias == null ? Set.of() : alergias;
        List<PrendaRecomendable> prendas;
        try {
            prendas = catalogo.listarPrendasPublicadas();
        } catch (Exception ex) {
            log.warn("Catálogo no disponible, degradación a lista vacía", ex); // RNF-29
            return List.of();
        }
        if (prendas == null || prendas.isEmpty()) return List.of();
        List<RecomendacionItem> salida = new ArrayList<>();
        for (PrendaRecomendable p : prendas) {
            try {
                if (p == null || p.tablaTallas().estaVacia()) continue;
                var talla = estrategia.sugerir(bio, p.tablaTallas());
                if (talla.isEmpty()) continue;
                var eval = filtro.evaluar(al, p);
                if (eval.excluye()) continue;
                int puntaje = 100 - distancia(bio, talla.get())
                        - (eval.nivel() == NivelRiesgo.ADVERTENCIA ? 30 : 0)
                        + (p.tieneStock(talla.get().talla()) ? 5 : 0);
                salida.add(new RecomendacionItem(p.id(), p.nombre(), talla.get().talla(),
                        Math.max(0, puntaje), eval.nivel(), eval.motivos(), p.tieneStock(talla.get().talla())));
            } catch (Exception ex) {
                log.warn("Prenda omitida por fallo aislado", ex); // una prenda nunca tumba el motor
            }
        }
        return salida.stream()
                .sorted(Comparator.comparingInt(RecomendacionItem::puntaje).reversed())
                .limit(props.maxResultados()).toList();
    }

    private int distancia(DatosBiometricos bio, RangoTalla r) {
        BigDecimal ce = r.estaturaMinCm().add(r.estaturaMaxCm()).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
        BigDecimal cp = r.pesoMinKg().add(r.pesoMaxKg()).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
        double de = ce.subtract(bio.estaturaCm()).doubleValue();
        double dp = cp.subtract(bio.pesoKg()).doubleValue();
        return (int) Math.sqrt(de * de + dp * dp);
    }
}
