package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.LineaPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Resumen de pedido para el vendedor: solo cuenta y suma sus propias líneas,
 * aunque el pedido incluya prendas de otras tiendas.
 */
public record PedidoVendedorResumenResponse(Long id, EstadoPedido estado, LocalDateTime fechaCreacion,
                                            int cantidadItemsPropios, BigDecimal subtotalPropio,
                                            String numeroGuia) {

    public static PedidoVendedorResumenResponse from(Pedido pedido, Set<Long> prendasDelVendedor) {
        List<LineaPedido> propias = pedido.getItems().stream()
                .filter(l -> prendasDelVendedor.contains(l.getPrendaId()))
                .toList();
        BigDecimal subtotal = propias.stream().map(LineaPedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PedidoVendedorResumenResponse(pedido.getId(), pedido.getEstado(), pedido.getFechaCreacion(),
                propias.stream().mapToInt(LineaPedido::getCantidad).sum(), subtotal, pedido.getNumeroGuia());
    }
}
