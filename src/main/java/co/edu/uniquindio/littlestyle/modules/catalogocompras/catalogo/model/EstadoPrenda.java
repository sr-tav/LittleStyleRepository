package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model;

/**
 * Solo las prendas {@code ACTIVA} se publican en el catálogo y en el motor de recomendaciones.
 * Las prendas no se eliminan: se desactivan, para conservar la trazabilidad con alertas y pedidos.
 */
public enum EstadoPrenda {
    ACTIVA,
    INACTIVA
}
