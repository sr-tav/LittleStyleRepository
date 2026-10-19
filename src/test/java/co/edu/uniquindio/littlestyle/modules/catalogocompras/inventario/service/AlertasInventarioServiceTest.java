package co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.event.AlertaReabastecimientoEmitida;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.model.AlertaReabastecimiento;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.model.NivelStock;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.repository.AlertaReabastecimientoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures.VENDEDOR_ID;
import static co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures.prenda;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests del servicio que emite y resuelve alertas de reabastecimiento al
 * evaluar el stock de una prenda talla por talla.
 * Se mockean el repositorio de alertas y el publicador de eventos.
 */
@ExtendWith(MockitoExtension.class)
class AlertasInventarioServiceTest {

    @Mock
    private AlertaReabastecimientoRepository alertaRepository;
    @Mock
    private ApplicationEventPublisher eventos;
    @InjectMocks
    private AlertasInventarioService service;

    // Stub global para todos los tests: al guardar una alerta le asigna un id fijo
    // y por defecto no hay alertas previas sin talla.
    @BeforeEach
    void guardarAsignaId() {
        lenient().when(alertaRepository.save(any())).thenAnswer(inv -> {
            AlertaReabastecimiento alerta = inv.getArgument(0);
            alerta.setId(900L);
            return alerta;
        });
        lenient().when(alertaRepository.findAllByPrendaIdAndTallaIsNullAndFechaResolucionIsNull(any()))
                .thenReturn(List.of());
    }

    // Alerta activa de ejemplo, emitida el día anterior con la talla y el nivel dados.
    private static AlertaReabastecimiento activa(Prenda prenda, String talla, NivelStock nivel) {
        return AlertaReabastecimiento.builder().id(1L).prenda(prenda).talla(talla).nivel(nivel)
                .stockAlEmitir(2).stockMinimoAlEmitir(5).fechaEmision(LocalDateTime.now().minusDays(1)).build();
    }

    // Cuando una talla toca su mínimo, se emite la alerta de stock bajo y se publica
    // el evento para avisar al vendedor.
    @Test
    void emiteAlertaDeStockBajoPorTallaAlAlcanzarElMinimo() {
        Prenda p = prenda(10L, 5, 5, 8);
        when(alertaRepository.findFirstByPrendaIdAndTallaAndFechaResolucionIsNull(10L, "4"))
                .thenReturn(Optional.empty());
        when(alertaRepository.findFirstByPrendaIdAndTallaAndFechaResolucionIsNull(10L, "6"))
                .thenReturn(Optional.empty());

        List<AlertaReabastecimiento> emitidas = service.evaluar(p);

        // Solo la talla 4 (5 <= 5) alerta; la 6 (8 > 5) no.
        assertThat(emitidas).hasSize(1);
        assertThat(emitidas.getFirst().getNivel()).isEqualTo(NivelStock.STOCK_BAJO);
        assertThat(emitidas.getFirst().getTalla()).isEqualTo("4");
        assertThat(emitidas.getFirst().getStockAlEmitir()).isEqualTo(5);
        assertThat(emitidas.getFirst().getStockMinimoAlEmitir()).isEqualTo(5);
        ArgumentCaptor<AlertaReabastecimientoEmitida> evento = ArgumentCaptor.forClass(AlertaReabastecimientoEmitida.class);
        verify(eventos).publishEvent(evento.capture());
        assertThat(evento.getValue().vendedorId()).isEqualTo(VENDEDOR_ID);
        assertThat(evento.getValue().nivel()).isEqualTo(NivelStock.STOCK_BAJO);
        assertThat(evento.getValue().talla()).isEqualTo("4");
        assertThat(evento.getValue().alertaId()).isEqualTo(900L);
    }

    // Una talla sin stock mínimo configurado no genera alertas aunque se agote.
    @Test
    void tallaSinMinimoNuncaAlerta() {
        Prenda p = prenda(10L, 5, 0, 8);
        p.getTallas().stream().filter(t -> t.getTalla().equals("6")).findFirst().orElseThrow()
                .setStockMinimo(null);
        when(alertaRepository.findFirstByPrendaIdAndTallaAndFechaResolucionIsNull(10L, "4"))
                .thenReturn(Optional.empty());

        List<AlertaReabastecimiento> emitidas = service.evaluar(p);

        // La 6 agotada pero sin mínimo no alerta; la 4 (0 <= 5) sí.
        assertThat(emitidas).extracting(AlertaReabastecimiento::getTalla).containsExactly("4");
    }

    // Si ya hay una alerta activa con el mismo nivel, no se emite otra ni se notifica de nuevo.
    @Test
    void noRepiteLaAlertaMientrasElNivelNoCambie() {
        Prenda p = prenda(10L, 5, 1, 8);
        when(alertaRepository.findFirstByPrendaIdAndTallaAndFechaResolucionIsNull(10L, "4"))
                .thenReturn(Optional.of(activa(p, "4", NivelStock.STOCK_BAJO)));
        when(alertaRepository.findFirstByPrendaIdAndTallaAndFechaResolucionIsNull(10L, "6"))
                .thenReturn(Optional.empty());

        assertThat(service.evaluar(p)).isEmpty();
        verify(alertaRepository, never()).save(any());
        verify(eventos, never()).publishEvent(any());
    }

