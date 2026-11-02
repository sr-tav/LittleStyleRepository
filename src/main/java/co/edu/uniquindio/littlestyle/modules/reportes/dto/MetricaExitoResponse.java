package co.edu.uniquindio.littlestyle.modules.reportes.dto;

import java.time.Instant;

/**
 * Foto de la tasa de éxito de exportaciones del servidor (RNF-24: estrictamente más del 98 %).
 *
 * <p>{@code tasaExito} es una fracción entre 0 y 1, o {@code null} cuando todavía no hay
 * evidencia ({@code ok + error == 0}). En ese caso {@code estado} es {@link EstadoRnf24#SIN_DATOS}
 * y {@code cumpleRnf24} es {@code false}: «sin datos» no es «cumple». El umbral se aplica con
 * {@code > 0.98}, no {@code >=}: un 98,00 % exacto todavía no cumple. {@code desde} es el instante
 * en que arrancó el proceso; los contadores de Micrometer se reinician con cada despliegue.</p>
 */
public record MetricaExitoResponse(long ok, long error, long rechazado, Double tasaExito,
                                   EstadoRnf24 estado, boolean cumpleRnf24, Instant desde) {
}
