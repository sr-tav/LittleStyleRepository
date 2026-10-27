package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.dto.CarritoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.dto.ItemCarritoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.service.CarritoService;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.DireccionEnvioRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.ItemResumenCheckoutRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.PedidoPagoSnapshot;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoCreadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.CrearPedidoRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.ResumenCheckoutRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.ResumenCheckoutResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.HistorialEstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.LineaPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.HistorialEstadoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.service.PerfilInfantilService;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import co.edu.uniquindio.littlestyle.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Prepara la compra para el pago y crea el pedido cuando Wompi aprueba el cobro. */
@Service
@RequiredArgsConstructor
public class PedidoCheckoutService {

    private final CarritoService carritoService;
    private final CheckoutFacade checkoutFacade;
    private final PerfilInfantilService perfilInfantilService;
    private final PrendaRepository prendaRepository;
    private final PedidoRepository pedidoRepository;
    private final HistorialEstadoRepository historialRepository;
    private final Clock clock;
    private final ApplicationEventPublisher eventos;

    /**
     * Bloquea el carrito y sus prendas, verifica que todo siga disponible y calcula el resumen. Devuelve el
     * snapshot que se guarda en la transacción de pago.
     */
    @Transactional
    public PedidoPagoSnapshot prepararPago(CrearPedidoRequest request) {
        if (request == null || request.perfilInfantilId() == null || request.perfilInfantilId() <= 0
                || request.direccion() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "El perfil y la dirección de envío deben ser válidos", "direccion");
        }
        perfilInfantilService.obtener(request.perfilInfantilId());
        CarritoResponse carrito = carritoService.bloquearYObtenerActual();
        if (carrito.items().isEmpty()) {
            throw new BusinessException(HttpStatus.CONFLICT, "El carrito está vacío", "items");
        }
        if (carrito.items().stream().anyMatch(item -> !item.disponible())) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Una o más prendas del carrito ya no están disponibles. Vuelve al carrito para retirarlas o elegir otras antes de continuar.",
                    "items");
        }
        carrito.items().stream().map(ItemCarritoResponse::prendaId).distinct().sorted()
                .forEach(prendaId -> prendaRepository.bloquearPorIdYEstado(prendaId, EstadoPrenda.ACTIVA)
                        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                                "Una prenda del carrito ya no está disponible. Vuelve al carrito para retirarla o elegir otra antes de continuar.")));
        List<ItemResumenCheckoutRequest> items = carrito.items().stream()
                .map(item -> new ItemResumenCheckoutRequest(item.prendaId(), item.talla(), item.cantidad(),
                        item.tallaSugerida()))
                .toList();
        ResumenCheckoutResponse resumen = checkoutFacade.calcularResumen(
                new ResumenCheckoutRequest(request.perfilInfantilId(), request.direccion()), items);
        DireccionEnvioRequest direccion = normalizarDireccion(request.direccion());
        return new PedidoPagoSnapshot(resumen.perfilInfantilId(), direccion, resumen.items(),
                resumen.subtotal(), resumen.costoEnvio(), resumen.total());
    }

    /**
     * Crea el pedido en {@code PENDIENTE_PAGO} con los datos del snapshot, registra el primer movimiento del
     * historial y publica {@link PedidoCreadoEvent}.
     */
    @Transactional
    public Pedido crearPedidoDesdePago(Long clienteId, PedidoPagoSnapshot snapshot) {
        if (snapshot == null || snapshot.direccion() == null || snapshot.items().isEmpty()) {
            throw new IllegalStateException("La transacción de pago no contiene una compra válida");
        }
        Usuario cliente = Usuario.builder().id(clienteId).build();
        LocalDateTime ahora = LocalDateTime.now(clock);
        Pedido pedido = Pedido.builder()
                .cliente(cliente)
                .perfilInfantilId(snapshot.perfilInfantilId())
                .estado(EstadoPedido.PENDIENTE_PAGO)
                .destinatario(snapshot.direccion().destinatario())
                .direccion(snapshot.direccion().direccion())
                .complemento(snapshot.direccion().complemento())
                .codigoPostal(snapshot.direccion().codigoPostal())
                .departamento(snapshot.direccion().departamento())
                .ciudad(snapshot.direccion().municipio())
                .telefono(snapshot.direccion().telefono())
                .subtotal(snapshot.subtotal())
                .costoEnvio(snapshot.costoEnvio())
                .total(snapshot.total())
                .fechaCreacion(ahora)
                .fechaActualizacion(ahora)
                .build();
        snapshot.items().forEach(linea -> pedido.agregar(LineaPedido.builder()
                .prendaId(linea.prendaId())
                .nombre(linea.nombre())
                .talla(linea.talla())
                .cantidad(linea.cantidad())
                .precioUnitario(linea.precioUnitario())
                .subtotal(linea.subtotal())
                .tallaSugerida(linea.tallaSugerida())
                .build()));
        pedidoRepository.saveAndFlush(pedido);
        historialRepository.save(HistorialEstadoPedido.builder()
                .pedido(pedido)
                .anterior(null)
                .nuevo(EstadoPedido.PENDIENTE_PAGO)
                .fechaCambio(ahora)
                .build());
        eventos.publishEvent(new PedidoCreadoEvent(pedido.getId()));
        return pedido;
    }

    /** Copia la dirección sin espacios sobrantes; el complemento vacío queda nulo. */
    private static DireccionEnvioRequest normalizarDireccion(DireccionEnvioRequest direccion) {
        return new DireccionEnvioRequest(
                direccion.destinatario().trim(),
                direccion.direccion().trim(),
                direccion.complemento() == null || direccion.complemento().isBlank()
                        ? null : direccion.complemento().trim(),
                direccion.codigoPostal().trim(),
                direccion.departamento().trim(),
                direccion.municipio().trim(),
                direccion.telefono().trim());
    }
}
