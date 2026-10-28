package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model;

/**
 * Estado de moderación posterior de una prenda, decidido por el administrador (US-14) y ortogonal a
 * {@link EstadoPrenda}: la prenda se publica libremente y solo se oculta cuando el administrador la
 * desactiva.
 * @see "US-14"
 * @see "RNF-08"
 */
public enum EstadoModeracion {
    /** Aún no revisada; la prenda permanece publicada mientras no esté desactivada. */
    SIN_REVISAR,
    /** Revisada y publicable junto con el resto de condiciones de visibilidad. */
    APROBADA,
    /** Retirada del consumo público por el administrador; el motivo queda registrado. */
    DESACTIVADA
}
