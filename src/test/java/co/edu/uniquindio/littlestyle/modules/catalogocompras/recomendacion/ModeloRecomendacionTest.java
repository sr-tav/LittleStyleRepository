package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.MaterialTextil;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Suite de los modelos de recomendación (records): valida las reglas de
 * construcción de RangoTalla y ComponenteMaterial, el ordenamiento de la
 * tabla de tallas y los valores por defecto de PrendaRecomendable.
 * Son tests puros, sin mocks.
 */
class ModeloRecomendacionTest {

    // Fábrica de rango de talla con enteros para no repetir BigDecimal en cada test
    private static RangoTalla rango(String talla, int eMin, int eMax, int pMin, int pMax) {
        return new RangoTalla(talla, BigDecimal.valueOf(eMin), BigDecimal.valueOf(eMax), BigDecimal.valueOf(pMin), BigDecimal.valueOf(pMax));
    }

    // Un rango con estatura mínima mayor a la máxima no tiene sentido: debe fallar en construcción
    @Test
    void rangoRechazaMinimoMayorQueMaximo() {
        assertThrows(IllegalArgumentException.class, () -> rango("4", 110, 100, 15, 20));
    }

    // Los extremos inclusivos: 100 y 110 entran, y el peso 21 queda fuera porque 20 es el tope
    @Test
    void rangoContieneValoresEnLosExtremos() {
        RangoTalla r = rango("4", 100, 110, 15, 20);
        assertTrue(r.contieneEstatura(BigDecimal.valueOf(100)));
        assertTrue(r.contieneEstatura(BigDecimal.valueOf(110)));
        assertFalse(r.contienePeso(BigDecimal.valueOf(21)));
    }

    // Aunque los rangos lleguen desordenados, la tabla los ordena por estatura de menor a mayor
    @Test
    void tablaSeOrdenaPorEstatura() {
        TablaTallas t = new TablaTallas(List.of(rango("6", 111, 120, 21, 25), rango("4", 100, 110, 15, 20)));
        assertEquals("4", t.rangos().get(0).talla());
    }

    // El porcentaje de material debe estar entre 1 y 100: ni cero ni por encima del tope
    @Test
    void componenteRechazaPorcentajeFueraDeRango() {
        assertThrows(IllegalArgumentException.class, () -> new ComponenteMaterial(MaterialTextil.ALGODON, 0));
        assertThrows(IllegalArgumentException.class, () -> new ComponenteMaterial(MaterialTextil.ALGODON, 101));
    }

    // Con tabla nula y composición nula la prenda no explota: normaliza a vacío
    // y el stock se calcula según la cantidad por talla del mapa
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
