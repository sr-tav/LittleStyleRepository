package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.service;

import co.edu.uniquindio.littlestyle.config.RecomendacionProperties;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.MaterialTextil;
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

/**
 * Suite del motor de recomendación: cubre ordenamiento y exclusiones,
 * degradación ante fallos del catálogo, errores del endpoint por prenda,
 * reglas de puntaje/afinidad y la selección de estrategia por categoría
 * o marca. Solo se mockea el puerto del catálogo.
 */
@ExtendWith(MockitoExtension.class)
class MotorRecomendacionServiceTest {

    @Mock CatalogoRecomendacionPort catalogo;

    // Arma el motor con la estrategia por defecto (holgura) y el filtro estándar
    private MotorRecomendacionService motor() {
            var holgura = new EstrategiaConHolgura(new EstrategiaExacta(), new EstrategiaProximidad());
            return new MotorRecomendacionService(catalogo, holgura,
                    List.of(new EstrategiaExacta(), new EstrategiaProximidad(), holgura),
                    new FiltroAlergiaEstandar(),
                    new RecomendacionProperties(6, 24, null, null, null));
        }
        // Datos biometricos típicos de una niña de la talla 4
        private DatosBiometricos bio() {
            return new DatosBiometricos(new BigDecimal("105"), new BigDecimal("18"),
                    LocalDate.now(), Holgura.REGULAR, Contextura.MEDIA);
        }
        // Perfil listo para usar con las alergias que se le pasen
        private PerfilBiometrico pb(Set<AlergiaTextil> alergias) {
            return new PerfilBiometrico(bio(), alergias, null, Set.of(), Set.of());
        }
        // Fábrica de prenda recomendable con los mínimos para pasar el filtro
        private PrendaRecomendable prenda(Long id, List<RangoTalla> rangos,
                                          List<ComponenteMaterial> comp, boolean niquel, Map<String,Integer> stock) {
            return new PrendaRecomendable(id, "P"+id, "C", "M", BigDecimal.TEN, null,
                    new TablaTallas(rangos), comp, false, false, niquel, false, null, null, stock);
        }
        // Atajo para construir rangos de talla con enteros
        private RangoTalla r(String t, int e1, int e2, int p1, int p2) {
            return new RangoTalla(t, BigDecimal.valueOf(e1), BigDecimal.valueOf(e2),
                    BigDecimal.valueOf(p1), BigDecimal.valueOf(p2));
        }

        // La prenda de níquel queda fuera por el filtro y la sin tabla se salta;
        // las otras dos se ordenan por puntaje (el stock disponible suma)
        @Test void ordenaExcluyeEIgnoraTablaVacia() {
            var mejor = prenda(1L, List.of(r("4",100,110,15,20)),
                    List.of(new ComponenteMaterial(MaterialTextil.ALGODON,100)), false, Map.of("4",5));
            var peor = prenda(3L, List.of(r("4",100,110,15,20)),
                    List.of(new ComponenteMaterial(MaterialTextil.ALGODON,100)), false, Map.of("4",0));
            var niquel = new PrendaRecomendable(2L,"P2","C","M",BigDecimal.TEN,null,
                    new TablaTallas(List.of(r("4",100,110,15,20))), List.of(),
                    false,false,true,false,null,null,Map.of("4",5));
            var sinTabla = prenda(4L, List.of(), List.of(), false, Map.of());
            when(catalogo.listarPrendasPublicadas()).thenReturn(List.of(niquel, sinTabla, peor, mejor));

            var res = motor().recomendar(pb(Set.of(AlergiaTextil.NIQUEL)));

            // Excluida por níquel y sin tabla fuera; ordena por puntaje (stock suma).
            assertEquals(2, res.size());
            assertEquals(1L, res.get(0).prendaId());
            assertEquals("4", res.get(0).tallaSugerida());
            assertEquals(3L, res.get(1).prendaId());
        }

        // Si el catálogo está caído, el motor responde lista vacía en vez de explotar
        @Test void degradacionAnteFalloCatalogo() {
            when(catalogo.listarPrendasPublicadas()).thenThrow(new RuntimeException("caido"));
            assertTrue(motor().recomendar(pb(Set.of())).isEmpty());
        }

        // Un null colado en el listado no debe tumbar la recomendación para el resto
        @Test void unaPrendaRotaNoTumbaLasDemas() {
            var buena = prenda(1L, List.of(r("4",100,110,15,20)),
                    List.of(new ComponenteMaterial(MaterialTextil.ALGODON,100)), false, Map.of("4",1));
            when(catalogo.listarPrendasPublicadas()).thenReturn(Arrays.asList(null, buena));
            var res = motor().recomendar(pb(Set.of()));
            assertEquals(1, res.size()); // RNF-29
        }

