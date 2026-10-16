package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoEstadoCambiadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.HistorialEstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.LineaPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.DespachoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.HistorialEstadoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.service.AlertasInventarioService;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.util.List;
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
    private PrendaRepository prendaRepository;
    @Mock
    private AlertasInventarioService alertasInventarioService;
    @Mock
    private DespachoRepository despachoRepository;
    @Mock
    private ApplicationEventPublisher eventos;

    @InjectMocks
    private PedidoEstadoService service;

    private static Pedido pedido(EstadoPedido estado) {
        return Pedido.builder().id(7L).estado(estado).build();
    }

    @Test
    void cancelarGuardaHistorialYPublicaEvento() {
        Pedido pedido = pedido(EstadoPedido.REGISTRADO);
        when(pedidoRepository.bloquearPorId(7L)).thenReturn(Optional.of(pedido));
        when(despachoRepository.findAllByPedidoIdOrderByIdAsc(7L)).thenReturn(List.of());

        service.avanzar(7L, EstadoPedido.CANCELADO, null);

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CANCELADO);
        ArgumentCaptor<HistorialEstadoPedido> historial = ArgumentCaptor.forClass(HistorialEstadoPedido.class);
        verify(historialRepository).save(historial.capture());
        assertThat(historial.getValue().getAnterior()).isEqualTo(EstadoPedido.REGISTRADO);
        assertThat(historial.getValue().getNuevo()).isEqualTo(EstadoPedido.CANCELADO);
        verify(eventos).publishEvent(new PedidoEstadoCambiadoEvent(7L, EstadoPedido.CANCELADO));
    }

    @Test
    void avanceManualDistintoDeCancelarDevuelve409() {
        assertThatThrownBy(() -> service.avanzar(7L, EstadoPedido.EN_PREPARACION, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThatThrownBy(() -> service.avanzar(7L, EstadoPedido.ENVIADO, "COL123"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);
        verify(pedidoRepository, never()).bloquearPorId(any());
    }

    @Test
    void transicionInvalidaDevuelve409() {
        when(pedidoRepository.bloquearPorId(7L)).thenReturn(Optional.of(pedido(EstadoPedido.PENDIENTE_PAGO)));
        when(despachoRepository.findAllByPedidoIdOrderByIdAsc(7L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.avanzar(7L, EstadoPedido.CANCELADO, "COL123"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void pedidoInexistenteDevuelve404() {
        when(pedidoRepository.bloquearPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.avanzar(99L, EstadoPedido.CANCELADO, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void marcarPagadoAManoDevuelve409() {
        // PAGADO solo lo pone el pago con descuento de stock; a mano sería
        // vender sin cobrar. Con despachos, el avance manual ni siquiera existe.
        assertThatThrownBy(() -> service.avanzar(7L, EstadoPedido.PAGADO, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);
        verify(pedidoRepository, never()).bloquearPorId(any());
    }

    @Test
    void noSePuedeCancelarPedidoParcialmenteEnviado() {
        Pedido pedido = pedido(EstadoPedido.PARCIALMENTE_ENVIADO);
        when(pedidoRepository.bloquearPorId(7L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> service.avanzar(7L, EstadoPedido.CANCELADO, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void cancelarConPagoPrevioReponeStock() {
        Pedido pedido = pedido(EstadoPedido.EN_PREPARACION);
        pedido.agregar(LineaPedido.builder().prendaId(20L).nombre("Pantalón").talla("4")
                .cantidad(2).precioUnitario(new java.math.BigDecimal("100")).build());
        var prenda = co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda.builder()
                .id(20L).build();
        var talla = co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.TallaPrenda.builder()
                .talla("4").stock(5).prenda(prenda).build();
        prenda.getTallas().add(talla);
        when(pedidoRepository.bloquearPorId(7L)).thenReturn(Optional.of(pedido));
        when(despachoRepository.findAllByPedidoIdOrderByIdAsc(7L)).thenReturn(List.of());
        when(prendaRepository.bloquearPorId(20L)).thenReturn(Optional.of(prenda));

        service.avanzar(7L, EstadoPedido.CANCELADO, null);

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CANCELADO);
        assertThat(talla.getStock()).isEqualTo(7);
    }

    @Test
    void reponerStockFallaLaTransaccionSiFaltaLaPrenda() {
        Pedido pedido = pedido(EstadoPedido.EN_PREPARACION);
        pedido.agregar(LineaPedido.builder().prendaId(20L).nombre("Pantalón").talla("4")
                .cantidad(2).precioUnitario(new java.math.BigDecimal("100")).build());
        when(pedidoRepository.bloquearPorId(7L)).thenReturn(Optional.of(pedido));
        when(despachoRepository.findAllByPedidoIdOrderByIdAsc(7L)).thenReturn(List.of());
        when(prendaRepository.bloquearPorId(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.avanzar(7L, EstadoPedido.CANCELADO, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CANCELADO);
    }

    @Test
    void clienteSoloCancelaAntesDelPago() {
        Pedido pendiente = pedido(EstadoPedido.PENDIENTE_PAGO);
        when(pedidoRepository.findByIdAndClienteId(7L, 1L)).thenReturn(Optional.of(pendiente));
        when(pedidoRepository.bloquearPorId(7L)).thenReturn(Optional.of(pendiente));
        when(despachoRepository.findAllByPedidoIdOrderByIdAsc(7L)).thenReturn(List.of());

        service.cancelarComoCliente(1L, 7L);

        assertThat(pendiente.getEstado()).isEqualTo(EstadoPedido.CANCELADO);
        assertThatThrownBy(() -> service.cancelarComoCliente(1L, 8L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void clienteNoCancelaPedidoPagado() {
        Pedido pagado = pedido(EstadoPedido.PAGADO);
        when(pedidoRepository.findByIdAndClienteId(7L, 1L)).thenReturn(Optional.of(pagado));

        assertThatThrownBy(() -> service.cancelarComoCliente(1L, 7L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }
}
