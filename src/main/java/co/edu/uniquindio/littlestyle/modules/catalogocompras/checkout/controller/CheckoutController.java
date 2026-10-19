package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.controller;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.ResumenCheckoutRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.ResumenCheckoutResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.PedidoDetalleResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.PedidoEstadoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.PedidoResumenResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.DireccionEnvioRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.DireccionGuardadaResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.ItemResumenCheckoutRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.service.CarritoService;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service.CheckoutFacade;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service.DireccionClienteService;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service.PedidoConsultaService;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service.PedidoEstadoService;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import co.edu.uniquindio.littlestyle.shared.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Checkout y pedidos del cliente autenticado: dirección de envío, resumen de compra, historial y detalle de
 * pedidos, y cancelación antes del pago.
 */
@RestController
@RequestMapping("/api/cliente/checkout")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENTE')")
public class CheckoutController {

    private final CheckoutFacade checkoutFacade;
    private final PedidoConsultaService pedidoConsultaService;
    private final PedidoEstadoService pedidoEstadoService;
    private final CarritoService carritoService;
    private final DireccionClienteService direccionClienteService;

    /** Devuelve la dirección de envío guardada del cliente, si tiene una. */
    @GetMapping("/direccion")
    public ResponseEntity<DireccionGuardadaResponse> obtenerDireccion() {
        return ResponseEntity.ok(direccionClienteService.obtener());
    }

    /** Guarda o reemplaza la dirección de envío del cliente. */
    @PutMapping("/direccion")
    public ResponseEntity<DireccionGuardadaResponse> guardarDireccion(
            @Valid @RequestBody DireccionEnvioRequest request) {
        return ResponseEntity.ok(direccionClienteService.guardar(request));
    }

    /**
     * Calcula el resumen de la compra con el carrito actual: valida stock y alergias del perfil y suma el
     * costo de envío según la dirección.
     */
    @PostMapping("/resumen")
    public ResponseEntity<ResumenCheckoutResponse> calcularResumen(
            @Valid @RequestBody ResumenCheckoutRequest request) {
        var items = carritoService.obtener().items().stream()
                .map(item -> new ItemResumenCheckoutRequest(item.prendaId(), item.talla(), item.cantidad(),
                        item.tallaSugerida()))
                .toList();
        return ResponseEntity.ok(checkoutFacade.calcularResumen(request, items));
    }

    /** Lista los pedidos del cliente, del más reciente al más antiguo. */
    @GetMapping("/pedidos")
    public ResponseEntity<List<PedidoResumenResponse>> misPedidos() {
        return ResponseEntity.ok(pedidoConsultaService.misPedidos());
    }

    /** Devuelve el detalle de un pedido propio con sus despachos e historial. */
    @GetMapping("/pedidos/{id}")
    public ResponseEntity<PedidoDetalleResponse> detallePedido(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoConsultaService.detalle(id));
    }

    /**
     * Retracto del cliente antes del pago: solo desde PENDIENTE_PAGO, sin
     * stock ni reembolso involucrados. Después del pago la cancelación es
     * del vendedor/administrador.
     */
    @PostMapping("/pedidos/{id}/cancelar")
    public ResponseEntity<PedidoEstadoResponse> cancelarPedido(@PathVariable Long id) {
        Long clienteId = SecurityUtils.usuarioActual()
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado")).id();
        var pedido = pedidoEstadoService.cancelarComoCliente(clienteId, id);
        return ResponseEntity.ok(new PedidoEstadoResponse(
                pedido.getId(), pedido.getEstado(), pedido.getNumeroGuia(), pedido.getFechaActualizacion()));
    }
}
