package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.LineaDisponibilidadPagoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model.EstadoTransaccionPago;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model.MotivoReembolso;

import java.math.BigDecimal;
import java.util.List;

/**
 * Estado de un pago para la página de resultado. Si se requiere reembolso por falta de stock, incluye las
 * prendas agotadas y las líneas que siguen disponibles para volver a comprarlas.
 */
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
    /** Guarda copias inmutables de las listas. */
    public ResultadoPagoResponse {
        prendasAgotadas = List.copyOf(prendasAgotadas);
        lineasDisponibles = List.copyOf(lineasDisponibles);
    }
}
