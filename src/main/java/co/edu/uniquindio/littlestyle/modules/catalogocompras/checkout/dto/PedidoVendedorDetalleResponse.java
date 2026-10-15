package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.LineaPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Detalle para el vendedor: solo sus líneas, pero con la dirección completa
 * porque debe preparar y despachar el paquete. Incluye el historial del pedido.
 */
public record PedidoVendedorDetalleResponse(Long id, EstadoPedido estado, LocalDateTime fechaCreacion,
                                            String numeroGuia, String destinatario, String direccion,
                                            String complemento, String codigoPostal, String ciudad,
                                            String departamento, String telefono,
                                            List<PedidoDetalleResponse.Item> items,
                                            List<PedidoDetalleResponse.Movimiento> historial) {

    public static PedidoVendedorDetalleResponse from(Pedido pedido, Set<Long> prendasDelVendedor,
                                                     List<PedidoDetalleResponse.Movimiento> historial) {
        List<PedidoDetalleResponse.Item> propias = pedido.getItems().stream()
                .filter(l -> prendasDelVendedor.contains(l.getPrendaId()))
                .map(PedidoDetalleResponse.Item::from)
                .toList();
        return new PedidoVendedorDetalleResponse(pedido.getId(), pedido.getEstado(), pedido.getFechaCreacion(),
                pedido.getNumeroGuia(), pedido.getDestinatario(), pedido.getDireccion(), pedido.getComplemento(),
                pedido.getCodigoPostal(), pedido.getCiudad(), pedido.getDepartamento(), pedido.getTelefono(),
                propias, historial);
    }
}
