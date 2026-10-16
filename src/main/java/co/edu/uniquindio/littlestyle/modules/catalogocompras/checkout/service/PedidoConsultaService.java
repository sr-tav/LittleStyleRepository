package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.DespachoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.PedidoDetalleResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.PedidoResumenResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Despacho;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.LineaPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.DespachoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.HistorialEstadoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import co.edu.uniquindio.littlestyle.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Trazabilidad del cliente sobre sus pedidos (US-12, RNF-13). Un pedido ajeno
 * se trata como inexistente, igual que los perfiles infantiles. El cliente ve
 * el estado global y, dentro del pedido, un bloque por vendedor con su estado
 * y su guía.
 */
@Service
@RequiredArgsConstructor
public class PedidoConsultaService {

    private final PedidoRepository pedidoRepository;
    private final HistorialEstadoRepository historialRepository;
    private final DespachoRepository despachoRepository;
    private final co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository prendaRepository;

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
                .map(h -> new PedidoDetalleResponse.Movimiento(h.getAnterior(), h.getNuevo(),
                        h.getFechaCambio(), h.getNumeroGuia()))
                .toList();
        return new PedidoDetalleResponse(pedido.getId(), pedido.getEstado(), pedido.getSubtotal(),
                pedido.getCostoEnvio(), pedido.getTotal(), pedido.getFechaCreacion(), pedido.getNumeroGuia(),
                pedido.getCiudad(), pedido.getDepartamento(),
                pedido.getItems().stream().map(l -> PedidoDetalleResponse.Item.from(l, imagenDe(l))).toList(),
                despachosDe(pedido), historial);
    }

    /** Miniatura del producto para el detalle, como cualquier e-commerce. */
    private String imagenDe(LineaPedido linea) {
        return prendaRepository.findById(linea.getPrendaId())
                .flatMap(co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda::imagenPrincipal)
                .map(co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.ImagenPrenda::getUrl)
                .orElse(null);
    }

    private List<DespachoResponse> despachosDe(Pedido pedido) {
        List<Despacho> despachos = despachoRepository.findAllByPedidoIdOrderByIdAsc(pedido.getId());
        List<DespachoResponse> salida = new ArrayList<>();
        for (Despacho despacho : despachos) {
            List<PedidoDetalleResponse.Item> lineas = pedido.getItems().stream()
                    .filter(l -> l.getDespacho() != null && l.getDespacho().getId().equals(despacho.getId()))
                    .map(l -> PedidoDetalleResponse.Item.from(l, imagenDe(l)))
                    .toList();
            salida.add(DespachoResponse.from(despacho, lineas));
        }
        return salida;
    }

    private static Long clienteActual() {
        return SecurityUtils.usuarioActual()
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado")).id();
    }
}
