package co.edu.uniquindio.littlestyle.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Respuesta del administrador al cierre de una incidencia (resuelta o rechazada), US-14. */
public record ResolucionIncidenciaRequest(
        @NotBlank(message = "La respuesta es obligatoria")
        @Size(min = 10, max = 4000, message = "La respuesta debe tener entre 10 y 4000 caracteres")
        String respuesta) {
}
