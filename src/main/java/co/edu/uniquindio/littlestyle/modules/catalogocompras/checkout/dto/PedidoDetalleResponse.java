package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.LineaPedido;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Estampado;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detalle de un pedido para el cliente: totales, destino, líneas, despachos por vendedor e historial de
 * estados. {@code numeroPedido} es el consecutivo del cliente; {@code id} es el identificador interno.
 */
public record PedidoDetalleResponse(Long id, Long numeroPedido, EstadoPedido estado, BigDecimal subtotal,
                                    BigDecimal costoEnvio, BigDecimal total, LocalDateTime fechaCreacion,
                                    String numeroGuia, String ciudad, String departamento,
                                    List<Item> items, List<DespachoResponse> despachos,
                                    List<Movimiento> historial) {

    /** Línea del pedido con los precios congelados al comprar. */
    public record Item(Long prendaId, String nombre, String talla, int cantidad,
                       BigDecimal precioUnitario, BigDecimal subtotal, String tallaSugerida,
                       String imagenUrl, ColorPreferido color, Estampado estampado) {
        /** Línea sin estampado (pedidos anteriores a la selección de estampado). */
        public Item(Long prendaId, String nombre, String talla, int cantidad, BigDecimal precioUnitario,
                    BigDecimal subtotal, String tallaSugerida, String imagenUrl, ColorPreferido color) {
            this(prendaId, nombre, talla, cantidad, precioUnitario, subtotal, tallaSugerida, imagenUrl, color, null);
        }

        /** Construye la línea sin imagen. */
        public static Item from(LineaPedido l) {
            return new Item(l.getPrendaId(), l.getNombre(), l.getTalla(), l.getCantidad(),
                    l.getPrecioUnitario(), l.getSubtotal(), l.getTallaSugerida(), null, l.getColor(),
                    l.getEstampado());
        }

        /** Construye la línea con la imagen actual de la prenda y el color comprado. */
        public static Item from(LineaPedido l, String imagenUrl) {
            return new Item(l.getPrendaId(), l.getNombre(), l.getTalla(), l.getCantidad(),
                    l.getPrecioUnitario(), l.getSubtotal(), l.getTallaSugerida(), imagenUrl, l.getColor(),
                    l.getEstampado());
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
