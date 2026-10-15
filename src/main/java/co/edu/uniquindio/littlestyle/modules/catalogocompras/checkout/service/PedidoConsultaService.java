package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.PedidoDetalleResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.PedidoResumenResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.HistorialEstadoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import co.edu.uniquindio.littlestyle.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Trazabilidad del cliente sobre sus pedidos (US-12, RNF-13). Un pedido ajeno
 * se trata como inexistente, igual que los perfiles infantiles.
 */
@Service
@RequiredArgsConstructor
public class PedidoConsultaService {

    private final PedidoRepository pedidoRepository;
    private final HistorialEstadoRepository historialRepository;

    @Transactional(readOnly = true)
    public List<PedidoResumenResponse> misPedidos() {
        return pedidoRepository.findAllByClienteIdOrderByFechaCreacionDesc(clienteActual())
                .stream().map(PedidoResumenResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PedidoDetalleResponse detalle(Long id) {
        Pedido pedido = pedidoRepository.findByIdAndClienteId(id, clienteActual())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        List<PedidoDetalleResponse.Movimiento> historial = historialRepository
                .findAllByPedidoIdOrderByFechaCambioAsc(id).stream()
                .map(h -> new PedidoDetalleResponse.Movimiento(h.getAnterior(), h.getNuevo(), h.getFechaCambio()))
                .toList();
        return new PedidoDetalleResponse(pedido.getId(), pedido.getEstado(), pedido.getSubtotal(),
                pedido.getCostoEnvio(), pedido.getTotal(), pedido.getFechaCreacion(), pedido.getNumeroGuia(),
                pedido.getCiudad(), pedido.getDepartamento(),
                pedido.getItems().stream().map(PedidoDetalleResponse.Item::from).toList(), historial);
    }

    private static Long clienteActual() {
        return SecurityUtils.usuarioActual()
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado")).id();
    }
}
