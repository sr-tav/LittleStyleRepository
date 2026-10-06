package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.RangoTalla;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.TablaTallas;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto.DatosBiometricos;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class EstrategiaTallaTest {
    private TablaTallas tabla() {
        return new TablaTallas(List.of( new RangoTalla("4", new BigDecimal("100"), new BigDecimal("110"), new BigDecimal("15"), new BigDecimal("20")),
                new RangoTalla("6", new BigDecimal("111"), new BigDecimal("122"), new BigDecimal("21"), new BigDecimal("26"))));
    }
    private DatosBiometricos bio(String est, String peso, Holgura h) {
        return new DatosBiometricos(new BigDecimal(est), new BigDecimal(peso), LocalDate.now(), h, Contextura.MEDIA);
    }
    @Test
    void exactaEnBorde() {
        assertEquals("4", new EstrategiaExacta().sugerir(bio("110","20",Holgura.REGULAR), tabla()).orElseThrow().talla());
    }
    @Test void proximidadCuandoNoHayExacta() {
        var ex = new EstrategiaExacta().sugerir(bio("150","40",Holgura.REGULAR), tabla());
        assertTrue(ex.isEmpty());
        assertEquals("6", new EstrategiaProximidad().sugerir(bio("150","40",Holgura.REGULAR), tabla()).orElseThrow().talla());
    }
    @Test void holgadaSubeUnaTalla() {
        var e = new EstrategiaConHolgura(new EstrategiaExacta(), new EstrategiaProximidad());
        assertEquals("6", e.sugerir(bio("105","18",Holgura.HOLGADA), tabla()).orElseThrow().talla());
        assertEquals("4", e.sugerir(bio("105","18",Holgura.REGULAR), tabla()).orElseThrow().talla());
    }
    @Test void tablaVaciaRetornaEmpty() {
        assertTrue(new EstrategiaExacta().sugerir(bio("105","18",Holgura.REGULAR), new TablaTallas(List.of())).isEmpty());
    }
}
