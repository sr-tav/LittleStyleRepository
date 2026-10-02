package co.edu.uniquindio.littlestyle.shared.exception;

import org.springframework.http.HttpStatus;

public class EmailYaRegistradoException extends BusinessException {

    public EmailYaRegistradoException(String email) {
        super(HttpStatus.CONFLICT, "Ya existe una cuenta registrada con el correo " + email, "email");
    }
}
