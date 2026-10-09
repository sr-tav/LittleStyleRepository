package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.GeneroPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.ImagenPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.TallaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.MaterialTextil;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Estampado;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Ficha del producto para el cliente (base de US-09, ficha técnica textil).
 * Incluye la tabla de tallas con disponibilidad por talla, sin revelar
 * existencias exactas ni el stock mínimo del vendedor.
 */
public record PrendaDetalleResponse(
        Long id,
        String nombre,
        CategoriaPrenda categoria,
        GeneroPrenda genero,
        String descripcion,
        String marca,
        BigDecimal precio,
        List<String> imagenes,
        boolean disponible,
        Set<ColorPreferido> colores,
        Estampado estampado,
        List<Componente> composicion,
        List<TallaDisponible> tallas,
        LocalDateTime fechaActualizacion
) {

    public record Componente(MaterialTextil material, int porcentaje) {
    }

    public record TallaDisponible(String talla, BigDecimal estaturaMinCm, BigDecimal estaturaMaxCm,
                                  BigDecimal pesoMinKg, BigDecimal pesoMaxKg, boolean disponible) {
        static TallaDisponible from(TallaPrenda t) {
            return new TallaDisponible(t.getTalla(), t.getEstaturaMinCm(), t.getEstaturaMaxCm(),
                    t.getPesoMinKg(), t.getPesoMaxKg(), t.getStock() > 0);
        }
    }

    public static PrendaDetalleResponse from(Prenda p) {
        return new PrendaDetalleResponse(
                p.getId(), p.getNombre(), p.getCategoria(), p.getGenero(), p.getDescripcion(), p.getMarca(), p.getPrecio(),
                p.getImagenes().stream().map(ImagenPrenda::getUrl).toList(),
                p.stockTotal() > 0,
                new TreeSet<>(p.getColores()), p.getEstampado(),
                p.getComposicion().stream().map(c -> new Componente(c.getMaterial(), c.getPorcentaje())).toList(),
                p.getTallas().stream().map(TallaDisponible::from).toList(),
                p.getFechaActualizacion());
    }
}
