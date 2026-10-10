package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.LineaDisponibilidadPagoResponse;

import java.util.List;

public record ResultadoConfirmacionPago(
        Estado estado,
        List<LineaDisponibilidadPagoResponse> prendasAgotadas
) {
    public ResultadoConfirmacionPago {
        prendasAgotadas = List.copyOf(prendasAgotadas);
    }

    public enum Estado {
        CONFIRMADO,
        STOCK_INSUFICIENTE,
        PEDIDO_VENCIDO
    }
}
