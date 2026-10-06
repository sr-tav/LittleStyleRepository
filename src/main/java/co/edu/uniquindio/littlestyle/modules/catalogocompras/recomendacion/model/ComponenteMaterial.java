package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model;

public record ComponenteMaterial(MaterialTextil material, int porcentaje) {
    public ComponenteMaterial {
        if (material == null) {
            throw new IllegalArgumentException("El material es obligatorio");
        }
        if (porcentaje  < 1 || porcentaje > 100) {
            throw new IllegalArgumentException("El porcentaje debe estar entre  1 y 100");
        }
    }
}
