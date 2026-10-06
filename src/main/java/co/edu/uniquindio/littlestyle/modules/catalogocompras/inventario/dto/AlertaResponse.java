package co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.ImagenPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.model.AlertaReabastecimiento;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.model.NivelStock;

import java.time.LocalDateTime;

/**
 * Alerta activa con los valores actuales de la prenda. {@code diferencia} es stock actual menos mínimo
 * (negativo o cero cuando hace falta reabastecer), como en UI-22.
 */
public record AlertaResponse(
        Long id,
        Long prendaId,
        String nombre,
        CategoriaPrenda categoria,
        String imagenUrl,
        NivelStock nivel,
        int stockActual,
        int stockMinimo,
        int diferencia,
        LocalDateTime fechaEmision
) {

    public static AlertaResponse from(AlertaReabastecimiento alerta) {
        Prenda p = alerta.getPrenda();
        int actual = p.stockTotal();
        return new AlertaResponse(
                alerta.getId(), p.getId(), p.getNombre(), p.getCategoria(),
                p.imagenPrincipal().map(ImagenPrenda::getUrl).orElse(null),
                alerta.getNivel(), actual, p.getStockMinimo(), actual - p.getStockMinimo(),
                alerta.getFechaEmision());
    }
}
