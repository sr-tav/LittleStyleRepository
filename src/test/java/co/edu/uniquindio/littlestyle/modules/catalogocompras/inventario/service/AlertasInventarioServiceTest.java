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
import java.util.Optional;

import static co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures.VENDEDOR_ID;
import static co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures.prenda;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertasInventarioServiceTest {

    @Mock
    private AlertaReabastecimientoRepository alertaRepository;
    @Mock
    private ApplicationEventPublisher eventos;
    @InjectMocks
    private AlertasInventarioService service;

    @BeforeEach
    void guardarAsignaId() {
        lenient().when(alertaRepository.save(any())).thenAnswer(inv -> {
            AlertaReabastecimiento alerta = inv.getArgument(0);
            alerta.setId(900L);
            return alerta;
        });
    }

    private static AlertaReabastecimiento activa(Prenda prenda, NivelStock nivel) {
        return AlertaReabastecimiento.builder().id(1L).prenda(prenda).nivel(nivel)
                .stockAlEmitir(2).stockMinimoAlEmitir(5).fechaEmision(LocalDateTime.now().minusDays(1)).build();
    }

    @Test
    void emiteAlertaDeStockBajoYPublicaElEventoAlAlcanzarElMinimo() {
        Prenda p = prenda(10L, 5, 3, 2);
        when(alertaRepository.findFirstByPrendaIdAndFechaResolucionIsNull(10L)).thenReturn(Optional.empty());

        Optional<AlertaReabastecimiento> emitida = service.evaluar(p);

        assertThat(emitida).isPresent();
        assertThat(emitida.get().getNivel()).isEqualTo(NivelStock.STOCK_BAJO);
        assertThat(emitida.get().getStockAlEmitir()).isEqualTo(5);
        assertThat(emitida.get().getStockMinimoAlEmitir()).isEqualTo(5);
        ArgumentCaptor<AlertaReabastecimientoEmitida> evento = ArgumentCaptor.forClass(AlertaReabastecimientoEmitida.class);
        verify(eventos).publishEvent(evento.capture());
        assertThat(evento.getValue().vendedorId()).isEqualTo(VENDEDOR_ID);
        assertThat(evento.getValue().nivel()).isEqualTo(NivelStock.STOCK_BAJO);
        assertThat(evento.getValue().alertaId()).isEqualTo(900L);
    }

    @Test
    void noRepiteLaAlertaMientrasElNivelNoCambie() {
        Prenda p = prenda(10L, 5, 1, 1);
        when(alertaRepository.findFirstByPrendaIdAndFechaResolucionIsNull(10L))
                .thenReturn(Optional.of(activa(p, NivelStock.STOCK_BAJO)));

        assertThat(service.evaluar(p)).isEmpty();
        verify(alertaRepository, never()).save(any());
        verify(eventos, never()).publishEvent(any());
    }

    @Test
    void escalaAAgotadoResolviendoLaAlertaAnteriorYNotificando() {
        Prenda p = prenda(10L, 5, 0, 0);
        AlertaReabastecimiento previa = activa(p, NivelStock.STOCK_BAJO);
        when(alertaRepository.findFirstByPrendaIdAndFechaResolucionIsNull(10L)).thenReturn(Optional.of(previa));

        Optional<AlertaReabastecimiento> emitida = service.evaluar(p);

        assertThat(previa.estaActiva()).isFalse();
        assertThat(emitida).get().extracting(AlertaReabastecimiento::getNivel).isEqualTo(NivelStock.AGOTADO);
        verify(eventos).publishEvent(any(AlertaReabastecimientoEmitida.class));
    }

    @Test
    void reabastecerParcialmenteReemplazaLaAlertaSinVolverANotificar() {
        Prenda p = prenda(10L, 5, 2, 0);
        AlertaReabastecimiento previa = activa(p, NivelStock.AGOTADO);
        when(alertaRepository.findFirstByPrendaIdAndFechaResolucionIsNull(10L)).thenReturn(Optional.of(previa));

        assertThat(service.evaluar(p)).isEmpty();

        assertThat(previa.estaActiva()).isFalse();
        ArgumentCaptor<AlertaReabastecimiento> nueva = ArgumentCaptor.forClass(AlertaReabastecimiento.class);
        verify(alertaRepository).save(nueva.capture());
        assertThat(nueva.getValue().getNivel()).isEqualTo(NivelStock.STOCK_BAJO);
        verify(eventos, never()).publishEvent(any());
    }

    @Test
    void superarElMinimoResuelveLaAlertaActiva() {
        Prenda p = prenda(10L, 5, 4, 4);
        AlertaReabastecimiento previa = activa(p, NivelStock.STOCK_BAJO);
        when(alertaRepository.findFirstByPrendaIdAndFechaResolucionIsNull(10L)).thenReturn(Optional.of(previa));

        assertThat(service.evaluar(p)).isEmpty();

        assertThat(previa.getFechaResolucion()).isNotNull();
        verify(alertaRepository, never()).save(any());
    }

    @Test
    void unaPrendaInactivaNoGeneraAlertasYResuelveLaActiva() {
        Prenda p = prenda(10L, 5, 0, 0);
        p.setEstado(EstadoPrenda.INACTIVA);
        AlertaReabastecimiento previa = activa(p, NivelStock.AGOTADO);
        when(alertaRepository.findFirstByPrendaIdAndFechaResolucionIsNull(10L)).thenReturn(Optional.of(previa));

        assertThat(service.evaluar(p)).isEmpty();

        assertThat(previa.estaActiva()).isFalse();
        verify(alertaRepository, never()).save(any());
        verify(eventos, never()).publishEvent(any());
    }
}
