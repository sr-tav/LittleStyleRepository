package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model;

/**
 * Materiales textiles de la composición de una prenda (US-08).
 * Vive en el módulo de catálogo porque es la dueña de la ficha textil;
 * el motor de recomendación (US-07) lo consume desde aquí para no crear
 * una dependencia cíclica entre módulos (ADR-02).
 */
public enum MaterialTextil {
    ALGODON,
    POLIESTER,
    NYLON,
    ACRILICO,
    ELASTANO,
    LANA,
    VISCOSA,
    CUERO,
    LATEX,
    OTRO
}
