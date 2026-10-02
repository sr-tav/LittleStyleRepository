package co.edu.uniquindio.littlestyle.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Excepción de reglas de negocio con el código HTTP que debe devolverse al cliente.
 * Si se indica un campo, el error se asocia a ese input en el formulario del frontend.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String campo;

    public BusinessException(HttpStatus status, String mensaje) {
        this(status, mensaje, null);
    }

    public BusinessException(HttpStatus status, String mensaje, String campo) {
        super(mensaje);
        this.status = status;
        this.campo = campo;
    }
}
