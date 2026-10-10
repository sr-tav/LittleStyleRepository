package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.controller;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.ResumenCheckoutRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.ResumenCheckoutResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.CrearPedidoRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.ItemResumenCheckoutRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.PedidoCreadoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.DireccionEnvioRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.DireccionGuardadaResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.service.CarritoService;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service.CheckoutFacade;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service.DireccionClienteService;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service.PedidoCheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cliente/checkout")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENTE')")
public class CheckoutController {

    private final CheckoutFacade checkoutFacade;
    private final PedidoCheckoutService pedidoCheckoutService;
    private final CarritoService carritoService;
    private final DireccionClienteService direccionClienteService;

    @GetMapping("/direccion")
    public ResponseEntity<DireccionGuardadaResponse> obtenerDireccion() {
        return ResponseEntity.ok(direccionClienteService.obtener());
    }

    @PutMapping("/direccion")
    public ResponseEntity<DireccionGuardadaResponse> guardarDireccion(
            @Valid @RequestBody DireccionEnvioRequest request) {
        return ResponseEntity.ok(direccionClienteService.guardar(request));
    }

    @PostMapping("/resumen")
    public ResponseEntity<ResumenCheckoutResponse> calcularResumen(
            @Valid @RequestBody ResumenCheckoutRequest request) {
        var items = carritoService.obtener().items().stream()
                .map(item -> new ItemResumenCheckoutRequest(item.prendaId(), item.talla(), item.cantidad()))
                .toList();
        return ResponseEntity.ok(checkoutFacade.calcularResumen(request, items));
    }

    @PostMapping("/pedidos")
    public ResponseEntity<PedidoCreadoResponse> crearPedido(
            @Valid @RequestBody CrearPedidoRequest request) {
        return ResponseEntity.status(201).body(pedidoCheckoutService.crear(request));
    }
}
