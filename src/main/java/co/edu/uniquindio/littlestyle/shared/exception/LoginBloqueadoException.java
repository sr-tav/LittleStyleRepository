package co.edu.uniquindio.littlestyle.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.Duration;

/** Se lanza cuando se superó el número de intentos fallidos de login permitidos. */
@Getter
public class LoginBloqueadoException extends BusinessException {

    private final long segundosRestantes;

    public LoginBloqueadoException(Duration restante) {
        super(HttpStatus.TOO_MANY_REQUESTS, mensaje(restante));
        this.segundosRestantes = Math.max(1, (restante.toMillis() + 999) / 1000);
    }

    private static String mensaje(Duration restante) {
        long minutos = Math.max(1, (restante.toSeconds() + 59) / 60);
        return "Demasiados intentos fallidos de inicio de sesión. Intente de nuevo en "
                + minutos + (minutos == 1 ? " minuto" : " minutos");
    }
}
