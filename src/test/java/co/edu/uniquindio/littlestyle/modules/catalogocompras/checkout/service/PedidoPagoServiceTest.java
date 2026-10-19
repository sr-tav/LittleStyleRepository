package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

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

/**
 * Tests de la confirmación de pago del pedido: descuento de stock, registro del
 * historial de estados y eventos publicados (incluyendo por qué se publica
 * REGISTRADO y no PAGADO para el correo).
 * Repositorios, alertas y publicador de eventos se mockean; el reloj es fijo.
 */
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

    // Reloj congelado en una fecha fija para poder comparar fechas sin sorpresas.
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-10T10:00:00Z"), ZoneId.of("UTC"));

    private PedidoPagoService service;

    // Se arma el servicio a mano porque el Clock no es un mock, así no se puede usar @InjectMocks directo.
    @org.junit.jupiter.api.BeforeEach
    void armar() {
        service = new PedidoPagoService(pedidoRepository, historialRepository, prendaRepository,
                alertasInventarioService, clock, eventos, despachoService);
    }

    // Pedido en estado PENDIENTE_PAGO con dos unidades de "Pantalón", listo para confirmar el pago.
    private Pedido pedidoPendiente() {
        Pedido pedido = Pedido.builder().id(7L).estado(EstadoPedido.PENDIENTE_PAGO)
                .fechaCreacion(LocalDateTime.now(clock)).build();
        pedido.agregar(LineaPedido.builder().prendaId(20L).nombre("Pantalón").talla("4")
                .cantidad(2).precioUnitario(new BigDecimal("100")).build());
        return pedido;
    }

    // Prenda con una única talla "4" y el stock inicial que se le pase.
    private Prenda prendaConStock(int stock) {
        Prenda prenda = Prenda.builder().id(20L).build();
        TallaPrenda talla = TallaPrenda.builder().talla("4").stock(stock).prenda(prenda).build();
        prenda.getTallas().add(talla);
        return prenda;
    }

    // Confirma el pago de un pedido pendiente: pasa a PAGADO (descuenta stock) y luego queda REGISTRADO,
    // con dos entradas de historial, el evento de pago y un solo correo de cambio de estado.
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

    // Verifica explícitamente que al cliente le llega el correo por el estado REGISTRADO
    // (que es el que tiene su propio correo de pago confirmado) y no por el PAGADO transitorio.
    @Test
    void confirmaPagoPublicaRegistradoParaElCorreoDeEstado() {
        Pedido pedido = pedidoPendiente();
        when(pedidoRepository.bloquearPorId(7L)).thenReturn(Optional.of(pedido));
        when(prendaRepository.bloquearPorId(20L)).thenReturn(Optional.of(prendaConStock(5)));

        service.confirmarPago(7L);

        verify(eventos).publishEvent(new PedidoEstadoCambiadoEvent(7L, EstadoPedido.REGISTRADO));
    }
}
