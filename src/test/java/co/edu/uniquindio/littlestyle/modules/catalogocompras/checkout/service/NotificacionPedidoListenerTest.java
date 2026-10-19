package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoEstadoCambiadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.LineaPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import co.edu.uniquindio.littlestyle.shared.correo.CorreoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests del listener que envía correos al cambiar el ciclo de vida de un pedido:
 * creación, pago, rechazo, cambios de estado y despacho.
 * Se mockean los repositorios y el servicio de correo para verificar qué se envía y cuándo.
 */
@ExtendWith(MockitoExtension.class)
class NotificacionPedidoListenerTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository prendaRepository;
    @Mock
    private co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.repository.TransaccionPagoRepository transaccionRepository;
    @Mock
    private co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.DespachoRepository despachoRepository;
    @Mock
    private CorreoService correo;

    @InjectMocks
    private NotificacionPedidoListener listener;

    // Pedido de ejemplo con un cliente y una línea de "Pantalón", para reusar en varios tests.
    private Pedido pedido() {
        Pedido pedido = Pedido.builder()
                .id(7L)
                .cliente(Usuario.builder().id(1L).nombre("Ana").email("a@b.com").build())
                .estado(EstadoPedido.EN_PREPARACION)
                .total(new BigDecimal("107000"))
                .build();
        pedido.agregar(LineaPedido.builder().prendaId(3L).nombre("Pantalón")
                .talla("4").cantidad(1).precioUnitario(new BigDecimal("107000"))
                .subtotal(new BigDecimal("107000")).build());
        return pedido;
    }

    // Para estados como ENVIADO el cliente ya recibe el correo con la guía desde el despacho,
    // así que el listener no debe mandar otro correo de cambio de estado (evita duplicados).
    @Test
    void cambioAEnviadoNoDuplicaElCorreoConGuia() {
        listener.alCambiarEstado(new PedidoEstadoCambiadoEvent(7L, EstadoPedido.ENVIADO));

        verify(pedidoRepository, never()).findById(anyLong());
        verify(correo, never()).enviarCambioEstado(anyString(), anyString(), anyLong(), anyString(), anyString());
    }

    // Cuando el pedido pasa a preparación el cliente debe recibir su correo con el estado y el total.
    @Test
    void cambioAPreparacionAvisaAlCliente() {
        when(pedidoRepository.findById(7L)).thenReturn(Optional.of(pedido()));

        listener.alCambiarEstado(new PedidoEstadoCambiadoEvent(7L, EstadoPedido.EN_PREPARACION));

        verify(correo).enviarCambioEstado(eq("a@b.com"), eq("Ana"), eq(7L), eq("En preparación"), eq("107000"));
    }

    // Si por algún motivo llega un evento con un pedido que ya no existe, no se intenta enviar nada.
    @Test
    void pedidoInexistenteNoAvisa() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        listener.alCambiarEstado(new PedidoEstadoCambiadoEvent(99L, EstadoPedido.CANCELADO));

        verify(correo, never()).enviarCambioEstado(anyString(), anyString(), anyLong(), anyString(), anyString());
    }

    // La creación y la confirmación de pago disparan cada una su correo al cliente.
    @Test
    void creacionYPagoAvisanAlCliente() {
        when(pedidoRepository.findById(7L)).thenReturn(Optional.of(pedido()));

        listener.alCrearse(new co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoCreadoEvent(7L));
        listener.alPagarse(new co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoPagadoEvent(7L));

        verify(correo).enviarPedidoCreado(eq("a@b.com"), eq("Ana"), eq(7L), eq("107000"), anyList());
        verify(correo).enviarPagoConfirmado(eq("a@b.com"), eq("Ana"), eq(7L), eq("107000"), anyList());
    }

    // Al pagar, además del correo al cliente, el vendedor dueño de la prenda
    // debe recibir su propio aviso con la venta nueva.
    @Test
    void pagoAvisaAlVendedorConSusLineas() {
        when(pedidoRepository.findById(7L)).thenReturn(Optional.of(pedido()));
        var vendedor = Usuario.builder().id(9L).nombre("Luis").nombreTienda("Tienda Sol")
                .email("v@tienda.com").build();
        var prenda = co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda.builder()
                .id(3L).vendedor(vendedor).build();
        when(prendaRepository.findById(3L)).thenReturn(Optional.of(prenda));

        listener.alPagarse(new co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoPagadoEvent(7L));

        verify(correo).enviarPagoConfirmado(eq("a@b.com"), eq("Ana"), eq(7L), eq("107000"), anyList());
        verify(correo).enviarPedidoNuevoVendedor(eq("v@tienda.com"), eq("Tienda Sol"), eq(7L), eq("Ana"), anyList());
    }

    // Si Wompi rechaza el pago, el cliente se entera; aquí el listener trabaja con la
    // transacción directamente, sin necesidad de que exista el pedido.
    @Test
    void rechazoAvisaAlClienteSinPedido() {
        var cliente = Usuario.builder().id(1L).nombre("Ana").email("a@b.com").build();
        var transaccion = co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model.TransaccionPago.builder()
                .id(9L).cliente(cliente).referencia("LS-55-X").montoCentavos(7280000L).moneda("COP")
                .estado(co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model.EstadoTransaccionPago.RECHAZADA)
                .build();
        when(transaccionRepository.findById(9L)).thenReturn(Optional.of(transaccion));

        listener.alRechazarse(new co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoPagoRechazadoEvent(9L));

        verify(correo).enviarPagoRechazado(eq("a@b.com"), eq("Ana"), eq("LS-55-X"), eq("7280000"));
    }

    // Cuando sale un despacho, el cliente recibe el correo de envío con la guía del paquete.
    @Test
    void alEnviarDespachoAvisaConGuiaDelPaquete() {
        Pedido pedido = pedido();
        var vendedor = Usuario.builder().id(9L).nombre("Luis").email("v@tienda.com").build();
        var despachado = new co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Despacho();
        despachado.setId(5L);
        despachado.setPedido(pedido);
        despachado.setVendedor(vendedor);
        despachado.setEstado(EstadoPedido.ENVIADO);
        despachado.setNumeroGuia("GUIA-5");
        despachado.setFechaActualizacion(java.time.LocalDateTime.now());
        pedido.getItems().getFirst().setDespacho(despachado);
        when(despachoRepository.findById(5L)).thenReturn(Optional.of(despachado));
        when(pedidoRepository.findById(7L)).thenReturn(Optional.of(pedido));

        listener.alEnviarDespacho(new co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.DespachoEnviadoEvent(7L, 5L));

        verify(correo).enviarPedidoEnviado(eq("a@b.com"), eq("Ana"), eq(7L), eq("GUIA-5"), eq("107000"), anyList());
    }

    // Si la prenda tiene fotos, el correo debe incluir la imagen (miniatura) y el precio por unidad.
    @Test
    void lineaConFotoIncluyeMiniimagenYPrecios() {
        when(pedidoRepository.findById(7L)).thenReturn(Optional.of(pedido()));
        var prenda = co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda.builder()
                .id(3L).build();
        prenda.getImagenes().add(co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.ImagenPrenda
                .builder().url("/media/a.png").clave("a").orden(0)
                .fechaCarga(java.time.LocalDateTime.now()).build());
        when(prendaRepository.findById(3L)).thenReturn(Optional.of(prenda));

        listener.alCrearse(new co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoCreadoEvent(7L));

        var captor = org.mockito.ArgumentCaptor.forClass(java.util.List.class);
        verify(correo).enviarPedidoCreado(eq("a@b.com"), eq("Ana"), eq(7L), eq("107000"), captor.capture());
        var items = (List<CorreoService.ItemCorreo>) captor.getValue();
        assertThat(items).hasSize(1);
        assertThat(items.getFirst().imagenUrl()).isEqualTo("/media/a.png");
        assertThat(items.getFirst().precioUnitario()).isEqualTo("107000");
    }

    // Igual que con ENVIADO, PARCIALMENTE_ENVIADO no genera un correo extra de cambio de estado.
    @Test
    void cambioAParcialmenteEnviadoNoDuplicaElCorreo() {
        listener.alCambiarEstado(new PedidoEstadoCambiadoEvent(7L, EstadoPedido.PARCIALMENTE_ENVIADO));

        verify(pedidoRepository, never()).findById(anyLong());
        verify(correo, never()).enviarCambioEstado(anyString(), anyString(), anyLong(), anyString(), anyString());
    }

    // Este test no prueba el listener directo sino el notificador de alertas de stock:
    // al emitirse una alerta, el vendedor dueño de la prenda debe recibir el aviso.
    @Test
    void alertaStockAvisaAlVendedor() {
        co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository usuarios =
                org.mockito.Mockito.mock(co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository.class);
        var notificador = new co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.event.NotificadorAlertasReabastecimiento(
                usuarios, correo);
        Usuario vendedor = Usuario.builder().id(9L).nombre("Luis").nombreTienda("Tienda Sol")
                .email("v@tienda.com").build();
        when(usuarios.findById(9L)).thenReturn(Optional.of(vendedor));

        notificador.alEmitirAlerta(new co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.event.AlertaReabastecimientoEmitida(
                1L, 3L, 9L, "Pantalón", "4", co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.model.NivelStock.STOCK_BAJO, 2, 5));

        verify(correo).enviarAlertaStock(eq("v@tienda.com"), eq("Tienda Sol"), eq("Pantalón"), eq("4"), eq("STOCK_BAJO"), eq(2), eq(5));
    }
}
