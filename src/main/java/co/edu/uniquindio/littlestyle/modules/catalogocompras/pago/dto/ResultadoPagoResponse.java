package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.LineaDisponibilidadPagoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model.EstadoTransaccionPago;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model.MotivoReembolso;

import java.math.BigDecimal;
import java.util.List;

public record ResultadoPagoResponse(
        Long pedidoId,
        String referencia,
        EstadoTransaccionPago estadoPago,
        EstadoPedido estadoPedido,
        MotivoReembolso motivoReembolso,
        String metodoPago,
        BigDecimal total,
        List<LineaDisponibilidadPagoResponse> prendasAgotadas,
        List<LineaRecompraResponse> lineasDisponibles
) {
    public ResultadoPagoResponse {
        prendasAgotadas = List.copyOf(prendasAgotadas);
        lineasDisponibles = List.copyOf(lineasDisponibles);
    }
}
