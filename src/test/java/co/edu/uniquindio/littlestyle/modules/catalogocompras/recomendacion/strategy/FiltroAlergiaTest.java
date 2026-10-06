package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.ComponenteMaterial;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.MaterialTextil;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.PrendaRecomendable;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.TablaTallas;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.AlergiaTextil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.*;

public class FiltroAlergiaTest {
    private PrendaRecomendable prenda(List<ComponenteMaterial> comp, boolean tintes, boolean niquel) {
        return new PrendaRecomendable(1L,"P","C","M", BigDecimal.TEN,null,
                new TablaTallas(List.of()),comp,tintes,false,niquel,false,null,null,Map.of());
    }
    @Test
    void sinAlergiasEsApta() {
        assertEquals(NivelRiesgo.APTA, new FiltroAlergiaEstandar().evaluar(Set.of(), prenda(List.of(),false,false)).nivel());
    }
    @Test void sintetico100Excluye() {
        var r = new FiltroAlergiaEstandar().evaluar(Set.of(AlergiaTextil.FIBRAS_SINTETICAS),
                prenda(List.of(new ComponenteMaterial(MaterialTextil.POLIESTER,100)),false,false));
        assertEquals(NivelRiesgo.EXCLUIDA, r.nivel());
    }
    @Test void tintesSoloAdvierte() {
        var r = new FiltroAlergiaEstandar().evaluar(Set.of(AlergiaTextil.TINTES), prenda(List.of(),true,false));
        assertEquals(NivelRiesgo.ADVERTENCIA, r.nivel());
    }
    @Test void niquelExcluye() {
        var r = new FiltroAlergiaEstandar().evaluar(Set.of(AlergiaTextil.NIQUEL), prenda(List.of(),false,true));
        assertEquals(NivelRiesgo.EXCLUIDA, r.nivel());
    }
}
