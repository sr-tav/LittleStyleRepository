package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.adapter;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.*;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.port.CatalogoRecomendacionPort;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.*;
import java.math.BigDecimal; import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class CatalogoEjemploAdapter implements CatalogoRecomendacionPort {
    @Override
    public List<PrendaRecomendable> listarPrendasPublicadas() {
        return List.of(
                prenda(1L, "Remera Algodón", "CAMISETAS",
                        List.of(r("4", 100, 110, 15, 20), r("6", 111, 122, 21, 26)),
                        List.of(new ComponenteMaterial(MaterialTextil.ALGODON, 100)),
                        false, false, false, false, Map.of("4", 5, "6", 0)),
                prenda(2L, "Buzo Poliéster", "BUZOS",
                        List.of(r("4", 100, 110, 15, 20)),
                        List.of(new ComponenteMaterial(MaterialTextil.POLIESTER, 100)),
                        true, false, true, false, Map.of("4", 2)),
                prenda(3L, "Pantalón Mixto", "PANTALONES",
                        List.of(r("6", 111, 122, 21, 26)),
                        List.of(new ComponenteMaterial(MaterialTextil.ALGODON, 70),
                                new ComponenteMaterial(MaterialTextil.ELASTANO, 30)),
                        false, true, false, false, Map.of("6", 3))
        );
    }
    @Override
    public Optional<PrendaRecomendable> buscarPrendaPublicada(Long id) {
        return listarPrendasPublicadas().stream().filter(p -> p.id().equals(id)).findFirst();
    }
    private RangoTalla r(String t, int eMin, int eMax, int pMin, int pMax) {
        return new RangoTalla(t, BigDecimal.valueOf(eMin), BigDecimal.valueOf(eMax),
                BigDecimal.valueOf(pMin), BigDecimal.valueOf(pMax));
    }
    private PrendaRecomendable prenda(Long id, String nombre, String cat, List<RangoTalla> rangos,
                                      List<ComponenteMaterial> comp, boolean tintes, boolean broches, boolean niquel,
                                      boolean form, Map<String,Integer> stock) {
        return new PrendaRecomendable(id, nombre, cat, "Demo", BigDecimal.TEN, null,
                new TablaTallas(rangos), comp, tintes, broches, niquel, form,
                Set.of(ColorPreferido.AZUL), Estampado.LISO, stock);
    }
}