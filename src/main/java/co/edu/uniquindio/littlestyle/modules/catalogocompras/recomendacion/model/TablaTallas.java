package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model;

import java.util.Comparator;
import java.util.List;

/** Tallas de una prenda ordenadas por estatura mínima. */
public record TablaTallas(List<RangoTalla> rangos) {

    /** Tabla de tallas de una prenda, de la más pequeña a la más grande. */
    public TablaTallas{
        rangos = rangos == null ? List.of() : rangos.stream().sorted(Comparator.comparing(RangoTalla::estaturaMinCm)).toList();
    }

    /** Indica si la prenda no tiene tallas. */
    public boolean estaVacia() {
        return rangos.isEmpty();
    }
}
