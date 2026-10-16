package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.ComponenteMaterial;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.MaterialTextil;
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
    private PrendaRecomendable prendaFull(List<ComponenteMaterial> comp, boolean tintes, boolean broches,
                                          boolean niquel, boolean formol) {
        return new PrendaRecomendable(1L,"P","C","M", BigDecimal.TEN,null,
                new TablaTallas(List.of()),comp,tintes,broches,niquel,formol,null,null,Map.of());
    }
    @Test void sinteticoEnElUmbralExcluyeYBajoElUmbralNo() {
        var justo = prenda(List.of(new ComponenteMaterial(MaterialTextil.POLIESTER,30)),false,false);
        var bajo = prenda(List.of(new ComponenteMaterial(MaterialTextil.POLIESTER,29),
                new ComponenteMaterial(MaterialTextil.ALGODON,71)),false,false);
        var filtro = new FiltroAlergiaEstandar();
        assertEquals(NivelRiesgo.EXCLUIDA,
                filtro.evaluar(Set.of(AlergiaTextil.FIBRAS_SINTETICAS), justo).nivel());
        assertEquals(NivelRiesgo.APTA,
                filtro.evaluar(Set.of(AlergiaTextil.FIBRAS_SINTETICAS), bajo).nivel());
    }
    @Test void formaldehidoExcluye() {
        var r = new FiltroAlergiaEstandar().evaluar(Set.of(AlergiaTextil.FORMALDEHIDO),
                prendaFull(List.of(),false,false,false,true));
        assertEquals(NivelRiesgo.EXCLUIDA, r.nivel());
    }
    @Test void lanaSoloAdvierte() {
        var r = new FiltroAlergiaEstandar().evaluar(Set.of(AlergiaTextil.LANA),
                prenda(List.of(new ComponenteMaterial(MaterialTextil.LANA,100)),false,false));
        assertEquals(NivelRiesgo.ADVERTENCIA, r.nivel());
    }
    @Test void latexOElastanoSoloAdvierte() {
        var filtro = new FiltroAlergiaEstandar();
        assertEquals(NivelRiesgo.ADVERTENCIA, filtro.evaluar(Set.of(AlergiaTextil.LATEX_ELASTICO),
                prenda(List.of(new ComponenteMaterial(MaterialTextil.LATEX,5)),false,false)).nivel());
        assertEquals(NivelRiesgo.ADVERTENCIA, filtro.evaluar(Set.of(AlergiaTextil.LATEX_ELASTICO),
                prenda(List.of(new ComponenteMaterial(MaterialTextil.ELASTANO,5)),false,false)).nivel());
    }
    @Test void cueroSoloAdvierte() {
        var r = new FiltroAlergiaEstandar().evaluar(Set.of(AlergiaTextil.CUERO),
                prenda(List.of(new ComponenteMaterial(MaterialTextil.CUERO,100)),false,false));
        assertEquals(NivelRiesgo.ADVERTENCIA, r.nivel());
    }
    @Test void algodonSoloAdvierte() {
        var r = new FiltroAlergiaEstandar().evaluar(Set.of(AlergiaTextil.ALGODON),
                prenda(List.of(new ComponenteMaterial(MaterialTextil.ALGODON,100)),false,false));
        assertEquals(NivelRiesgo.ADVERTENCIA, r.nivel());
    }
    @Test void brochesSoloAdvierten() {
        var r = new FiltroAlergiaEstandar().evaluar(Set.of(AlergiaTextil.BROCHES_METALICOS),
                prendaFull(List.of(),false,true,false,false));
        assertEquals(NivelRiesgo.ADVERTENCIA, r.nivel());
    }
    @Test void variasAlergiasAcumulanMotivosYExcluyenSiAlgunaExcluye() {
        var r = new FiltroAlergiaEstandar().evaluar(Set.of(AlergiaTextil.NIQUEL, AlergiaTextil.LANA),
                prendaFull(List.of(new ComponenteMaterial(MaterialTextil.LANA,100)),false,false,true,false));
        assertEquals(NivelRiesgo.EXCLUIDA, r.nivel());
        assertEquals(2, r.motivos().size());
    }
    @Test void composicionVaciaSoloAdvierteConOtra() {
        var filtro = new FiltroAlergiaEstandar();
        var sinComposicion = prendaFull(List.of(),false,false,false,false);
        assertEquals(NivelRiesgo.ADVERTENCIA,
                filtro.evaluar(Set.of(AlergiaTextil.OTRA), sinComposicion).nivel());
        assertEquals(NivelRiesgo.APTA,
                filtro.evaluar(Set.of(AlergiaTextil.LANA), sinComposicion).nivel());
    }
    @Test void otraConComposicionDeclaradaEsApta() {
        var r = new FiltroAlergiaEstandar().evaluar(Set.of(AlergiaTextil.OTRA),
                prenda(List.of(new ComponenteMaterial(MaterialTextil.ALGODON,100)),false,false));
        assertEquals(NivelRiesgo.APTA, r.nivel());
    }
}
