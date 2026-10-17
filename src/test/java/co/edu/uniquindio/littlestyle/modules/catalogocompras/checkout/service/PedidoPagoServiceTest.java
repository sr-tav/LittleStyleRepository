package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.config.CheckoutProperties;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.TallaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoEstadoCambiadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoPagadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.HistorialEstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.LineaPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.HistorialEstadoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.service.AlertasInventarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoPagoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private HistorialEstadoRepository historialRepository;
    @Mock
    private PrendaRepository prendaRepository;
    @Mock
    private AlertasInventarioService alertasInventarioService;
    @Mock
    private ApplicationEventPublisher eventos;
    @Mock
    private DespachoService despachoService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-10T10:00:00Z"), ZoneId.of("UTC"));
    private final CheckoutProperties props = new CheckoutProperties(
            new BigDecimal("7000"), new BigDecimal("10000"), new BigDecimal("15000"),
            List.of(), Duration.ofMinutes(30));

    private PedidoPagoService service;

    @org.junit.jupiter.api.BeforeEach
    void armar() {
        service = new PedidoPagoService(pedidoRepository, historialRepository, prendaRepository,
                alertasInventarioService, props, clock, eventos, despachoService);
    }

    private Pedido pedidoPendiente() {
        Pedido pedido = Pedido.builder().id(7L).estado(EstadoPedido.PENDIENTE_PAGO)
                .fechaCreacion(LocalDateTime.now(clock)).build();
        pedido.agregar(LineaPedido.builder().prendaId(20L).nombre("Pantalón").talla("4")
                .cantidad(2).precioUnitario(new BigDecimal("100")).build());
        return pedido;
    }

    private Prenda prendaConStock(int stock) {
        Prenda prenda = Prenda.builder().id(20L).build();
        TallaPrenda talla = TallaPrenda.builder().talla("4").stock(stock).prenda(prenda).build();
        prenda.getTallas().add(talla);
        return prenda;
    }

    @Test
    void confirmaPagoRegistraYPublicaUnSoloCorreoDePago() {
        Pedido pedido = pedidoPendiente();
        Prenda prenda = prendaConStock(5);
        when(pedidoRepository.bloquearPorId(7L)).thenReturn(Optional.of(pedido));
        when(prendaRepository.bloquearPorId(20L)).thenReturn(Optional.of(prenda));

        var resultado = service.confirmarPago(7L);

        assertThat(resultado.estado()).isEqualTo(ResultadoConfirmacionPago.Estado.CONFIRMADO);
        // PAGADO es transitorio: queda REGISTRADO en la misma transacción.
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.REGISTRADO);
        assertThat(prenda.getTallas().getFirst().getStock()).isEqualTo(3);
        ArgumentCaptor<HistorialEstadoPedido> historial =
                ArgumentCaptor.forClass(HistorialEstadoPedido.class);
        verify(historialRepository, org.mockito.Mockito.times(2)).save(historial.capture());
        assertThat(historial.getAllValues()).extracting(HistorialEstadoPedido::getNuevo)
                .containsExactly(EstadoPedido.PAGADO, EstadoPedido.REGISTRADO);
        // Un solo correo de pago: el genérico de PAGADO no se publica; el de
        // REGISTRADO sí, porque ya tiene su propio correo de pago confirmado.
        verify(eventos).publishEvent(new PedidoPagadoEvent(7L));
        verify(eventos, org.mockito.Mockito.times(1))
                .publishEvent(any(PedidoEstadoCambiadoEvent.class));
        verify(eventos).publishEvent(new PedidoEstadoCambiadoEvent(7L, EstadoPedido.REGISTRADO));
    }

    @Test
    void confirmaPagoPublicaRegistradoParaElCorreoDeEstado() {
        Pedido pedido = pedidoPendiente();
        when(pedidoRepository.bloquearPorId(7L)).thenReturn(Optional.of(pedido));
        when(prendaRepository.bloquearPorId(20L)).thenReturn(Optional.of(prendaConStock(5)));

        service.confirmarPago(7L);

        verify(eventos).publishEvent(new PedidoEstadoCambiadoEvent(7L, EstadoPedido.REGISTRADO));
    }
}
