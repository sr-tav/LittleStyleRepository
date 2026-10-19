package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.MaterialTextil;

/** Material y porcentaje de la composición de una prenda. */
public record ComponenteMaterial(MaterialTextil material, int porcentaje) {
    /** Exige el material y un porcentaje entre 1 y 100. */
    public ComponenteMaterial {
        if (material == null) {
            throw new IllegalArgumentException("El material es obligatorio");
        }
        if (porcentaje  < 1 || porcentaje > 100) {
            throw new IllegalArgumentException("El porcentaje debe estar entre  1 y 100");
        }
    }
}
