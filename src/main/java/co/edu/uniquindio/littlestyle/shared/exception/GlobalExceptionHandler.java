package co.edu.uniquindio.littlestyle.shared.exception;

import co.edu.uniquindio.littlestyle.shared.dto.ErrorResponse;
import co.edu.uniquindio.littlestyle.shared.util.SecurityUtils;
import co.edu.uniquindio.littlestyle.shared.util.SeguridadLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Convierte las excepciones de los controladores en respuestas {@link ErrorResponse} con el código HTTP
 * adecuado y mensajes en español, sin exponer detalles internos.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Errores de Bean Validation: 400 con un mensaje por campo, priorizando el de campo obligatorio. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            // Si un campo viola varias reglas, el mensaje de "obligatorio" tiene prioridad
            boolean obligatorio = "NotBlank".equals(fe.getCode()) || "NotNull".equals(fe.getCode());
            if (obligatorio) {
                errores.put(fe.getField(), fe.getDefaultMessage());
            } else {
                errores.putIfAbsent(fe.getField(), fe.getDefaultMessage());
            }
        }
        return build(HttpStatus.BAD_REQUEST, "Hay campos con errores de validación", req, errores);
    }

    /** JSON mal formado o con tipos incorrectos: 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido", req, null);
    }

    /** Archivos más grandes que el límite de carga: 413. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUpload(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return build(HttpStatus.CONTENT_TOO_LARGE, "Los archivos superan el tamaño máximo permitido", req, null);
    }

    /** Carga multipart sin el archivo esperado: 400 sobre esa parte. */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Selecciona al menos un archivo", req,
                Map.of(ex.getRequestPartName(), "Selecciona al menos un archivo"));
    }

    /** Petición multipart inválida: 400. */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipart(MultipartException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "La carga de archivos no es válida", req, null);
    }

    /** Falta un parámetro obligatorio de la URL: 400. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex,
                                                            HttpServletRequest req) {
        // Antes caía en handleGeneric con 500 (RNF-29): p. ej. GET
        // /recomendaciones sin perfilId ahora responde 400 con el parámetro.
        return build(HttpStatus.BAD_REQUEST, "Falta el parámetro '" + ex.getParameterName() + "'",
                req, Map.of(ex.getParameterName(), "Este parámetro es obligatorio"));
    }

    /** Parámetro de ruta o de consulta con formato inválido (p. ej. texto donde se espera un número): 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest req) {
        String campo = ex.getName();
        return build(HttpStatus.BAD_REQUEST, "El parámetro '" + campo + "' no tiene un formato válido",
                req, Map.of(campo, "Revisa el valor enviado"));
    }

    /** Reglas de negocio: usa el estado, el campo y el código definidos en la {@link BusinessException}. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest req) {
        Map<String, String> errores = ex.getCampo() == null ? null : Map.of(ex.getCampo(), ex.getMessage());
        ErrorResponse body = ErrorResponse.of(ex.getStatus().value(), ex.getStatus().getReasonPhrase(),
                ex.getMessage(), req.getRequestURI(), ex.getCodigo(), errores);
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    /** Login bloqueado por intentos fallidos: 429 con la cabecera {@code Retry-After} en segundos. */
    @ExceptionHandler(LoginBloqueadoException.class)
    public ResponseEntity<ErrorResponse> handleLoginBloqueado(LoginBloqueadoException ex, HttpServletRequest req) {
        ErrorResponse body = ErrorResponse.of(ex.getStatus().value(), ex.getStatus().getReasonPhrase(),
                ex.getMessage(), req.getRequestURI(), null);
        return ResponseEntity.status(ex.getStatus())
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getSegundosRestantes()))
                .body(body);
    }

    /** Correo o contraseña incorrectos: 401. */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos", req, null);
    }

    /** Login de una cuenta suspendida o eliminada: 403. */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "La cuenta está suspendida o eliminada y no puede iniciar sesión",
                req, null);
    }

    /** Otros fallos de autenticación: 401, registrados en el log de seguridad. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest req) {
        SeguridadLog.LOG.warn("Autenticación rechazada: {} {} ip={} causa={}", req.getMethod(), req.getRequestURI(),
                req.getRemoteAddr(), ex.getClass().getSimpleName());
        return build(HttpStatus.UNAUTHORIZED, "No fue posible autenticar al usuario", req, null);
    }

    /** Violación de restricciones de la base de datos (p. ej. duplicados por carrera): 409. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.error("Violación de integridad de datos en {}", req.getRequestURI(), ex);
        return build(HttpStatus.CONFLICT, "El registro entra en conflicto con datos existentes", req, null);
    }

    /** Rol sin permiso sobre el recurso: 403. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        // Rechazos de seguridad a nivel de método (@PreAuthorize), que no pasan por RestAccessDeniedHandler
        SeguridadLog.LOG.warn("Acceso rechazado (403): {} {} ip={} {}", req.getMethod(), req.getRequestURI(),
                req.getRemoteAddr(), SecurityUtils.describirUsuarioActual());
        return build(HttpStatus.FORBIDDEN, "No tiene permisos para acceder a este recurso", req, null);
    }

    /** Cualquier otro error: 500 con un mensaje genérico; el detalle solo queda en el log. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Error no controlado en {}", req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado", req, null);
    }

    /** Arma la respuesta de error sin código de negocio. */
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String mensaje, HttpServletRequest req,
                                                Map<String, String> errores) {
        ErrorResponse body = ErrorResponse.of(status.value(), status.getReasonPhrase(), mensaje,
                req.getRequestURI(), errores);
        return ResponseEntity.status(status).body(body);
    }
}
