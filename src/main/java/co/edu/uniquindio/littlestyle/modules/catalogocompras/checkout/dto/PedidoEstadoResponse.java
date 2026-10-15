package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;

import java.time.LocalDateTime;

public record PedidoEstadoResponse(Long id, EstadoPedido estado, String numeroGuia,
                                   LocalDateTime fechaActualizacion) {
}
