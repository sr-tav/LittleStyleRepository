package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.LineaDisponibilidadPagoResponse;

import java.util.List;

/** Resultado de confirmar un pago: confirmado o cancelado por falta de stock, con las prendas agotadas. */
public record ResultadoConfirmacionPago(
        Estado estado,
        List<LineaDisponibilidadPagoResponse> prendasAgotadas
) {
    public ResultadoConfirmacionPago {
        prendasAgotadas = List.copyOf(prendasAgotadas);
    }

    /** Desenlace de la confirmación. */
    public enum Estado {
        CONFIRMADO,
        STOCK_INSUFICIENTE
    }
}
