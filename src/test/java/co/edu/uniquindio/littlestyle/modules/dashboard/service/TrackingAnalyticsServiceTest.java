package co.edu.uniquindio.littlestyle.modules.dashboard.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.dashboard.model.Busqueda;
import co.edu.uniquindio.littlestyle.modules.dashboard.model.ProductoVista;
import co.edu.uniquindio.littlestyle.modules.dashboard.repository.BusquedaRepository;
import co.edu.uniquindio.littlestyle.modules.dashboard.repository.ProductoVistaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * El registro de métricas solo cuenta demanda real: ignora automatizados, repeticiones y al dueño de la
 * prenda, y nunca propaga errores al catálogo.
 */
@ExtendWith(MockitoExtension.class)
class TrackingAnalyticsServiceTest {

    private static final String NAVEGADOR = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";

    @Mock
    private BusquedaRepository busquedaRepository;

    @Mock
    private ProductoVistaRepository productoVistaRepository;

    @Mock
    private PrendaRepository prendaRepository;

    private TrackingAnalyticsService tracking;

    @BeforeEach
    void setUp() {
        tracking = new TrackingAnalyticsService(busquedaRepository, productoVistaRepository, prendaRepository,
                new VentanaAntiRepeticion(Duration.ofMinutes(30), Clock.systemUTC()));
    }

    // ---- búsquedas ----

    @Test
    void guardaElTerminoEnMinusculasYConEspaciosColapsados() {
        tracking.registrarBusqueda("  Vestido   ROSA ", 12, 5L, NAVEGADOR);

        ArgumentCaptor<Busqueda> captor = ArgumentCaptor.forClass(Busqueda.class);
        verify(busquedaRepository).save(captor.capture());
        assertThat(captor.getValue().getTermino()).isEqualTo("vestido rosa");
        assertThat(captor.getValue().getTotalResultados()).isEqualTo(12);
        assertThat(captor.getValue().getFecha()).isNotNull();
    }

    @Test
    void ignoraTerminosVaciosOMuyCortos() {
        tracking.registrarBusqueda(null, 0, 5L, NAVEGADOR);
        tracking.registrarBusqueda("   ", 0, 5L, NAVEGADOR);
        tracking.registrarBusqueda("a", 0, 5L, NAVEGADOR);

        verifyNoInteractions(busquedaRepository);
    }

    @Test
    void recortaTerminosLargos() {
        assertThat(TrackingAnalyticsService.normalizar("x".repeat(300))).hasSize(120);
    }

    @Test
    void laMismaBusquedaDelMismoClienteSeCuentaUnaVezAunqueCambieElFormato() {
        tracking.registrarBusqueda("Vestido rosa", 4, 5L, NAVEGADOR);
        tracking.registrarBusqueda("  vestido   ROSA", 4, 5L, NAVEGADOR);

        verify(busquedaRepository, times(1)).save(any(Busqueda.class));
    }

    @Test
    void laMismaBusquedaDeOtroClienteSiCuenta() {
        tracking.registrarBusqueda("vestido rosa", 4, 5L, NAVEGADOR);
        tracking.registrarBusqueda("vestido rosa", 4, 6L, NAVEGADOR);

        verify(busquedaRepository, times(2)).save(any(Busqueda.class));
    }

    @Test
    void noCuentaBusquedasDeAutomatizadosNiSinCliente() {
        tracking.registrarBusqueda("vestido", 4, 5L, "Googlebot/2.1 (+http://www.google.com/bot.html)");
        tracking.registrarBusqueda("vestido", 4, null, NAVEGADOR);

        verifyNoInteractions(busquedaRepository);
    }

    // ---- vistas ----

    @Test
    void registraLaVistaConLaPrendaYLaFecha() {
        tracking.registrarVista(15L, 5L, NAVEGADOR);

        ArgumentCaptor<ProductoVista> captor = ArgumentCaptor.forClass(ProductoVista.class);
        verify(productoVistaRepository).save(captor.capture());
        assertThat(captor.getValue().getPrendaId()).isEqualTo(15L);
        assertThat(captor.getValue().getFecha()).isNotNull();
    }

    @Test
    void refrescarLaFichaNoSumaVistasNuevas() {
        tracking.registrarVista(15L, 5L, NAVEGADOR);
        tracking.registrarVista(15L, 5L, NAVEGADOR);
        tracking.registrarVista(15L, 5L, NAVEGADOR);

        verify(productoVistaRepository, times(1)).save(any(ProductoVista.class));
    }

    @Test
    void otroClienteOOtraPrendaSiSuman() {
        tracking.registrarVista(15L, 5L, NAVEGADOR);
        tracking.registrarVista(15L, 6L, NAVEGADOR); // otro cliente
        tracking.registrarVista(16L, 5L, NAVEGADOR); // otra prenda

        verify(productoVistaRepository, times(3)).save(any(ProductoVista.class));
    }

    @Test
    void laVistaDelDuenoDeLaPrendaNoCuenta() {
        when(prendaRepository.existsByIdAndVendedorId(15L, 7L)).thenReturn(true);

        tracking.registrarVista(15L, 7L, NAVEGADOR);

        verify(productoVistaRepository, never()).save(any());
    }

    @Test
    void unVendedorSiCuentaCuandoMiraLaPrendaDeOtro() {
        when(prendaRepository.existsByIdAndVendedorId(15L, 7L)).thenReturn(false);

        tracking.registrarVista(15L, 7L, NAVEGADOR);

        verify(productoVistaRepository).save(any(ProductoVista.class));
    }

    @Test
    void noCuentaVistasDeAutomatizados() {
        tracking.registrarVista(15L, 5L, "Googlebot/2.1");
        tracking.registrarVista(15L, 5L, "curl/8.5.0");
        tracking.registrarVista(15L, 5L, "python-requests/2.31");
        tracking.registrarVista(15L, 5L, "Mozilla/5.0 HeadlessChrome/130.0");
        tracking.registrarVista(15L, 5L, null);
        tracking.registrarVista(15L, 5L, "  ");

        verifyNoInteractions(productoVistaRepository);
    }

    @Test
    void noCuentaVistasSinClienteNiSinPrenda() {
        tracking.registrarVista(15L, null, NAVEGADOR);
        tracking.registrarVista(null, 5L, NAVEGADOR);

        verifyNoInteractions(productoVistaRepository);
    }

    @Test
    void reconoceNavegadoresReales() {
        assertThat(TrackingAnalyticsService.esAutomatizado(NAVEGADOR)).isFalse();
        assertThat(TrackingAnalyticsService.esAutomatizado(
                "Mozilla/5.0 (Linux; Android 10; CUBOT_X30) AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/130.0.0.0 Mobile Safari/537.36")).isFalse();
        assertThat(TrackingAnalyticsService.esAutomatizado(
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 "
                        + "(KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1")).isFalse();
    }

    // ---- errores ----

    @Test
    void unFalloDeLaBaseNoSePropagaAlCliente() {
        when(busquedaRepository.save(any())).thenThrow(new IllegalStateException("BD caída"));
        when(prendaRepository.existsByIdAndVendedorId(any(), any())).thenThrow(new IllegalStateException("BD caída"));

        assertThatCode(() -> tracking.registrarBusqueda("vestido", 1, 5L, NAVEGADOR)).doesNotThrowAnyException();
        assertThatCode(() -> tracking.registrarVista(1L, 5L, NAVEGADOR)).doesNotThrowAnyException();
    }
}
