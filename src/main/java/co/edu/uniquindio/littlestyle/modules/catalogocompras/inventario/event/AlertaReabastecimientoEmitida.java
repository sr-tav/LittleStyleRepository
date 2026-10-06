package co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.event;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.model.NivelStock;

/**
 * Evento de dominio publicado al emitir una alerta de reabastecimiento (ADR-14). Los observadores lo
 * reciben solo si la transacción que cambió el stock se confirma.
 */
public record AlertaReabastecimientoEmitida(
        Long alertaId,
        Long prendaId,
        Long vendedorId,
        String nombrePrenda,
        NivelStock nivel,
        int stockActual,
        int stockMinimo
) {
}