        // Para una prenda puntual el motor devuelve la talla que le queda bien y con stock
        @Test void porPrendaDevuelveTallaSugerida() {
            var buena = prenda(1L, List.of(r("4",100,110,15,20)),
                    List.of(new ComponenteMaterial(MaterialTextil.ALGODON,100)), false, Map.of("4",5));
            when(catalogo.buscarPrendaPublicada(1L)).thenReturn(Optional.of(buena));

            var res = motor().recomendarParaPrenda(pb(Set.of()), 1L);

            assertEquals(1L, res.prendaId());
            assertEquals("4", res.tallaSugerida());
            assertTrue(res.tieneStock());
        }

        // Prenda con níquel + perfil alérgico: el negocio lo rechaza con 422
        @Test void porPrendaExcluidaLanza422() {
            var niquel = new PrendaRecomendable(2L,"P2","C","M",BigDecimal.TEN,null,
                    new TablaTallas(List.of(r("4",100,110,15,20))), List.of(),
                    false,false,true,false,null,null,Map.of("4",5));
            when(catalogo.buscarPrendaPublicada(2L)).thenReturn(Optional.of(niquel));

            var ex = assertThrows(co.edu.uniquindio.littlestyle.shared.exception.BusinessException.class,
                    () -> motor().recomendarParaPrenda(pb(Set.of(AlergiaTextil.NIQUEL)), 2L));
            assertEquals(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        }

        // Si la prenda no está publicada o no existe, responde 404
        @Test void porPrendaInexistenteLanza404() {
            when(catalogo.buscarPrendaPublicada(99L)).thenReturn(Optional.empty());

            var ex = assertThrows(co.edu.uniquindio.littlestyle.shared.exception.BusinessException.class,
                    () -> motor().recomendarParaPrenda(pb(Set.of()), 99L));
            assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatus());
        }

