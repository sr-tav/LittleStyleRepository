package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.service;

import co.edu.uniquindio.littlestyle.config.RecomendacionProperties;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto.*;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.*;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.port.CatalogoRecomendacionPort;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy.*;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.*;
import java.math.BigDecimal; import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock; import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

    @ExtendWith(MockitoExtension.class)
    class MotorRecomendacionServiceTest {

        @Mock CatalogoRecomendacionPort catalogo;

        private MotorRecomendacionService motor() {
            return new MotorRecomendacionService(catalogo,
                    new EstrategiaConHolgura(new EstrategiaExacta(), new EstrategiaProximidad()),
                    new FiltroAlergiaEstandar(),
                    new RecomendacionProperties(6, 24));
        }
        private DatosBiometricos bio() {
            return new DatosBiometricos(new BigDecimal("105"), new BigDecimal("18"),
                    LocalDate.now(), Holgura.REGULAR, Contextura.MEDIA);
        }
        private PrendaRecomendable prenda(Long id, List<RangoTalla> rangos,
                                          List<ComponenteMaterial> comp, boolean niquel, Map<String,Integer> stock) {
            return new PrendaRecomendable(id, "P"+id, "C", "M", BigDecimal.TEN, null,
                    new TablaTallas(rangos), comp, false, false, niquel, false, null, null, stock);
        }
        private RangoTalla r(String t, int e1, int e2, int p1, int p2) {
            return new RangoTalla(t, BigDecimal.valueOf(e1), BigDecimal.valueOf(e2),
                    BigDecimal.valueOf(p1), BigDecimal.valueOf(p2));
        }

        @Test void ordenaExcluyeEIgnoraTablaVacia() {
            var buena = prenda(1L, List.of(r("4",100,110,15,20)),
                    List.of(new ComponenteMaterial(MaterialTextil.ALGODON,100)), false, Map.of("4",5));
            var niquel = prenda(2L, List.of(r("4",100,110,15,20)),
                    List.of(), false, Map.of("4",5));
            // le forzamos niquel=true via constructor: el boolean 9 es contieneNiquel
            niquel = new PrendaRecomendable(2L,"P2","C","M",BigDecimal.TEN,null,
                    new TablaTallas(List.of(r("4",100,110,15,20))), List.of(),
                    false,false,true,false,null,null,Map.of("4",5));
            var sinTabla = prenda(3L, List.of(), List.of(), false, Map.of());
            when(catalogo.listarPrendasPublicadas()).thenReturn(List.of(niquel, sinTabla, buena));

            var res = motor().recomendar(bio(), Set.of(AlergiaTextil.NIQUEL));

            assertEquals(1, res.size());
            assertEquals(1L, res.get(0).prendaId());
            assertEquals("4", res.get(0).tallaSugerida());
        }

        @Test void degradacionAnteFalloCatalogo() {
            when(catalogo.listarPrendasPublicadas()).thenThrow(new RuntimeException("caido"));
            assertTrue(motor().recomendar(bio(), Set.of()).isEmpty());
        }

        @Test void unaPrendaRotaNoTumbaLasDemas() {
            var buena = prenda(1L, List.of(r("4",100,110,15,20)),
                    List.of(new ComponenteMaterial(MaterialTextil.ALGODON,100)), false, Map.of("4",1));
            when(catalogo.listarPrendasPublicadas()).thenReturn(Arrays.asList(null, buena));
            var res = motor().recomendar(bio(), Set.of());
            assertEquals(1, res.size()); // RNF-29
        }

        @Test void porPrendaDevuelveTallaSugerida() {
            var buena = prenda(1L, List.of(r("4",100,110,15,20)),
                    List.of(new ComponenteMaterial(MaterialTextil.ALGODON,100)), false, Map.of("4",5));
            when(catalogo.buscarPrendaPublicada(1L)).thenReturn(Optional.of(buena));

            var res = motor().recomendarParaPrenda(bio(), Set.of(), 1L);

            assertEquals(1L, res.prendaId());
            assertEquals("4", res.tallaSugerida());
            assertTrue(res.tieneStock());
        }

        @Test void porPrendaExcluidaLanza422() {
            var niquel = new PrendaRecomendable(2L,"P2","C","M",BigDecimal.TEN,null,
                    new TablaTallas(List.of(r("4",100,110,15,20))), List.of(),
                    false,false,true,false,null,null,Map.of("4",5));
            when(catalogo.buscarPrendaPublicada(2L)).thenReturn(Optional.of(niquel));

            var ex = assertThrows(co.edu.uniquindio.littlestyle.shared.exception.BusinessException.class,
                    () -> motor().recomendarParaPrenda(bio(), Set.of(AlergiaTextil.NIQUEL), 2L));
            assertEquals(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        }

        @Test void porPrendaInexistenteLanza404() {
            when(catalogo.buscarPrendaPublicada(99L)).thenReturn(Optional.empty());

            var ex = assertThrows(co.edu.uniquindio.littlestyle.shared.exception.BusinessException.class,
                    () -> motor().recomendarParaPrenda(bio(), Set.of(), 99L));
            assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatus());
        }
}
