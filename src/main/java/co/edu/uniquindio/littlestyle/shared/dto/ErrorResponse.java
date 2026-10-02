package co.edu.uniquindio.littlestyle.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String mensaje,
        String path,
        Map<String, String> errores
) {
    public static ErrorResponse of(int status, String error, String mensaje, String path,
                                   Map<String, String> errores) {
        return new ErrorResponse(Instant.now(), status, error, mensaje, path, errores);
    }
}
