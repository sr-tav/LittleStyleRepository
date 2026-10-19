package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.LineaPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detalle de un pedido para el cliente: totales, destino, líneas, despachos por vendedor e historial de
 * estados.
 */
public record PedidoDetalleResponse(Long id, EstadoPedido estado, BigDecimal subtotal,
                                    BigDecimal costoEnvio, BigDecimal total, LocalDateTime fechaCreacion,
                                    String numeroGuia, String ciudad, String departamento,
                                    List<Item> items, List<DespachoResponse> despachos,
                                    List<Movimiento> historial) {

    /** Línea del pedido con los precios congelados al comprar. */
    public record Item(Long prendaId, String nombre, String talla, int cantidad,
                       BigDecimal precioUnitario, BigDecimal subtotal, String tallaSugerida,
                       String imagenUrl) {
        /** Construye la línea sin imagen. */
        public static Item from(LineaPedido l) {
            return new Item(l.getPrendaId(), l.getNombre(), l.getTalla(), l.getCantidad(),
                    l.getPrecioUnitario(), l.getSubtotal(), l.getTallaSugerida(), null);
        }

        /** Construye la línea con la imagen actual de la prenda. */
        public static Item from(LineaPedido l, String imagenUrl) {
            return new Item(l.getPrendaId(), l.getNombre(), l.getTalla(), l.getCantidad(),
                    l.getPrecioUnitario(), l.getSubtotal(), l.getTallaSugerida(), imagenUrl);
        }
    }

    /** Cambio de estado del historial, con la guía si fue el paso a enviado. */
    public record Movimiento(EstadoPedido anterior, EstadoPedido nuevo, LocalDateTime fecha,
                             String numeroGuia) {
        /** Movimiento sin número de guía. */
        public Movimiento(EstadoPedido anterior, EstadoPedido nuevo, LocalDateTime fecha) {
            this(anterior, nuevo, fecha, null);
        }
    }
}
