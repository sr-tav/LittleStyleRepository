package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.PedidoDetalleResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.PedidoVendedorResumenResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
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
class PedidoVendedorServiceTest {

    private static final long VENDEDOR_ID = 31L;

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private PrendaRepository prendaRepository;
    @Mock
    private HistorialEstadoRepository historialRepository;

    @InjectMocks
    private PedidoVendedorService service;

    @BeforeEach
    void autenticar() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(VENDEDOR_ID, "tienda@test.com", "Tienda", Rol.VENDEDOR), null, List.of()));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static Pedido pedido(Long id) {
        Pedido pedido = Pedido.builder().id(id).estado(EstadoPedido.EN_PREPARACION)
                .subtotal(new BigDecimal("150000")).costoEnvio(new BigDecimal("7000"))
                .total(new BigDecimal("157000")).fechaCreacion(LocalDateTime.now()).build();
        pedido.agregar(LineaPedido.builder().prendaId(10L).nombre("Pantalón").talla("4")
                .cantidad(2).precioUnitario(new BigDecimal("50000")).subtotal(new BigDecimal("100000")).build());
        pedido.agregar(LineaPedido.builder().prendaId(99L).nombre("Vestido ajeno").talla("6")
                .cantidad(1).precioUnitario(new BigDecimal("50000")).subtotal(new BigDecimal("50000")).build());
        return pedido;
    }

    @Test
    void listaSoloCuentaYSumaSusPropiasLineas() {
        when(prendaRepository.findIdsByVendedorId(VENDEDOR_ID)).thenReturn(List.of(10L));
        when(pedidoRepository.findAllConLineasDelVendedor(VENDEDOR_ID)).thenReturn(List.of(pedido(7L)));

        List<PedidoVendedorResumenResponse> resumen = service.misPedidos();

        assertThat(resumen).hasSize(1);
        assertThat(resumen.getFirst().cantidadItemsPropios()).isEqualTo(2);
        assertThat(resumen.getFirst().subtotalPropio()).isEqualByComparingTo("100000");
    }

    @Test
    void detalleDePedidoSinLineasPropiasDevuelve404() {
        Pedido ajeno = Pedido.builder().id(8L).estado(EstadoPedido.EN_PREPARACION)
                .subtotal(new BigDecimal("50000")).costoEnvio(new BigDecimal("7000"))
                .total(new BigDecimal("57000")).fechaCreacion(LocalDateTime.now()).build();
        ajeno.agregar(LineaPedido.builder().prendaId(99L).nombre("Vestido ajeno").talla("6")
                .cantidad(1).precioUnitario(new BigDecimal("50000"))
                .subtotal(new BigDecimal("50000")).build());
        when(prendaRepository.findIdsByVendedorId(VENDEDOR_ID)).thenReturn(List.of(10L));
        when(pedidoRepository.findById(8L)).thenReturn(Optional.of(ajeno));

        assertThatThrownBy(() -> service.detalle(8L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void detalleMuestraDireccionYSoloLineasPropias() {
        Pedido pedido = pedido(7L);
        pedido.setDestinatario("Ana");
        pedido.setCiudad("Armenia");
        when(prendaRepository.findIdsByVendedorId(VENDEDOR_ID)).thenReturn(List.of(10L));
        when(pedidoRepository.findById(7L)).thenReturn(Optional.of(pedido));
        when(historialRepository.findAllByPedidoIdOrderByFechaCambioAsc(7L)).thenReturn(List.of());

        var detalle = service.detalle(7L);

        assertThat(detalle.items()).hasSize(1);
        assertThat(detalle.items().stream().map(PedidoDetalleResponse.Item::nombre).toList())
                .containsExactly("Pantalón");
        assertThat(detalle.ciudad()).isEqualTo("Armenia");
    }
}
