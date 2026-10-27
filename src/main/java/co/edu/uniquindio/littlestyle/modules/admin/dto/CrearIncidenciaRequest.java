package co.edu.uniquindio.littlestyle.modules.admin.dto;

import co.edu.uniquindio.littlestyle.modules.admin.model.TipoIncidencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Reporte de incidencia desde la vista del cliente o vendedor (US-14). {@code APELACION_SUSPENSION} queda
 * reservado para el sistema (se abre sola al suspender o bloquear); el usuario apela por SOPORTE_GENERAL.
 * {@code referenciaId} solo aplica a apelaciones de producto (id de la prenda afectada).
 */
public record CrearIncidenciaRequest(
        @NotNull(message = "El tipo de incidencia es obligatorio")
        TipoIncidencia tipo,
        @NotBlank(message = "El asunto es obligatorio")
        @Size(min = 5, max = 200, message = "El asunto debe tener entre 5 y 200 caracteres")
        String asunto,
        @NotBlank(message = "La descripcion es obligatoria")
        @Size(min = 20, max = 4000, message = "La descripcion debe tener entre 20 y 4000 caracteres")
        String descripcion,
        Long referenciaId,
        @Size(max = 500, message = "El detalle de la referencia no puede superar 500 caracteres")
        String referenciaDetalle) {
}
