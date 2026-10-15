package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.dto.CarritoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.dto.ItemCarritoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.service.CarritoService;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.CrearPedidoRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoCreadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.DireccionEnvioRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.ItemResumenCheckoutRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.LineaResumenCheckoutResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.PedidoCreadoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.ResumenCheckoutRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.ResumenCheckoutResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.ClaveIdempotenciaPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.HistorialEstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.LineaPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.ClaveIdempotenciaRepository;
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

@Service
@RequiredArgsConstructor
public class PedidoCheckoutService {

    private final CarritoService carritoService;
    private final CheckoutFacade checkoutFacade;
    private final PerfilInfantilService perfilInfantilService;
    private final PrendaRepository prendaRepository;
    private final PedidoRepository pedidoRepository;
    private final HistorialEstadoRepository historialRepository;
    private final ClaveIdempotenciaRepository clavesIdempotencia;
    private final Clock clock;
    private final ApplicationEventPublisher eventos;

    @Transactional
    public PedidoCreadoResponse crear(CrearPedidoRequest request) {
        return crear(request, null);
    }

    /**
     * Crea el pedido de forma idempotente: si la clave ya se usó y no venció,
     * devuelve el pedido original sin crear otro (reintentos tras timeout).
     */
    @Transactional
    public PedidoCreadoResponse crear(CrearPedidoRequest request, String claveIdempotencia) {
        Long clienteId = SecurityUtils.usuarioActual()
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"))
                .id();
        String clave = normalizarClave(claveIdempotencia);
        if (clave != null) {
            var existente = clavesIdempotencia.findByClaveAndClienteId(clave, clienteId);
            if (existente.isPresent()) {
                if (existente.get().getExpira().isAfter(LocalDateTime.now(clock))) {
                    Pedido pedido = pedidoRepository.findById(existente.get().getPedidoId())
                            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
                    return respuestaExistente(pedido);
                }
                clavesIdempotencia.delete(existente.get());
            }
        }
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
                    "Lo sentimos, ya no quedan unidades disponibles para una o más prendas del carrito.",
                    "items");
        }
        carrito.items().stream().map(ItemCarritoResponse::prendaId).distinct().sorted()
                .forEach(prendaId -> prendaRepository.bloquearPorIdYEstado(prendaId, EstadoPrenda.ACTIVA)
                        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                                "Una prenda del carrito ya no se encuentra disponible")));
        List<ItemResumenCheckoutRequest> items = carrito.items().stream()
                .map(item -> new ItemResumenCheckoutRequest(item.prendaId(), item.talla(), item.cantidad()))
                .toList();
        ResumenCheckoutResponse resumen = checkoutFacade.calcularResumen(
                new ResumenCheckoutRequest(request.perfilInfantilId(), request.direccion()), items);

        Usuario cliente = SecurityUtils.usuarioActual()
                .map(usuario -> Usuario.builder().id(usuario.id()).build())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));
        Pedido pedido = Pedido.builder()
                .cliente(cliente)
                .perfilInfantilId(resumen.perfilInfantilId())
                .estado(EstadoPedido.PENDIENTE_PAGO)
                .destinatario(request.direccion().destinatario().trim())
                .direccion(request.direccion().direccion().trim())
                .complemento(request.direccion().complemento() == null
                        || request.direccion().complemento().isBlank()
                        ? null : request.direccion().complemento().trim())
                .codigoPostal(request.direccion().codigoPostal().trim())
                .departamento(request.direccion().departamento().trim())
                .ciudad(request.direccion().municipio().trim())
                .telefono(request.direccion().telefono())
                .subtotal(resumen.subtotal())
                .costoEnvio(resumen.costoEnvio())
                .total(resumen.total())
                .fechaCreacion(LocalDateTime.now(clock))
                .fechaActualizacion(LocalDateTime.now(clock))
                .build();
        for (int i = 0; i < resumen.items().size(); i++) {
            var linea = resumen.items().get(i);
            pedido.agregar(LineaPedido.builder()
                    .prendaId(linea.prendaId())
                    .nombre(linea.nombre())
                    .talla(linea.talla())
                    .cantidad(linea.cantidad())
                    .precioUnitario(linea.precioUnitario())
                    .subtotal(linea.subtotal())
                    .build());
        }
        pedidoRepository.saveAndFlush(pedido);
        historialRepository.save(HistorialEstadoPedido.builder()
                .pedido(pedido)
                .anterior(null)
                .nuevo(EstadoPedido.PENDIENTE_PAGO)
                .fechaCambio(pedido.getFechaCreacion())
                .build());
        eventos.publishEvent(new PedidoCreadoEvent(pedido.getId()));
        if (clave != null) {
            clavesIdempotencia.save(ClaveIdempotenciaPedido.builder()
                    .clave(clave)
                    .clienteId(clienteId)
                    .pedidoId(pedido.getId())
                    .expira(LocalDateTime.now(clock).plusHours(24))
                    .build());
        }
        carritoService.vaciar();

        var direccionPersistida = new DireccionEnvioRequest(
                pedido.getDestinatario(), pedido.getDireccion(), pedido.getComplemento(),
                pedido.getCodigoPostal(), pedido.getDepartamento(), pedido.getCiudad(), pedido.getTelefono());
        return new PedidoCreadoResponse(pedido.getId(), pedido.getEstado(), pedido.getPerfilInfantilId(),
                direccionPersistida, resumen.items(), pedido.getSubtotal(), pedido.getCostoEnvio(),
                pedido.getTotal());
    }

    private PedidoCreadoResponse respuestaExistente(Pedido pedido) {
        var direccion = new DireccionEnvioRequest(
                pedido.getDestinatario(), pedido.getDireccion(), pedido.getComplemento(),
                pedido.getCodigoPostal(), pedido.getDepartamento(), pedido.getCiudad(), pedido.getTelefono());
        var lineas = pedido.getItems().stream()
                .map(l -> new LineaResumenCheckoutResponse(l.getPrendaId(), l.getNombre(), l.getTalla(),
                        l.getCantidad(), l.getPrecioUnitario(), l.getSubtotal()))
                .toList();
        return new PedidoCreadoResponse(pedido.getId(), pedido.getEstado(), pedido.getPerfilInfantilId(),
                direccion, lineas, pedido.getSubtotal(), pedido.getCostoEnvio(), pedido.getTotal());
    }

    private static String normalizarClave(String clave) {
        if (clave == null || clave.isBlank()) {
            return null;
        }
        if (clave.trim().length() > 64) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "La clave de idempotencia admite máximo 64 caracteres", "Idempotency-Key");
        }
        return clave.trim();
    }
}
