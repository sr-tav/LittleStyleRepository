package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.ImagenPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.TallaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.model.NivelStock;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.MaterialTextil;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Estampado;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public record PrendaResponse(
        Long id,
        String nombre,
        CategoriaPrenda categoria,
        String descripcion,
        String marca,
        BigDecimal precio,
        EstadoPrenda estado,
        int stockMinimo,
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

    public record Componente(MaterialTextil material, int porcentaje) {
    }

    public record Talla(Long id, String talla, BigDecimal estaturaMinCm, BigDecimal estaturaMaxCm,
                        BigDecimal pesoMinKg, BigDecimal pesoMaxKg, int stock) {
        static Talla from(TallaPrenda t) {
            return new Talla(t.getId(), t.getTalla(), t.getEstaturaMinCm(), t.getEstaturaMaxCm(),
                    t.getPesoMinKg(), t.getPesoMaxKg(), t.getStock());
        }
    }

    public record Imagen(Long id, String url, int orden) {
        public static Imagen from(ImagenPrenda i) {
            return new Imagen(i.getId(), i.getUrl(), i.getOrden());
        }
    }

    public static PrendaResponse from(Prenda p) {
        int stockTotal = p.stockTotal();
        return new PrendaResponse(
                p.getId(), p.getNombre(), p.getCategoria(), p.getDescripcion(), p.getMarca(), p.getPrecio(),
                p.getEstado(), p.getStockMinimo(), stockTotal, NivelStock.calcular(stockTotal, p.getStockMinimo()),
                new TreeSet<>(p.getColores()), p.getEstampado(),
                p.isTintesSinteticos(), p.isBrochesMetalicos(), p.isContieneNiquel(), p.isTratamientoFormaldehido(),
                p.getComposicion().stream().map(c -> new Componente(c.getMaterial(), c.getPorcentaje())).toList(),
                p.getTallas().stream().map(Talla::from).toList(),
                p.getImagenes().stream().map(Imagen::from).toList(),
                p.getFechaCreacion(), p.getFechaActualizacion());
    }
}
