package co.edu.uniquindio.littlestyle.modules.dashboard.service;

import co.edu.uniquindio.littlestyle.modules.dashboard.repository.BusquedaRepository;
import co.edu.uniquindio.littlestyle.modules.dashboard.repository.ProductoVistaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** La tarea programada calcula bien el límite y un fallo de base de datos no la tumba. */
@ExtendWith(MockitoExtension.class)
class RetencionAnalyticsServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-28T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private BusquedaRepository busquedaRepository;

    @Mock
    private ProductoVistaRepository productoVistaRepository;

    private RetencionAnalyticsService retencion;

    @BeforeEach
    void setUp() {
        retencion = new RetencionAnalyticsService(busquedaRepository, productoVistaRepository, RELOJ, 12);
    }

    @Test
    void laTareaBorraLoAnteriorAMesesDeRetencionEnAmbasTablas() {
        LocalDateTime limite = LocalDateTime.of(2025, 10, 28, 10, 0);

        retencion.purgarProgramado();

        verify(busquedaRepository).eliminarAnterioresA(limite);
        verify(productoVistaRepository).eliminarAnterioresA(limite);
    }

    @Test
    void laRetencionSeConfiguraEnMeses() {
        new RetencionAnalyticsService(busquedaRepository, productoVistaRepository, RELOJ, 6).purgarProgramado();

        verify(busquedaRepository).eliminarAnterioresA(LocalDateTime.of(2026, 4, 28, 10, 0));
    }

    @Test
    void devuelveCuantasFilasSeEliminaron() {
        LocalDateTime limite = LocalDateTime.of(2025, 10, 28, 10, 0);
        when(busquedaRepository.eliminarAnterioresA(limite)).thenReturn(7);
        when(productoVistaRepository.eliminarAnterioresA(limite)).thenReturn(120);

        RetencionAnalyticsService.Resultado resultado = retencion.purgarAnterioresA(limite);

        assertThat(resultado.busquedas()).isEqualTo(7);
        assertThat(resultado.vistas()).isEqualTo(120);
    }

    @Test
    void unFalloDeLaBaseNoTumbaLaTareaProgramada() {
        when(busquedaRepository.eliminarAnterioresA(any())).thenThrow(new IllegalStateException("BD caída"));

        assertThatCode(() -> retencion.purgarProgramado()).doesNotThrowAnyException();
    }

    @Test
    void unaRetencionMenorAUnMesSeRechazaParaNoBorrarTodoElHistorial() {
        assertThatThrownBy(() -> new RetencionAnalyticsService(busquedaRepository, productoVistaRepository, RELOJ, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RetencionAnalyticsService(busquedaRepository, productoVistaRepository, RELOJ, -3))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
