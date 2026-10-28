package co.edu.uniquindio.littlestyle.modules.dashboard.service;

import co.edu.uniquindio.littlestyle.modules.dashboard.model.Busqueda;
import co.edu.uniquindio.littlestyle.modules.dashboard.model.ProductoVista;
import co.edu.uniquindio.littlestyle.modules.dashboard.repository.BusquedaRepository;
import co.edu.uniquindio.littlestyle.modules.dashboard.repository.ProductoVistaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** El registro de métricas normaliza lo que guarda y nunca propaga errores al catálogo. */
@ExtendWith(MockitoExtension.class)
class TrackingAnalyticsServiceTest {

    @Mock
    private BusquedaRepository busquedaRepository;

    @Mock
    private ProductoVistaRepository productoVistaRepository;

    @InjectMocks
    private TrackingAnalyticsService tracking;

    @Test
    void guardaElTerminoEnMinusculasYConEspaciosColapsados() {
        tracking.registrarBusqueda("  Vestido   ROSA ", 12);

        ArgumentCaptor<Busqueda> captor = ArgumentCaptor.forClass(Busqueda.class);
        verify(busquedaRepository).save(captor.capture());
        assertThat(captor.getValue().getTermino()).isEqualTo("vestido rosa");
        assertThat(captor.getValue().getTotalResultados()).isEqualTo(12);
        assertThat(captor.getValue().getFecha()).isNotNull();
    }

    @Test
    void ignoraTerminosVaciosOMuyCortos() {
        tracking.registrarBusqueda(null, 0);
        tracking.registrarBusqueda("   ", 0);
        tracking.registrarBusqueda("a", 0);

        verifyNoInteractions(busquedaRepository);
    }

    @Test
    void recortaTerminosLargos() {
        assertThat(TrackingAnalyticsService.normalizar("x".repeat(300))).hasSize(120);
    }

    @Test
    void registraLaVistaConLaPrendaYLaFecha() {
        tracking.registrarVista(15L);

        ArgumentCaptor<ProductoVista> captor = ArgumentCaptor.forClass(ProductoVista.class);
        verify(productoVistaRepository).save(captor.capture());
        assertThat(captor.getValue().getPrendaId()).isEqualTo(15L);
        assertThat(captor.getValue().getFecha()).isNotNull();
    }

    @Test
    void unFalloDeLaBaseNoSePropagaAlCliente() {
        when(busquedaRepository.save(any())).thenThrow(new IllegalStateException("BD caída"));
        when(productoVistaRepository.save(any())).thenThrow(new IllegalStateException("BD caída"));

        assertThatCode(() -> tracking.registrarBusqueda("vestido", 1)).doesNotThrowAnyException();
        assertThatCode(() -> tracking.registrarVista(1L)).doesNotThrowAnyException();
    }
}
