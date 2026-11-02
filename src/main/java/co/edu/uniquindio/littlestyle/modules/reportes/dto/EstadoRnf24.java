package co.edu.uniquindio.littlestyle.modules.reportes.dto;

/**
 * Estado auditable del RNF-24 («más del 98 % de las exportaciones deben finalizar correctamente»).
 *
 * <p>{@link #SIN_DATOS} existe para no confundir «aún no hay evidencia» con «se cumple». Un
 * auditor necesita distinguir las tres situaciones: sin solicitudes registradas, con solicitudes
 * por debajo del umbral y con solicitudes por encima del umbral.</p>
 */
public enum EstadoRnf24 {
    CUMPLE,
    NO_CUMPLE,
    SIN_DATOS
}
