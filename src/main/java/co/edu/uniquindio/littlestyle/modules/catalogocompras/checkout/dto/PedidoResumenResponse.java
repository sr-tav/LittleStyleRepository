package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fila del historial de pedidos del cliente; {@code numeroPedido} es su consecutivo, distinto del id interno. */
public record PedidoResumenResponse(Long id, Long numeroPedido, EstadoPedido estado, BigDecimal total,
                                    LocalDateTime fechaCreacion, int cantidadItems, String numeroGuia) {

    /** Construye la fila a partir del pedido. */
    public static PedidoResumenResponse from(Pedido p) {
        return new PedidoResumenResponse(p.getId(), p.numeroVisible(), p.getEstado(), p.getTotal(),
                p.getFechaCreacion(), p.getItems().size(), p.getNumeroGuia());
    }
}
