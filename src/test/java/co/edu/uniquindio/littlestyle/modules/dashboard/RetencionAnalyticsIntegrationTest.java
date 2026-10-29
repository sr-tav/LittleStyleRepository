package co.edu.uniquindio.littlestyle.modules.dashboard;

import co.edu.uniquindio.littlestyle.modules.dashboard.model.Busqueda;
import co.edu.uniquindio.littlestyle.modules.dashboard.model.ProductoVista;
import co.edu.uniquindio.littlestyle.modules.dashboard.repository.BusquedaRepository;
import co.edu.uniquindio.littlestyle.modules.dashboard.repository.ProductoVistaRepository;
import co.edu.uniquindio.littlestyle.modules.dashboard.service.RetencionAnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** La depuración contra H2: los DELETE en JPQL funcionan de verdad y respetan el límite exacto. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RetencionAnalyticsIntegrationTest {

    @Autowired private RetencionAnalyticsService retencion;
    @Autowired private BusquedaRepository busquedaRepository;
    @Autowired private ProductoVistaRepository productoVistaRepository;

    @Test
    void borraSoloLoAnteriorAlLimiteEnAmbasTablas() {
        LocalDateTime limite = LocalDateTime.now().minusMonths(12);

        Busqueda busquedaVieja = busqueda("vestido viejo", limite.minusDays(1));
        Busqueda busquedaEnElLimite = busqueda("vestido en el limite", limite);
        Busqueda busquedaReciente = busqueda("vestido reciente", LocalDateTime.now().minusMonths(2));
        ProductoVista vistaVieja = vista(1L, limite.minusMonths(6));
        ProductoVista vistaEnElLimite = vista(2L, limite);
        ProductoVista vistaReciente = vista(3L, LocalDateTime.now());

        RetencionAnalyticsService.Resultado resultado = retencion.purgarAnterioresA(limite);

        assertThat(resultado.busquedas()).isEqualTo(1);
        assertThat(resultado.vistas()).isEqualTo(1);
        assertThat(busquedaRepository.existsById(busquedaVieja.getId())).isFalse();
        assertThat(busquedaRepository.existsById(busquedaEnElLimite.getId())).isTrue();
        assertThat(busquedaRepository.existsById(busquedaReciente.getId())).isTrue();
        assertThat(productoVistaRepository.existsById(vistaVieja.getId())).isFalse();
        assertThat(productoVistaRepository.existsById(vistaEnElLimite.getId())).isTrue();
        assertThat(productoVistaRepository.existsById(vistaReciente.getId())).isTrue();
    }

    @Test
    void sinNadaQueBorrarDevuelveCeros() {
        busqueda("vestido reciente", LocalDateTime.now());

        RetencionAnalyticsService.Resultado resultado = retencion.purgarAnterioresA(LocalDateTime.now().minusYears(5));

        assertThat(resultado.busquedas()).isZero();
        assertThat(resultado.vistas()).isZero();
    }

    private Busqueda busqueda(String termino, LocalDateTime fecha) {
        return busquedaRepository.saveAndFlush(
                Busqueda.builder().termino(termino).totalResultados(3).fecha(fecha).build());
    }

    private ProductoVista vista(Long prendaId, LocalDateTime fecha) {
        return productoVistaRepository.saveAndFlush(
                ProductoVista.builder().prendaId(prendaId).fecha(fecha).build());
    }
}
