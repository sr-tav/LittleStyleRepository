package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


 class ModeloRecomendacionTest {

    private static RangoTalla rango(String talla, int eMin, int eMax, int pMin, int pMax) {
        return new RangoTalla(talla, BigDecimal.valueOf(eMin), BigDecimal.valueOf(eMax), BigDecimal.valueOf(pMin), BigDecimal.valueOf(pMax));
    }

    @Test
    void rangoRechazaMinimoMayorQueMaximo() {
        assertThrows(IllegalArgumentException.class, () -> rango("4", 110, 100, 15, 20));
    }

    @Test
    void rangoContieneValoresEnLosExtremos() {
        RangoTalla r = rango("4", 100, 110, 15, 20);
        assertTrue(r.contieneEstatura(BigDecimal.valueOf(100)));
        assertTrue(r.contieneEstatura(BigDecimal.valueOf(110)));
        assertFalse(r.contienePeso(BigDecimal.valueOf(21)));
    }

    @Test
    void tablaSeOrdenaPorEstatura() {
        TablaTallas t = new TablaTallas(List.of(rango("6", 111, 120, 21, 25), rango("4", 100, 110, 15, 20)));
        assertEquals("4", t.rangos().get(0).talla());
    }

    @Test
    void componenteRechazaPorcentajeFueraDeRango() {
        assertThrows(IllegalArgumentException.class, () -> new ComponenteMaterial(MaterialTextil.ALGODON, 0));
        assertThrows(IllegalArgumentException.class, () -> new ComponenteMaterial(MaterialTextil.ALGODON, 101));
    }

    @Test
    void prendaNormalizaNulosYReportaStock() {
        PrendaRecomendable p = new PrendaRecomendable(1L, "Vestido", "VESTIDOS", "Marca", BigDecimal.TEN, null, null, null, false,
                false, false, false, null, null, Map.of("4", 3, "6", 0));
        assertTrue(p.tablaTallas().estaVacia());
        assertFalse(p.tieneComposicion());
        assertTrue(p.tieneStock("4"));
        assertFalse(p.tieneStock("6"));
        assertFalse(p.tieneStock("8"));
    }
}
