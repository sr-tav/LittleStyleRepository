package co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.model;

/**
 * Estado de disponibilidad de una prenda según su stock total y el mínimo definido por el vendedor.
 * Se alerta cuando el stock alcanza o queda por debajo del mínimo (RNF-14).
 */
public enum NivelStock {
    DISPONIBLE,
    STOCK_BAJO,
    AGOTADO;

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

    public boolean requiereReabastecimiento() {
        return this != DISPONIBLE;
    }
}
