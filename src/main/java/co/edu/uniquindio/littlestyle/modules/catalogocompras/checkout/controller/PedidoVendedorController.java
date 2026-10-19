package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.controller;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.AvanzarEstadoRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.DespachoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service.PedidoVendedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Paquetes del vendedor autenticado (US-12 multi-vendedor). Cada vendedor ve
 * solo su despacho y lo avanza de REGISTRADO a EN_PREPARACION y ENVIADO con
 * guía; el pedido madre se recalcula solo.
 */
@RestController
@RequestMapping("/api/vendedor/pedidos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDEDOR')")
public class PedidoVendedorController {

    private final PedidoVendedorService pedidos;

    /** Lista los despachos del vendedor, del actualizado más recientemente al más antiguo. */
    @GetMapping
    public ResponseEntity<List<DespachoResponse>> misPedidos() {
        return ResponseEntity.ok(pedidos.misDespachos());
    }

    /** Devuelve un despacho propio con sus productos. */
    @GetMapping("/{id}")
    public ResponseEntity<DespachoResponse> detalle(@PathVariable Long id) {
        return ResponseEntity.ok(pedidos.detalle(id));
    }

    /**
     * Avanza un despacho propio al siguiente estado (con guía al enviar) y recalcula el estado del pedido.
     */
    @PutMapping("/{id}/estado")
    public ResponseEntity<DespachoResponse> avanzarEstado(
            @PathVariable Long id, @Valid @RequestBody AvanzarEstadoRequest request) {
        return ResponseEntity.ok(pedidos.avanzarEstado(id, request));
    }
}
