package co.edu.uniquindio.littlestyle.modules.admin.dto;

import co.edu.uniquindio.littlestyle.modules.admin.model.EstadoIncidencia;
import co.edu.uniquindio.littlestyle.modules.admin.model.Incidencia;
import co.edu.uniquindio.littlestyle.modules.admin.model.TipoIncidencia;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;

import java.time.LocalDateTime;

/**
 * Incidencia visible para el administrador y para su titular (US-14). El usuario solo ve sus propias
 * incidencias, asi que el correo reportante no es informacion sensible en esta pantalla.
 */
public record IncidenciaResponse(
        Long id,
        String reportanteEmail,
        Rol reportanteRol,
        TipoIncidencia tipo,
        Long referenciaId,
        String referenciaDetalle,
        String asunto,
        String descripcion,
        EstadoIncidencia estado,
        String respuesta,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaResolucion) {

    /** Mapeo desde la entidad, compartido por la bandeja del admin y la vista del titular. */
    public static IncidenciaResponse from(Incidencia incidencia) {
        return new IncidenciaResponse(
                incidencia.getId(),
                incidencia.getReportanteEmail(),
                incidencia.getReportanteRol(),
                incidencia.getTipo(),
                incidencia.getReferenciaId(),
                incidencia.getReferenciaDetalle(),
                incidencia.getAsunto(),
                incidencia.getDescripcion(),
                incidencia.getEstado(),
                incidencia.getRespuesta(),
                incidencia.getFechaCreacion(),
                incidencia.getFechaResolucion());
    }
}
