package co.edu.uniquindio.littlestyle.modules.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita los intentos fallidos de login por combinación correo + IP. Al alcanzar el máximo dentro de
 * la ventana, la combinación queda bloqueada durante el tiempo configurado. Se usa la IP junto al
 * correo para que un tercero no pueda bloquear la cuenta de otra persona desde su propia conexión.
 * El estado vive en memoria: se pierde al reiniciar y no se comparte entre instancias.
 */
@Service
public class IntentosLoginService {

    private final int maxIntentos;
    private final Duration ventana;
    private final Duration bloqueo;
    private final Clock clock;
    private final Map<String, Registro> registros = new ConcurrentHashMap<>();

    @Autowired
    public IntentosLoginService(@Value("${app.security.login.max-intentos:5}") int maxIntentos,
                                @Value("${app.security.login.ventana-minutos:15}") long ventanaMinutos,
                                @Value("${app.security.login.bloqueo-minutos:15}") long bloqueoMinutos) {
        this(maxIntentos, Duration.ofMinutes(ventanaMinutos), Duration.ofMinutes(bloqueoMinutos), Clock.systemUTC());
    }

    IntentosLoginService(int maxIntentos, Duration ventana, Duration bloqueo, Clock clock) {
        this.maxIntentos = maxIntentos;
        this.ventana = ventana;
        this.bloqueo = bloqueo;
        this.clock = clock;
    }

    /** Tiempo restante de bloqueo, o vacío si la combinación puede intentar iniciar sesión. */
    public Optional<Duration> bloqueoRestante(String email, String ip) {
        Registro registro = registros.get(clave(email, ip));
        Instant ahora = clock.instant();
        if (registro == null || registro.bloqueadoHasta() == null || !ahora.isBefore(registro.bloqueadoHasta())) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(ahora, registro.bloqueadoHasta()));
    }

    /** Registra un fallo y devuelve {@code true} si con él la combinación quedó bloqueada. */
    public boolean registrarFallo(String email, String ip) {
        Instant ahora = clock.instant();
        purgarExpirados(ahora);
        Registro actualizado = registros.compute(clave(email, ip), (k, previo) -> {
            boolean reiniciar = previo == null
                    || !ahora.isBefore(previo.inicioVentana().plus(ventana))
                    || (previo.bloqueadoHasta() != null && !ahora.isBefore(previo.bloqueadoHasta()));
            int fallos = reiniciar ? 1 : previo.fallos() + 1;
            Instant inicio = reiniciar ? ahora : previo.inicioVentana();
            Instant bloqueadoHasta = fallos >= maxIntentos ? ahora.plus(bloqueo) : null;
            return new Registro(fallos, inicio, bloqueadoHasta);
        });
        return actualizado.bloqueadoHasta() != null;
    }

    public void registrarExito(String email, String ip) {
        registros.remove(clave(email, ip));
    }

    /** Evita que el mapa crezca sin límite con combinaciones que ya no están en ventana ni bloqueadas. */
    private void purgarExpirados(Instant ahora) {
        registros.values().removeIf(r -> !ahora.isBefore(r.inicioVentana().plus(ventana))
                && (r.bloqueadoHasta() == null || !ahora.isBefore(r.bloqueadoHasta())));
    }

    private static String clave(String email, String ip) {
        return email + "|" + ip;
    }

    private record Registro(int fallos, Instant inicioVentana, Instant bloqueadoHasta) {
    }
}
