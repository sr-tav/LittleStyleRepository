package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.PedidoDetalleResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.HistorialEstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.LineaPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.HistorialEstadoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoConsultaServiceTest {

    private static final long CLIENTE_ID = 21L;

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private HistorialEstadoRepository historialRepository;
    @Mock
    private co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.DespachoRepository despachoRepository;
    @Mock
    private co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository prendaRepository;

    @InjectMocks
    private PedidoConsultaService service;

    @BeforeEach
    void autenticar() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(CLIENTE_ID, "cliente@test.com", "Ana", Rol.CLIENTE), null, List.of()));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static Pedido pedido(Long id) {
        Pedido pedido = Pedido.builder().id(id).estado(EstadoPedido.ENVIADO)
                .subtotal(new BigDecimal("100000")).costoEnvio(new BigDecimal("7000"))
                .total(new BigDecimal("107000")).fechaCreacion(LocalDateTime.now())
                .numeroGuia("COL123").ciudad("Armenia").departamento("Quindío").build();
        pedido.agregar(LineaPedido.builder().prendaId(10L).nombre("Pantalón").talla("4")
                .cantidad(1).precioUnitario(new BigDecimal("100000")).subtotal(new BigDecimal("100000")).build());
        return pedido;
    }

    @Test
    void listaMisPedidosEnOrden() {
        when(pedidoRepository.findAllByClienteIdOrderByFechaCreacionDesc(CLIENTE_ID))
                .thenReturn(List.of(pedido(7L)));

        var resumen = service.misPedidos();

        assertThat(resumen).extracting(r -> r.id()).containsExactly(7L);
        assertThat(resumen.getFirst().numeroGuia()).isEqualTo("COL123");
    }

    @Test
    void detalleIncluyeItemsEHistorial() {
        when(pedidoRepository.findByIdAndClienteId(7L, CLIENTE_ID)).thenReturn(Optional.of(pedido(7L)));
        when(despachoRepository.findAllByPedidoIdOrderByIdAsc(7L)).thenReturn(List.of());
        when(historialRepository.findAllByPedidoIdOrderByFechaCambioAsc(7L)).thenReturn(List.of(
                HistorialEstadoPedido.builder().pedido(pedido(7L)).anterior(null)
                        .nuevo(EstadoPedido.PENDIENTE_PAGO).fechaCambio(LocalDateTime.now()).build(),
                HistorialEstadoPedido.builder().pedido(pedido(7L)).anterior(EstadoPedido.EN_PREPARACION)
                        .nuevo(EstadoPedido.ENVIADO).fechaCambio(LocalDateTime.now()).build()));

        PedidoDetalleResponse detalle = service.detalle(7L);

        assertThat(detalle.items()).hasSize(1);
        assertThat(detalle.items().getFirst().nombre()).isEqualTo("Pantalón");
        assertThat(detalle.historial()).extracting(PedidoDetalleResponse.Movimiento::nuevo)
                .containsExactly(EstadoPedido.PENDIENTE_PAGO, EstadoPedido.ENVIADO);
    }

    @Test
    void detalleIncluyeMiniaturaDelProducto() {
        when(pedidoRepository.findByIdAndClienteId(7L, CLIENTE_ID)).thenReturn(Optional.of(pedido(7L)));
        when(despachoRepository.findAllByPedidoIdOrderByIdAsc(7L)).thenReturn(List.of());
        when(historialRepository.findAllByPedidoIdOrderByFechaCambioAsc(7L)).thenReturn(List.of());
        var prenda = co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda.builder()
                .id(10L).build();
        prenda.getImagenes().add(co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.ImagenPrenda
                .builder().url("/media/a.png").clave("a").orden(0)
                .fechaCarga(LocalDateTime.now()).build());
        when(prendaRepository.findById(10L)).thenReturn(Optional.of(prenda));

        PedidoDetalleResponse detalle = service.detalle(7L);

        assertThat(detalle.items().getFirst().imagenUrl()).isEqualTo("/media/a.png");
    }

    @Test
    void pedidoAjenoDevuelve404() {
        when(pedidoRepository.findByIdAndClienteId(99L, CLIENTE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detalle(99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
