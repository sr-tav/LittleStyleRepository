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
        int stockMinimo,
        NivelStock nivel,
        List<StockTalla> tallas
) {

    public record StockTalla(String talla, int stock) {
    }

    public static InventarioItemResponse from(Prenda p) {
        int total = p.stockTotal();
        return new InventarioItemResponse(
                p.getId(), p.getNombre(), p.getCategoria(),
                p.imagenPrincipal().map(ImagenPrenda::getUrl).orElse(null),
                p.getEstado(), total, p.getStockMinimo(), NivelStock.calcular(total, p.getStockMinimo()),
                p.getTallas().stream().map(t -> new StockTalla(t.getTalla(), t.getStock())).toList());
    }
}
