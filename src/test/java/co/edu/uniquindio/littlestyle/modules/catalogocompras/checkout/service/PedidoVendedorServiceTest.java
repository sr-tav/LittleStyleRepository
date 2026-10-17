package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.AvanzarEstadoRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.DespachoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Despacho;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.LineaPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.DespachoRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoVendedorServiceTest {

    private static final long VENDEDOR_ID = 31L;

    @Mock
    private DespachoRepository despachoRepository;
    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository prendaRepository;
    @Mock
    private DespachoService despachos;

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

    private Usuario vendedor(long id) {
        return Usuario.builder().id(id).nombre("Tienda " + id).nombreTienda("Tienda " + id).build();
    }

    private Pedido pedidoConDespacho(long pedidoId, long despachoId, EstadoPedido estadoDespacho) {
        Pedido pedido = Pedido.builder().id(pedidoId).estado(EstadoPedido.REGISTRADO)
                .subtotal(new BigDecimal("150000")).costoEnvio(new BigDecimal("7000"))
                .total(new BigDecimal("157000")).fechaCreacion(LocalDateTime.now()).build();
        Despacho despacho = new Despacho();
        despacho.setId(despachoId);
        despacho.setPedido(pedido);
        despacho.setVendedor(vendedor(VENDEDOR_ID));
        despacho.setEstado(estadoDespacho);
        despacho.setFechaActualizacion(LocalDateTime.now());
        LineaPedido linea = LineaPedido.builder().prendaId(10L).nombre("Pantalón").talla("4")
                .cantidad(2).precioUnitario(new BigDecimal("50000")).subtotal(new BigDecimal("100000")).build();
        linea.setPedido(pedido);
        linea.setDespacho(despacho);
        pedido.agregar(linea);
        return pedido;
    }

    private Despacho despachoPropio(long despachoId, long pedidoId, EstadoPedido estado) {
        Pedido pedido = Pedido.builder().id(pedidoId).estado(EstadoPedido.REGISTRADO).build();
        Despacho despacho = new Despacho();
        despacho.setId(despachoId);
        despacho.setPedido(pedido);
        despacho.setVendedor(vendedor(VENDEDOR_ID));
        despacho.setEstado(estado);
        despacho.setFechaActualizacion(LocalDateTime.now());
        return despacho;
    }

    @Test
    void listaSoloSusDespachos() {
        Despacho d = despachoPropio(5L, 7L, EstadoPedido.REGISTRADO);
        when(despachoRepository.findAllByVendedorIdOrderByFechaActualizacionDesc(VENDEDOR_ID))
                .thenReturn(List.of(d));
        Pedido pedido = pedidoConDespacho(7L, 5L, EstadoPedido.REGISTRADO);
        when(pedidoRepository.findById(7L)).thenReturn(Optional.of(pedido));

        List<DespachoResponse> resumen = service.misDespachos();

        assertThat(resumen).hasSize(1);
        assertThat(resumen.getFirst().despachoId()).isEqualTo(5L);
        assertThat(resumen.getFirst().pedidoId()).isEqualTo(7L);
        assertThat(resumen.getFirst().items()).hasSize(1);
    }

    @Test
    void detalleDeDespachoAjenoDevuelve403() {
        Despacho ajeno = despachoPropio(9L, 8L, EstadoPedido.REGISTRADO);
        ajeno.setVendedor(vendedor(99L));
        when(despachoRepository.findById(9L)).thenReturn(Optional.of(ajeno));

        assertThatThrownBy(() -> service.detalle(9L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void detalleInexistenteDevuelve404() {
        when(despachoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detalle(99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void avanzarDespachoPropioDelegaAlServicioDeDespachos() {
        Despacho d = despachoPropio(5L, 7L, EstadoPedido.REGISTRADO);
        when(despachoRepository.findById(5L)).thenReturn(Optional.of(d));
        Despacho avanzado = despachoPropio(5L, 7L, EstadoPedido.EN_PREPARACION);
        when(despachos.avanzarComoVendedor(5L, VENDEDOR_ID, EstadoPedido.EN_PREPARACION, null))
                .thenReturn(avanzado);
        Pedido pedido = pedidoConDespacho(7L, 5L, EstadoPedido.EN_PREPARACION);
        when(pedidoRepository.findById(7L)).thenReturn(Optional.of(pedido));

        DespachoResponse detalle = service.avanzarEstado(5L,
                new AvanzarEstadoRequest(EstadoPedido.EN_PREPARACION, null));

        assertThat(detalle.estado()).isEqualTo(EstadoPedido.EN_PREPARACION);
        verify(despachos).avanzarComoVendedor(5L, VENDEDOR_ID, EstadoPedido.EN_PREPARACION, null);
    }

    @Test
    void avanzarDespachoAjenoDevuelve403() {
        Despacho ajeno = despachoPropio(9L, 8L, EstadoPedido.REGISTRADO);
        ajeno.setVendedor(vendedor(99L));
        when(despachoRepository.findById(9L)).thenReturn(Optional.of(ajeno));

        assertThatThrownBy(() -> service.avanzarEstado(9L,
                new AvanzarEstadoRequest(EstadoPedido.EN_PREPARACION, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
