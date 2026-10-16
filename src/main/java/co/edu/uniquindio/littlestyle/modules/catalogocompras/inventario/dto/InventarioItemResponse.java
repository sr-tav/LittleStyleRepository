package co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.ImagenPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.model.NivelStock;

import java.util.List;

public record InventarioItemResponse(
        Long prendaId,
        String nombre,
        CategoriaPrenda categoria,
        String imagenUrl,
        EstadoPrenda estado,
        int stockTotal,
        NivelStock nivel,
        List<StockTalla> tallas
) {

    public record StockTalla(String talla, int stock, Integer stockMinimo) {
    }

    public static InventarioItemResponse from(Prenda p) {
        return new InventarioItemResponse(
                p.getId(), p.getNombre(), p.getCategoria(),
                p.imagenPrincipal().map(ImagenPrenda::getUrl).orElse(null),
                p.getEstado(), p.stockTotal(), nivelDe(p),
                p.getTallas().stream()
                        .map(t -> new StockTalla(t.getTalla(), t.getStock(), t.getStockMinimo())).toList());
    }

    /** Peor nivel entre las tallas con mínimo; sin mínimos siempre disponible. */
    static NivelStock nivelDe(Prenda p) {
        return NivelStock.peor(p.getTallas().stream()
                .filter(t -> t.getStockMinimo() != null)
                .map(t -> NivelStock.calcular(t.getStock(), t.getStockMinimo()))
                .toList());
    }
}
