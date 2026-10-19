package co.edu.uniquindio.littlestyle.config.security;

import co.edu.uniquindio.littlestyle.shared.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static co.edu.uniquindio.littlestyle.shared.util.SeguridadLog.LOG;

/**
 * Responde 401 en JSON cuando una petición a un endpoint protegido no trae un JWT válido.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper;

    /** Recibe el mapeador JSON con el que se escribe la respuesta de error. */
    public RestAuthenticationEntryPoint(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * Escribe un {@link ErrorResponse} 401 con la causa que dejó el filtro JWT (sesión expirada, token
     * inválido o cuenta inactiva) o un mensaje genérico si no hubo token.
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object jwtError = request.getAttribute(JwtAuthenticationFilter.JWT_ERROR_ATTRIBUTE);
        String mensaje = jwtError != null ? jwtError.toString() : "Debe iniciar sesión para acceder a este recurso";
        LOG.warn("Acceso rechazado (401): {} {} ip={} motivo=\"{}\"",
                request.getMethod(), request.getRequestURI(), request.getRemoteAddr(), mensaje);

        HttpStatus status = HttpStatus.UNAUTHORIZED;
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getOutputStream(),
                ErrorResponse.of(status.value(), status.getReasonPhrase(), mensaje, request.getRequestURI(), null));
    }
}
