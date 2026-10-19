package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Despacho;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Paquete de un vendedor dentro de un pedido (US-12 multi-vendedor).
 * El vendedor solo ve su despacho, con su estado, su guía y sus productos.
 */
public record DespachoResponse(Long despachoId, Long pedidoId, Long vendedorId, String tienda,
                               EstadoPedido estado, String numeroGuia, LocalDateTime fechaActualizacion,
                               List<PedidoDetalleResponse.Item> items) {

    /** Construye la respuesta con el nombre de la tienda (o del vendedor si no tiene tienda). */
    public static DespachoResponse from(Despacho despacho, List<PedidoDetalleResponse.Item> items) {
        String tienda = despacho.getVendedor().getNombreTienda() != null
                ? despacho.getVendedor().getNombreTienda()
                : despacho.getVendedor().getNombre();
        return new DespachoResponse(despacho.getId(), despacho.getPedido().getId(),
                despacho.getVendedor().getId(), tienda, despacho.getEstado(),
                despacho.getNumeroGuia(), despacho.getFechaActualizacion(), items);
    }
}
