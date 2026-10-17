package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.LineaPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoDetalleResponse(Long id, EstadoPedido estado, BigDecimal subtotal,
                                    BigDecimal costoEnvio, BigDecimal total, LocalDateTime fechaCreacion,
                                    String numeroGuia, String ciudad, String departamento,
                                    List<Item> items, List<DespachoResponse> despachos,
                                    List<Movimiento> historial) {

    public record Item(Long prendaId, String nombre, String talla, int cantidad,
                       BigDecimal precioUnitario, BigDecimal subtotal, String tallaSugerida,
                       String imagenUrl) {
        public static Item from(LineaPedido l) {
            return new Item(l.getPrendaId(), l.getNombre(), l.getTalla(), l.getCantidad(),
                    l.getPrecioUnitario(), l.getSubtotal(), l.getTallaSugerida(), null);
        }

        public static Item from(LineaPedido l, String imagenUrl) {
            return new Item(l.getPrendaId(), l.getNombre(), l.getTalla(), l.getCantidad(),
                    l.getPrecioUnitario(), l.getSubtotal(), l.getTallaSugerida(), imagenUrl);
        }
    }

    public record Movimiento(EstadoPedido anterior, EstadoPedido nuevo, LocalDateTime fecha,
                             String numeroGuia) {
        public Movimiento(EstadoPedido anterior, EstadoPedido nuevo, LocalDateTime fecha) {
            this(anterior, nuevo, fecha, null);
        }
    }
}
