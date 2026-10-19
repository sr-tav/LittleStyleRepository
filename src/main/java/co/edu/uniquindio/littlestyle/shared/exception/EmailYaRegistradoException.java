package co.edu.uniquindio.littlestyle.shared.exception;

import org.springframework.http.HttpStatus;

/** El correo ya pertenece a otra cuenta. Responde 409 sobre el campo {@code email}. */
public class EmailYaRegistradoException extends BusinessException {

    /** Crea el error con el correo en conflicto. */
    public EmailYaRegistradoException(String email) {
        super(HttpStatus.CONFLICT, "Ya existe una cuenta registrada con el correo " + email, "email");
    }
}