        // Una prenda sin tabla de tallas no puede recomendar: 422 controlado, no 500
        @Test void porPrendaSinTablaLanza422EnVezDe500() {
            var sinTabla = prenda(5L, List.of(), List.of(), false, Map.of());
            when(catalogo.buscarPrendaPublicada(5L)).thenReturn(Optional.of(sinTabla));

            var ex = assertThrows(co.edu.uniquindio.littlestyle.shared.exception.BusinessException.class,
                    () -> motor().recomendarParaPrenda(pb(Set.of()), 5L));
            assertEquals(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        }

        // El fallo del catálogo en el endpoint por prenda se traduce a 503
        @Test void porPrendaConCatalogoCaidoLanza503EnVezDe500() {
            when(catalogo.buscarPrendaPublicada(1L)).thenThrow(new RuntimeException("caido"));

            var ex = assertThrows(co.edu.uniquindio.littlestyle.shared.exception.BusinessException.class,
                    () -> motor().recomendarParaPrenda(pb(Set.of()), 1L));
            assertEquals(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
        }

        // Caso extremo con talla ajustada al cuerpo: el puntaje queda siempre en el rango 0-100
        @Test void puntajeNuncaSupera100() {
            var exacta = prenda(1L, List.of(r("4",104,106,17,19)),
                    List.of(new ComponenteMaterial(MaterialTextil.ALGODON,100)), false, Map.of("4",5));
            when(catalogo.buscarPrendaPublicada(1L)).thenReturn(Optional.of(exacta));

            var res = motor().recomendarParaPrenda(pb(Set.of()), 1L);

            assertTrue(res.puntaje() >= 0 && res.puntaje() <= 100);
        }

        // La alergia "OTRA" con texto descriptivo no excluye la prenda, pero sí marca advertencia
        // incluyendo el texto que escribió el padre
        @Test void otraAlergiaConTextoAdvierteAunqueHayaComposicion() {
            var conComposicion = prenda(1L, List.of(r("4",100,110,15,20)),
                    List.of(new ComponenteMaterial(MaterialTextil.ALGODON,100)), false, Map.of("4",5));
            when(catalogo.listarPrendasPublicadas()).thenReturn(List.of(conComposicion));
            var perfil = new PerfilBiometrico(bio(), Set.of(AlergiaTextil.OTRA),
                    "picazón con encajes", Set.of(), Set.of());

            var res = motor().recomendar(perfil);

            assertEquals(1, res.size());
            assertEquals(NivelRiesgo.ADVERTENCIA, res.get(0).riesgo());
            assertTrue(res.get(0).advertencias().stream().anyMatch(m -> m.contains("picazón con encajes")));
        }

        // "OTRA" sin descripción no tiene con qué advertir, así que la prenda queda apta
        @Test void otraAlergiaSinTextoNoAdvierte() {
            var conComposicion = prenda(1L, List.of(r("4",100,110,15,20)),
                    List.of(new ComponenteMaterial(MaterialTextil.ALGODON,100)), false, Map.of("4",5));
            when(catalogo.listarPrendasPublicadas()).thenReturn(List.of(conComposicion));
            var perfil = new PerfilBiometrico(bio(), Set.of(AlergiaTextil.OTRA), "  ", Set.of(), Set.of());

            var res = motor().recomendar(perfil);

            assertEquals(1, res.size());
            assertEquals(NivelRiesgo.APTA, res.get(0).riesgo());
        }

        // Las preferencias de color y estampado suben la afinidad sin tocar el puntaje base
        @Test void preferenciasDeColorYEstampadoSubenElPuntaje() {
            var aJuego = new PrendaRecomendable(1L,"P1","C","M",BigDecimal.TEN,null,
                    new TablaTallas(List.of(r("4",100,110,15,20))),
                    List.of(new ComponenteMaterial(MaterialTextil.ALGODON,100)),
                    false,false,false,false,Set.of(ColorPreferido.AZUL),Estampado.LISO,Map.of("4",5));
            var neutra = new PrendaRecomendable(2L,"P2","C","M",BigDecimal.TEN,null,
                    new TablaTallas(List.of(r("4",100,110,15,20))),
                    List.of(new ComponenteMaterial(MaterialTextil.ALGODON,100)),
                    false,false,false,false,Set.of(ColorPreferido.ROJO),Estampado.RAYAS,Map.of("4",5));
            when(catalogo.listarPrendasPublicadas()).thenReturn(List.of(neutra, aJuego));
            var bioCercana = new DatosBiometricos(new BigDecimal("108"), new BigDecimal("19"),
                    LocalDate.now(), Holgura.REGULAR, Contextura.MEDIA);
            var perfil = new PerfilBiometrico(bioCercana, Set.of(), null,
                    Set.of(ColorPreferido.AZUL), Set.of(Estampado.LISO));

            var res = motor().recomendar(perfil);

            // La afinidad solo reordena: el puntaje (talla+seguridad) no se contamina.
            assertEquals(2, res.size());
            assertEquals(res.get(0).puntaje(), res.get(1).puntaje());
            assertEquals(1L, res.get(0).prendaId());
            assertEquals(10, res.get(0).afinidad());
            assertEquals(0, res.get(1).afinidad());
        }

        // Que la prenda combine con los gustos no salva a la que trae material alergénico:
        // la neutra apta siempre gana en el orden
        @Test void afinidadNuncaCompensaUnaAdvertencia() {
            var advertida = new PrendaRecomendable(1L,"P1","C","M",BigDecimal.TEN,null,
                    new TablaTallas(List.of(r("4",100,110,15,20))),
                    List.of(new ComponenteMaterial(MaterialTextil.LANA,100)),
                    false,false,false,false,Set.of(ColorPreferido.AZUL),Estampado.LISO,Map.of("4",5));
            var neutra = new PrendaRecomendable(2L,"P2","C","M",BigDecimal.TEN,null,
                    new TablaTallas(List.of(r("4",100,110,15,20))),
                    List.of(new ComponenteMaterial(MaterialTextil.ALGODON,100)),
                    false,false,false,false,Set.of(ColorPreferido.ROJO),Estampado.RAYAS,Map.of("4",5));
            when(catalogo.listarPrendasPublicadas()).thenReturn(List.of(advertida, neutra));
            var perfil = new PerfilBiometrico(bio(), Set.of(AlergiaTextil.LANA), null,
                    Set.of(ColorPreferido.AZUL), Set.of(Estampado.LISO));

            var res = motor().recomendar(perfil);

            assertEquals(2, res.size());
            assertEquals(2L, res.get(0).prendaId());
            assertTrue(res.get(0).puntaje() > res.get(1).puntaje());
        }

        // Listar corta en el máximo (24), pero /ids devuelve todos los que pasaron el filtro
        @Test void respetaMaxResultadosPeroIdsNoTieneTope() {
            List<PrendaRecomendable> muchas = new ArrayList<>();
            for (long i = 1; i <= 30; i++) {
                muchas.add(prenda(i, List.of(r("4",100,110,15,20)),
                        List.of(new ComponenteMaterial(MaterialTextil.ALGODON,100)), false, Map.of("4",5)));
            }
            when(catalogo.listarPrendasPublicadas()).thenReturn(muchas);
            var perfil = pb(Set.of());

            assertEquals(24, motor().recomendar(perfil).size());
            assertEquals(30, motor().idsRecomendados(perfil).size());
        }

        // Perfil sin medición guardada: mejor vacío que un error inesperado
        @Test void bioNuloDevuelveVacioEnVezDe500() {
            var buena = prenda(1L, List.of(r("4",100,110,15,20)),
                    List.of(new ComponenteMaterial(MaterialTextil.ALGODON,100)), false, Map.of("4",1));
            when(catalogo.listarPrendasPublicadas()).thenReturn(List.of(buena));

            assertTrue(motor().recomendar(new PerfilBiometrico(null, Set.of(), null, Set.of(), Set.of())).isEmpty());
        }

        // La estrategia se puede fijar por categoría en la configuración: los vestidos piden "exacta"
        @Test void estrategiaSeConfiguraPorCategoria() {
            var holgura = new EstrategiaConHolgura(new EstrategiaExacta(), new EstrategiaProximidad());
            var props = new RecomendacionProperties(6, 24, null, Map.of("VESTIDOS", "exacta"), null);
            var motor = new MotorRecomendacionService(catalogo, holgura,
                    List.of(new EstrategiaExacta(), new EstrategiaProximidad(), holgura),
                    new FiltroAlergiaEstandar(), props);
            var vestido = new PrendaRecomendable(9L,"V","VESTIDOS","M",BigDecimal.TEN,null,
                    new TablaTallas(List.of(r("4",100,110,15,20))), List.of(),
                    false,false,false,false,null,null,Map.of("4",5));

            assertEquals("EXACTA", motor.elegirEstrategia(vestido).nombre());
        }

        // Un nombre de estrategia mal escrito en la config degrada a la por defecto (holgura)
        @Test void estrategiaDesconocidaUsaLaPorDefectoSinRomper() {            var holgura = new EstrategiaConHolgura(new EstrategiaExacta(), new EstrategiaProximidad());
            var props = new RecomendacionProperties(6, 24, null, Map.of("VESTIDOS", "inexistente"), null);
            var motor = new MotorRecomendacionService(catalogo, holgura,
                    List.of(new EstrategiaExacta(), new EstrategiaProximidad(), holgura),
                    new FiltroAlergiaEstandar(), props);
            var vestido = new PrendaRecomendable(9L,"V","VESTIDOS","M",BigDecimal.TEN,null,
                    new TablaTallas(List.of(r("4",100,110,15,20))), List.of(),
                    false,false,false,false,null,null,Map.of("4",5));

            assertEquals("HOLGURA", motor.elegirEstrategia(vestido).nombre());
        }

        // Si hay configuración por marca y por categoría, gana la de marca (más específica)
        @Test void estrategiaPorMarcaMandaSobreCategoria() {
            var holgura = new EstrategiaConHolgura(new EstrategiaExacta(), new EstrategiaProximidad());
            var props = new RecomendacionProperties(6, 24, null,
                    Map.of("VESTIDOS", "proximidad"), Map.of("TIENDA SOL", "exacta"));
            var motor = new MotorRecomendacionService(catalogo, holgura,
                    List.of(new EstrategiaExacta(), new EstrategiaProximidad(), holgura),
                    new FiltroAlergiaEstandar(), props);
            var vestido = new PrendaRecomendable(9L,"V","VESTIDOS","Tienda Sol",BigDecimal.TEN,null,
                    new TablaTallas(List.of(r("4",100,110,15,20))), List.of(),
                    false,false,false,false,null,null,Map.of("4",5));

            assertEquals("EXACTA", motor.elegirEstrategia(vestido).nombre());
        }

        @Test void contexturaDistintaNoCambiaElPuntaje() {
            var prenda = new PrendaRecomendable(1L,"P1","C","M",BigDecimal.TEN,null,
                    new TablaTallas(List.of(r("4",100,110,15,20))),
                    List.of(new ComponenteMaterial(MaterialTextil.ALGODON,100)),
                    false,false,false,false,Set.of(ColorPreferido.AZUL),Estampado.LISO,Map.of("4",5));
            var bioMedia = new DatosBiometricos(new BigDecimal("105"), new BigDecimal("18"),
                    LocalDate.now(), Holgura.REGULAR, Contextura.MEDIA);
            var bioRobusta = new DatosBiometricos(new BigDecimal("105"), new BigDecimal("18"),
                    LocalDate.now(), Holgura.REGULAR, Contextura.ROBUSTA);
            var perfilMedia = new PerfilBiometrico(bioMedia, Set.of(), null,
                    Set.of(ColorPreferido.AZUL), Set.of(Estampado.LISO));
            var perfilRobusta = new PerfilBiometrico(bioRobusta, Set.of(), null,
                    Set.of(ColorPreferido.AZUL), Set.of(Estampado.LISO));

            // La contextura se captura en el perfil pero no puntúa.
            assertEquals(MotorRecomendacionService.afinidad(perfilMedia, prenda),
                    MotorRecomendacionService.afinidad(perfilRobusta, prenda));
        }
}
