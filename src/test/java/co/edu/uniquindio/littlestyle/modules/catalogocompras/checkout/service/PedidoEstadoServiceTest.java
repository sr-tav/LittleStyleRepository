package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoEnviadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoEstadoCambiadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.HistorialEstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.HistorialEstadoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoEstadoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private HistorialEstadoRepository historialRepository;
    @Mock
    private ApplicationEventPublisher eventos;

    @InjectMocks
    private PedidoEstadoService service;

    private static Pedido pedido(EstadoPedido estado) {
        return Pedido.builder().id(7L).estado(estado).build();
    }

    @Test
    void avanzarGuardaHistorialYPublicaEventoAlEnviar() {
        Pedido pedido = pedido(EstadoPedido.EN_PREPARACION);
        when(pedidoRepository.findById(7L)).thenReturn(Optional.of(pedido));

        service.avanzar(7L, EstadoPedido.ENVIADO, "COL123");

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.ENVIADO);
        assertThat(pedido.getNumeroGuia()).isEqualTo("COL123");
        ArgumentCaptor<HistorialEstadoPedido> historial = ArgumentCaptor.forClass(HistorialEstadoPedido.class);
        verify(historialRepository).save(historial.capture());
        assertThat(historial.getValue().getAnterior()).isEqualTo(EstadoPedido.EN_PREPARACION);
        assertThat(historial.getValue().getNuevo()).isEqualTo(EstadoPedido.ENVIADO);
        assertThat(historial.getValue().getNumeroGuia()).isEqualTo("COL123");
        verify(eventos).publishEvent(new PedidoEnviadoEvent(7L));
        verify(eventos).publishEvent(new PedidoEstadoCambiadoEvent(7L, EstadoPedido.ENVIADO));
    }

    @Test
    void avanzarSinEnviarPublicaCambioEstadoPeroNoEventoEnviado() {
        Pedido pedido = pedido(EstadoPedido.PAGADO);
        when(pedidoRepository.findById(7L)).thenReturn(Optional.of(pedido));

        service.avanzar(7L, EstadoPedido.EN_PREPARACION, null);

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.EN_PREPARACION);
        verify(eventos).publishEvent(new PedidoEstadoCambiadoEvent(7L, EstadoPedido.EN_PREPARACION));
        verify(eventos, never()).publishEvent(any(PedidoEnviadoEvent.class));
    }

    @Test
    void enviarSinGuiaDevuelve400() {
        when(pedidoRepository.findById(7L)).thenReturn(Optional.of(pedido(EstadoPedido.EN_PREPARACION)));

        assertThatThrownBy(() -> service.avanzar(7L, EstadoPedido.ENVIADO, "  "))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void transicionInvalidaDevuelve409() {
        when(pedidoRepository.findById(7L)).thenReturn(Optional.of(pedido(EstadoPedido.PENDIENTE_PAGO)));

        assertThatThrownBy(() -> service.avanzar(7L, EstadoPedido.ENVIADO, "COL123"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void pedidoInexistenteDevuelve404() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.avanzar(99L, EstadoPedido.CANCELADO, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
