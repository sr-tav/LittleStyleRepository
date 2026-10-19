package co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.ImagenPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.TallaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.model.AlertaReabastecimiento;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.model.NivelStock;

import java.time.LocalDateTime;
import java.util.Optional;

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

    /**
     * Construye la alerta con el stock y el mínimo actuales de la talla. Las alertas heredadas sin talla, o
     * de una talla que ya se quitó de la prenda, usan los valores guardados al emitirlas en vez de fallar.
     */
    public static AlertaResponse from(AlertaReabastecimiento alerta) {
        Prenda p = alerta.getPrenda();
        var tallaActual = alerta.getTalla() == null ? Optional.<TallaPrenda>empty()
                : p.getTallas().stream().filter(t -> t.getTalla().equals(alerta.getTalla())).findFirst();
        if (tallaActual.isEmpty()) {
            return new AlertaResponse(
                    alerta.getId(), p.getId(), p.getNombre(), p.getCategoria(),
                    p.imagenPrincipal().map(ImagenPrenda::getUrl).orElse(null), alerta.getTalla(),
                    alerta.getNivel(), alerta.getStockAlEmitir(), alerta.getStockMinimoAlEmitir(),
                    alerta.getStockAlEmitir() - alerta.getStockMinimoAlEmitir(),
                    alerta.getFechaEmision());
        }
        var talla = tallaActual.get();
        int minimo = talla.getStockMinimo() == null ? 0 : talla.getStockMinimo();
        return new AlertaResponse(
                alerta.getId(), p.getId(), p.getNombre(), p.getCategoria(),
                p.imagenPrincipal().map(ImagenPrenda::getUrl).orElse(null), talla.getTalla(),
                alerta.getNivel(), talla.getStock(), minimo, talla.getStock() - minimo,
                alerta.getFechaEmision());
    }
}
