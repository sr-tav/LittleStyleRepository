package co.edu.uniquindio.littlestyle.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Cuerpo JSON de todas las respuestas de error de la API. {@code codigo} identifica errores que el frontend
 * trata de forma especial y {@code errores} asocia mensajes a campos del formulario; los nulos se omiten.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String mensaje,
        String path,
        String codigo,
        Map<String, String> errores
) {
    /** Crea un error sin código, con la hora actual. */
    public static ErrorResponse of(int status, String error, String mensaje, String path,
                                   Map<String, String> errores) {
        return new ErrorResponse(Instant.now(), status, error, mensaje, path, null, errores);
    }

    /** Crea un error con código de negocio, con la hora actual. */
    public static ErrorResponse of(int status, String error, String mensaje, String path,
                                   String codigo, Map<String, String> errores) {
        return new ErrorResponse(Instant.now(), status, error, mensaje, path, codigo, errores);
    }
}