    // Si el stock baja más (de stock bajo a agotado), la alerta previa se resuelve
    // y se emite una nueva de mayor severidad con su notificación.
    @Test
    void escalaAAgotadoResolviendoLaAlertaAnteriorYNotificando() {
        Prenda p = prenda(10L, 5, 0, 8);
        AlertaReabastecimiento previa = activa(p, "4", NivelStock.STOCK_BAJO);
        when(alertaRepository.findFirstByPrendaIdAndTallaAndFechaResolucionIsNull(10L, "4"))
                .thenReturn(Optional.of(previa));
        when(alertaRepository.findFirstByPrendaIdAndTallaAndFechaResolucionIsNull(10L, "6"))
                .thenReturn(Optional.empty());

        List<AlertaReabastecimiento> emitidas = service.evaluar(p);

        assertThat(previa.estaActiva()).isFalse();
        assertThat(emitidas).extracting(AlertaReabastecimiento::getNivel)
                .containsExactly(NivelStock.AGOTADO);
        verify(eventos).publishEvent(any(AlertaReabastecimientoEmitida.class));
    }

    // Con un reabastecimiento parcial, la alerta de agotado cambia a stock bajo:
    // se crea una nueva sin volver a notificar al vendedor.
    @Test
    void reabastecerParcialmenteReemplazaLaAlertaSinVolverANotificar() {
        Prenda p = prenda(10L, 5, 2, 8);
        AlertaReabastecimiento previa = activa(p, "4", NivelStock.AGOTADO);
        when(alertaRepository.findFirstByPrendaIdAndTallaAndFechaResolucionIsNull(10L, "4"))
                .thenReturn(Optional.of(previa));
        when(alertaRepository.findFirstByPrendaIdAndTallaAndFechaResolucionIsNull(10L, "6"))
                .thenReturn(Optional.empty());

        assertThat(service.evaluar(p)).isEmpty();

        assertThat(previa.estaActiva()).isFalse();
        ArgumentCaptor<AlertaReabastecimiento> nueva = ArgumentCaptor.forClass(AlertaReabastecimiento.class);
        verify(alertaRepository).save(nueva.capture());
        assertThat(nueva.getValue().getNivel()).isEqualTo(NivelStock.STOCK_BAJO);
        assertThat(nueva.getValue().getTalla()).isEqualTo("4");
        verify(eventos, never()).publishEvent(any());
    }

    // Cuando el stock vuelve a estar por encima del mínimo, la alerta activa simplemente se resuelve.
    @Test
    void superarElMinimoResuelveLaAlertaActiva() {
        Prenda p = prenda(10L, 5, 9, 9);
        AlertaReabastecimiento previa = activa(p, "4", NivelStock.STOCK_BAJO);
        when(alertaRepository.findFirstByPrendaIdAndTallaAndFechaResolucionIsNull(10L, "4"))
                .thenReturn(Optional.of(previa));
        when(alertaRepository.findFirstByPrendaIdAndTallaAndFechaResolucionIsNull(10L, "6"))
                .thenReturn(Optional.empty());

        assertThat(service.evaluar(p)).isEmpty();

        assertThat(previa.getFechaResolucion()).isNotNull();
        verify(alertaRepository, never()).save(any());
    }

    // Una prenda desactivada no emite alertas nuevas, pero sí resuelve la que tuviera activa.
    @Test
    void unaPrendaInactivaNoGeneraAlertasYResuelveLaActiva() {
        Prenda p = prenda(10L, 5, 0, 0);
        p.setEstado(EstadoPrenda.INACTIVA);
        AlertaReabastecimiento previa = activa(p, "4", NivelStock.AGOTADO);
        when(alertaRepository.findFirstByPrendaIdAndTallaAndFechaResolucionIsNull(10L, "4"))
                .thenReturn(Optional.of(previa));
        when(alertaRepository.findFirstByPrendaIdAndTallaAndFechaResolucionIsNull(10L, "6"))
                .thenReturn(Optional.empty());

        assertThat(service.evaluar(p)).isEmpty();

        assertThat(previa.estaActiva()).isFalse();
        verify(alertaRepository, never()).save(any());
        verify(eventos, never()).publishEvent(any());
    }

    @Test
    void resuelveAlertasHeradadasSinTalla() {
        Prenda p = prenda(10L, 5, 9, 9);
        AlertaReabastecimiento heredada = AlertaReabastecimiento.builder().id(2L).prenda(p)
                .nivel(NivelStock.STOCK_BAJO)
                .stockAlEmitir(3).stockMinimoAlEmitir(5).fechaEmision(LocalDateTime.now().minusDays(1)).build();
        when(alertaRepository.findAllByPrendaIdAndTallaIsNullAndFechaResolucionIsNull(10L))
                .thenReturn(List.of(heredada));
        when(alertaRepository.findFirstByPrendaIdAndTallaAndFechaResolucionIsNull(10L, "4"))
                .thenReturn(Optional.empty());
        when(alertaRepository.findFirstByPrendaIdAndTallaAndFechaResolucionIsNull(10L, "6"))
                .thenReturn(Optional.empty());

        service.evaluar(p);

        assertThat(heredada.estaActiva()).isFalse();
    }
}
