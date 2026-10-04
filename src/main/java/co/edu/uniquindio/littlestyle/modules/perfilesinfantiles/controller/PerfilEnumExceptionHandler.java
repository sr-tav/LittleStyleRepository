package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.controller;

import co.edu.uniquindio.littlestyle.shared.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackageClasses = PerfilInfantilController.class)
public class PerfilEnumExceptionHandler {

    private static final Map<String, String> MENSAJES = Map.of(
            "alergias", "Seleccione una alergia válida",
            "coloresPreferidos", "Seleccione un color válido",
            "estampadosPreferidos", "Seleccione un estampado válido");

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleEnumInvalido(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        String campo = campoEnError(ex);
        Map<String, String> errores = campo != null && MENSAJES.containsKey(campo)
                ? Map.of(campo, MENSAJES.get(campo))
                : null;
        String mensaje = errores == null
                ? "El cuerpo de la petición no es válido"
                : "Hay campos con errores de validación";
        ErrorResponse body = ErrorResponse.of(HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), mensaje, request.getRequestURI(), errores);
        return ResponseEntity.badRequest().body(body);
    }

    private String campoEnError(HttpMessageNotReadableException error) {
        StringBuilder detalle = new StringBuilder();
        Throwable actual = error;
        while (actual != null) {
            detalle.append(actual.getMessage()).append(' ');
            actual = actual.getCause();
        }
        return MENSAJES.keySet().stream()
                .filter(campo -> detalle.indexOf(campo) >= 0)
                .findFirst()
                .orElse(null);
    }
}
