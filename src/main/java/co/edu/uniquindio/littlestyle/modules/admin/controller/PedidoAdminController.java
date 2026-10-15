package co.edu.uniquindio.littlestyle.modules.admin.controller;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.AvanzarEstadoRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.PedidoEstadoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service.PedidoEstadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Avance de estados de pedidos por el administrador. US-14 extenderá este
 * controlador con la moderación; el cliente solo consulta (US-12).
 */
@RestController
@RequestMapping("/api/admin/pedidos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class PedidoAdminController {

    private final PedidoEstadoService estados;

    @PutMapping("/{id}/estado")
    public ResponseEntity<PedidoEstadoResponse> avanzarEstado(
            @PathVariable Long id, @Valid @RequestBody AvanzarEstadoRequest request) {
        Pedido pedido = estados.avanzar(id, request.nuevo(), request.numeroGuia());
        return ResponseEntity.ok(new PedidoEstadoResponse(
                pedido.getId(), pedido.getEstado(), pedido.getNumeroGuia(), pedido.getFechaActualizacion()));
    }
}
