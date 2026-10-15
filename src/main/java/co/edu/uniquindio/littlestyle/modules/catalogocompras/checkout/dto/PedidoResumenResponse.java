package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PedidoResumenResponse(Long id, EstadoPedido estado, BigDecimal total,
                                    LocalDateTime fechaCreacion, int cantidadItems, String numeroGuia) {

    public static PedidoResumenResponse from(Pedido p) {
        return new PedidoResumenResponse(p.getId(), p.getEstado(), p.getTotal(),
                p.getFechaCreacion(), p.getItems().size(), p.getNumeroGuia());
    }
}
