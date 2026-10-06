package co.edu.uniquindio.littlestyle.config.security;

import co.edu.uniquindio.littlestyle.shared.dto.ErrorResponse;
import co.edu.uniquindio.littlestyle.shared.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static co.edu.uniquindio.littlestyle.shared.util.SeguridadLog.LOG;

/**
 * Responde 403 en JSON cuando el usuario está autenticado pero su rol no tiene acceso al recurso.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    public RestAccessDeniedHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        LOG.warn("Acceso rechazado (403): {} {} ip={} {}", request.getMethod(), request.getRequestURI(),
                request.getRemoteAddr(), SecurityUtils.describirUsuarioActual());
        HttpStatus status = HttpStatus.FORBIDDEN;
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getOutputStream(),
                ErrorResponse.of(status.value(), status.getReasonPhrase(),
                        "No tiene permisos para acceder a este recurso", request.getRequestURI(), null));
    }
}
