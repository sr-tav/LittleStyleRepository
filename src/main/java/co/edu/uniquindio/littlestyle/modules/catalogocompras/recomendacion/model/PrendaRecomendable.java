package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Estampado;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Vista de una prenda para el motor: tabla de tallas, composición, atributos que afectan alergias, estilo
 * (colores y estampado) y stock por talla.
 */
public record PrendaRecomendable(Long id, String nombre, String categoria, String marca, BigDecimal precio, String imagenUrl,
                                 TablaTallas tablaTallas, List<ComponenteMaterial> composicion, boolean tintesSinteticos, boolean brochesMetalicos,
                                 boolean contieneNiquel, boolean tratamientoFormaldehido, Set<ColorPreferido> colores, Estampado estampado,
                                 Map<String, Integer> stockPorTalla) {

    /** Exige id y nombre; las colecciones nulas quedan vacías e inmutables. */
    public PrendaRecomendable {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(nombre, "nombre");
        tablaTallas = tablaTallas == null ? new TablaTallas(List.of()) : tablaTallas;
        composicion = composicion == null ? List.of() : List.copyOf(composicion);
        colores = colores == null ? Set.of() : Set.copyOf(colores);
        stockPorTalla = stockPorTalla == null ? Map.of() : Map.copyOf(stockPorTalla);
    }

    /** Indica si el vendedor declaró la composición textil. */
    public boolean tieneComposicion() {
        return !composicion.isEmpty();
    }

    /** Indica si la talla tiene al menos una unidad. */
    public boolean tieneStock(String talla) {
        return stockPorTalla.getOrDefault(talla, 0) > 0;
    }
}
