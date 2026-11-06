package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.service.CarritoService;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.HistorialEstadoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.service.PerfilInfantilService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

/**
 * Auditoría 4.2: el número de pedido es un consecutivo por cliente, separado del id de la base de datos.
 * Se prueba el cálculo del siguiente número y que el cliente se bloquee antes de leerlo.
 */
@ExtendWith(MockitoExtension.class)
class PedidoCheckoutServiceNumeroTest {

    @Mock
    private CarritoService carritoService;
    @Mock
    private CheckoutFacade checkoutFacade;
    @Mock
    private PerfilInfantilService perfilInfantilService;
    @Mock
    private PrendaRepository prendaRepository;
    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private HistorialEstadoRepository historialRepository;
    @Mock
    private ApplicationEventPublisher eventos;

    private PedidoCheckoutService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new PedidoCheckoutService(carritoService, checkoutFacade, perfilInfantilService,
                prendaRepository, pedidoRepository, historialRepository, Clock.systemUTC(), eventos);
    }

    // El primer pedido de un cliente es el número 1, aunque su id global sea 4521
    @Test
    void elPrimerPedidoDeUnClienteEsElNumeroUno() {
        when(pedidoRepository.bloquearCliente(5L)).thenReturn(Optional.of(Usuario.builder().id(5L).build()));
        when(pedidoRepository.ultimoNumeroDelCliente(5L)).thenReturn(0);

        assertThat(servicio.siguienteNumeroDePedido(5L)).isEqualTo(1);
    }

    // Cada cliente lleva su propia cuenta: con 3 pedidos previos el siguiente es el 4
    @Test
    void elSiguienteEsElMayorConsecutivoDelClienteMasUno() {
        when(pedidoRepository.ultimoNumeroDelCliente(5L)).thenReturn(3);

        assertThat(servicio.siguienteNumeroDePedido(5L)).isEqualTo(4);
    }

    // Sin bloquear antes la fila del cliente, dos compras simultáneas podrían leer el mismo máximo
    @Test
    void bloqueaAlClienteAntesDeLeerSuUltimoNumero() {
        when(pedidoRepository.ultimoNumeroDelCliente(5L)).thenReturn(0);

        servicio.siguienteNumeroDePedido(5L);

        InOrder orden = inOrder(pedidoRepository);
        orden.verify(pedidoRepository).bloquearCliente(5L);
        orden.verify(pedidoRepository).ultimoNumeroDelCliente(5L);
    }
}
