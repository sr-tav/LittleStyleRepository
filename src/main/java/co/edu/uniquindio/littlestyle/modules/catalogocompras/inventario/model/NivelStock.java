package co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.model;

/**
 * Estado de disponibilidad de una prenda según su stock total y el mínimo definido por el vendedor.
 * Se alerta cuando el stock alcanza o queda por debajo del mínimo (RNF-14).
 */
public enum NivelStock {
    DISPONIBLE,
    STOCK_BAJO,
    AGOTADO;

    /** Nivel para un stock y un mínimo: sin stock es agotado y en el mínimo o por debajo es stock bajo. */
    public static NivelStock calcular(int stockTotal, int stockMinimo) {
        if (stockTotal <= 0) {
            return AGOTADO;
        }
        return stockTotal <= stockMinimo ? STOCK_BAJO : DISPONIBLE;
    }

    /** Peor nivel de un conjunto (para el badge del producto): AGOTADO manda. */
    public static NivelStock peor(java.util.Collection<NivelStock> niveles) {
        if (niveles.contains(AGOTADO)) return AGOTADO;
        if (niveles.contains(STOCK_BAJO)) return STOCK_BAJO;
        return DISPONIBLE;
    }

    /** Indica si el nivel debe generar una alerta (stock bajo o agotado). */
    public boolean requiereReabastecimiento() {
        return this != DISPONIBLE;
    }
}
