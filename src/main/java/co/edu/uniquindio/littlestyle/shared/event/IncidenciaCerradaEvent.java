package co.edu.uniquindio.littlestyle.shared.event;

/**
 * Publicada cuando el administrador resuelve o rechaza una incidencia; porta la respuesta final para
 * que el usuario reportante reciba el correo de cierre.
 */
public record IncidenciaCerradaEvent(Long incidenciaId, Long reportanteId, String reportanteEmail,
                                     String asunto, boolean resuelta, String respuesta) {
}
