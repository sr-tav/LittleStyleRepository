package co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.ImagenPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.model.AlertaReabastecimiento;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.model.NivelStock;

import java.time.LocalDateTime;

/**
 * Alerta activa con los valores actuales de la talla. {@code diferencia} es stock actual menos mínimo
 * (negativo o cero cuando hace falta reabastecer), como en UI-22.
 */
public record AlertaResponse(
        Long id,
        Long prendaId,
        String nombre,
        CategoriaPrenda categoria,
        String imagenUrl,
        String talla,
        NivelStock nivel,
        int stockActual,
        int stockMinimo,
        int diferencia,
        LocalDateTime fechaEmision
) {

    public static AlertaResponse from(AlertaReabastecimiento alerta) {
        Prenda p = alerta.getPrenda();
        if (alerta.getTalla() == null) {
            // Fila heredada de antes del mínimo por talla.
            return new AlertaResponse(
                    alerta.getId(), p.getId(), p.getNombre(), p.getCategoria(),
                    p.imagenPrincipal().map(ImagenPrenda::getUrl).orElse(null), null,
                    alerta.getNivel(), alerta.getStockAlEmitir(), alerta.getStockMinimoAlEmitir(),
                    alerta.getStockAlEmitir() - alerta.getStockMinimoAlEmitir(),
                    alerta.getFechaEmision());
        }
        var talla = p.getTallas().stream().filter(t -> t.getTalla().equals(alerta.getTalla())).findFirst()
                .orElseThrow(() -> new IllegalStateException("La talla ya no existe"));
        int minimo = talla.getStockMinimo() == null ? 0 : talla.getStockMinimo();
        return new AlertaResponse(
                alerta.getId(), p.getId(), p.getNombre(), p.getCategoria(),
                p.imagenPrincipal().map(ImagenPrenda::getUrl).orElse(null), talla.getTalla(),
                alerta.getNivel(), talla.getStock(), minimo, talla.getStock() - minimo,
                alerta.getFechaEmision());
    }
}
