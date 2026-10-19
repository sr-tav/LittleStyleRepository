package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.GeneroPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.ImagenPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.TallaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.model.NivelStock;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.MaterialTextil;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Estampado;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Prenda completa para el vendedor: datos, estado, stock total y nivel, atributos de seguridad textil,
 * composición, tallas con stock y mínimo, e imágenes.
 */
public record PrendaResponse(
        Long id,
        String nombre,
        CategoriaPrenda categoria,
        GeneroPrenda genero,
        String descripcion,
        String instruccionesCuidado,
        String marca,
        BigDecimal precio,
        EstadoPrenda estado,
        int stockTotal,
        NivelStock nivelStock,
        Set<ColorPreferido> colores,
        Estampado estampado,
        boolean tintesSinteticos,
        boolean brochesMetalicos,
        boolean contieneNiquel,
        boolean tratamientoFormaldehido,
        List<Componente> composicion,
        List<Talla> tallas,
        List<Imagen> imagenes,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {

    /** Material y porcentaje de la composición. */
    public record Componente(MaterialTextil material, int porcentaje) {
    }

    /** Talla con sus rangos, stock y stock mínimo. */
    public record Talla(Long id, String talla, BigDecimal estaturaMinCm, BigDecimal estaturaMaxCm,
                        BigDecimal pesoMinKg, BigDecimal pesoMaxKg, Integer edadMinMeses, Integer edadMaxMeses,
                        int stock, Integer stockMinimo) {
        /** Construye la talla a partir de la entidad. */
        static Talla from(TallaPrenda t) {
            return new Talla(t.getId(), t.getTalla(), t.getEstaturaMinCm(), t.getEstaturaMaxCm(),
                    t.getPesoMinKg(), t.getPesoMaxKg(), t.getEdadMinMeses(), t.getEdadMaxMeses(),
                    t.getStock(), t.getStockMinimo());
        }
    }

    /** Foto de la prenda con su posición; la de orden 0 es la principal. */
    public record Imagen(Long id, String url, int orden) {
        /** Construye la imagen a partir de la entidad. */
        public static Imagen from(ImagenPrenda i) {
            return new Imagen(i.getId(), i.getUrl(), i.getOrden());
        }
    }

    /** Construye la respuesta a partir de la prenda y sus colecciones. */
    public static PrendaResponse from(Prenda p) {
        int stockTotal = p.stockTotal();
        return new PrendaResponse(
                p.getId(), p.getNombre(), p.getCategoria(), p.getGenero(), p.getDescripcion(), p.getInstruccionesCuidado(), p.getMarca(), p.getPrecio(),
                p.getEstado(), stockTotal, nivelDe(p),
                new TreeSet<>(p.getColores()), p.getEstampado(),
                p.isTintesSinteticos(), p.isBrochesMetalicos(), p.isContieneNiquel(), p.isTratamientoFormaldehido(),
                p.getComposicion().stream().map(c -> new Componente(c.getMaterial(), c.getPorcentaje())).toList(),
                p.getTallas().stream().map(Talla::from).toList(),
                p.getImagenes().stream().map(Imagen::from).toList(),
                p.getFechaCreacion(), p.getFechaActualizacion());
    }

    /** Peor nivel entre las tallas con mínimo; sin mínimos siempre disponible. */
    static NivelStock nivelDe(Prenda p) {
        return NivelStock.peor(p.getTallas().stream()
                .filter(t -> t.getStockMinimo() != null)
                .map(t -> NivelStock.calcular(t.getStock(), t.getStockMinimo()))
                .toList());
    }
}
