package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.GeneroPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.ImagenPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tarjeta de la vitrina del cliente (base de US-09). No expone datos internos
 * del vendedor: ni stock mínimo ni existencias exactas, solo tallas disponibles.
 */
public record PrendaVitrinaResponse(
        Long id,
        String nombre,
        CategoriaPrenda categoria,
        GeneroPrenda genero,
        String marca,
        BigDecimal precio,
        String imagenUrl,
        boolean disponible,
        List<String> tallasDisponibles
) {

    public static PrendaVitrinaResponse from(Prenda p) {
        return new PrendaVitrinaResponse(
                p.getId(), p.getNombre(), p.getCategoria(), p.getGenero(), p.getMarca(), p.getPrecio(),
                p.imagenPrincipal().map(ImagenPrenda::getUrl).orElse(null),
                p.stockTotal() > 0,
                p.getTallas().stream().filter(talla -> talla.getStock() > 0).map(talla -> talla.getTalla()).toList());
    }
}
