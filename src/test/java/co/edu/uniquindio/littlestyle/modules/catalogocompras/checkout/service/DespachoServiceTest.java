package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Despacho;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.HistorialEstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.DespachoRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests del servicio de despachos: valida la transición de estados que
 * maneja el vendedor y el recálculo del estado general del pedido.
 * Todos los repositorios y el publicador de eventos se mockean con Mockito.
 */
@ExtendWith(MockitoExtension.class)
class DespachoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private DespachoRepository despachoRepository;
    @Mock
    private HistorialEstadoRepository historialRepository;
    @Mock
    private PrendaRepository prendaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ApplicationEventPublisher eventos;

    @InjectMocks
    private DespachoService service;

    // Helper que arma un despacho de prueba con su pedido, vendedor y estado indicados.
    private static Despacho despacho(Long id, Long pedidoId, Long vendedorId, EstadoPedido estado) {
        Pedido pedido = Pedido.builder().id(pedidoId).estado(EstadoPedido.REGISTRADO).build();
        Despacho despacho = new Despacho();
        despacho.setId(id);
        despacho.setPedido(pedido);
        despacho.setVendedor(Usuario.builder().id(vendedorId).nombre("Tienda").build());
        despacho.setEstado(estado);
        despacho.setFechaActualizacion(LocalDateTime.now());
        return despacho;
    }

    // Si el vendedor intenta pasar el pedido a ENVIADO sin número de guía, la operación se rechaza con 400.
    @Test
    void avanzarAEnviadoSinGuiaDevuelve400() {
        Despacho despacho = despacho(5L, 7L, 31L, EstadoPedido.EN_PREPARACION);
        when(despachoRepository.bloquearPorId(5L)).thenReturn(Optional.of(despacho));
        when(pedidoRepository.bloquearPorId(7L))
                .thenReturn(Optional.of(despacho.getPedido()));

        assertThatThrownBy(() -> service.avanzarComoVendedor(5L, 31L, EstadoPedido.ENVIADO, "  "))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // Con un despacho enviado y otro aún registrado, el pedido queda como PARCIALMENTE_ENVIADO
    // y el cambio debe quedar registrado en el historial de estados.
    @Test
    void recalcularConUnoEnviadoDaParcialmenteEnviado() {
        Pedido pedido = Pedido.builder().id(7L).estado(EstadoPedido.REGISTRADO).build();
        when(pedidoRepository.bloquearPorId(7L)).thenReturn(Optional.of(pedido));
        when(despachoRepository.findAllByPedidoIdOrderByIdAsc(7L)).thenReturn(List.of(
                despacho(5L, 7L, 31L, EstadoPedido.ENVIADO),
                despacho(6L, 7L, 32L, EstadoPedido.REGISTRADO)));

        EstadoPedido derivado = service.recalcularEstadoPedido(7L);

        assertThat(derivado).isEqualTo(EstadoPedido.PARCIALMENTE_ENVIADO);
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.PARCIALMENTE_ENVIADO);
        ArgumentCaptor<HistorialEstadoPedido> historial = ArgumentCaptor.forClass(HistorialEstadoPedido.class);
        verify(historialRepository).save(historial.capture());
        assertThat(historial.getValue().getAnterior()).isEqualTo(EstadoPedido.REGISTRADO);
        assertThat(historial.getValue().getNuevo()).isEqualTo(EstadoPedido.PARCIALMENTE_ENVIADO);
    }
}
