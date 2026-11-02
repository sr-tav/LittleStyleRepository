package co.edu.uniquindio.littlestyle.modules.dashboard.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Evita contar dos veces el mismo evento (misma vista o misma búsqueda del mismo cliente) dentro de una
 * ventana de tiempo: un refresco, volver atrás o abrir la ficha varias veces seguidas suma una sola vez.
 *
 * <p>La ventana arranca en el primer evento y no se renueva con las repeticiones, así que un cliente que
 * insiste suma como máximo una vez por ventana. El estado vive solo en memoria (igual que
 * {@code IntentosLoginService}): no se guarda quién vio qué, se pierde al reiniciar y no se comparte entre
 * instancias. Tras un reinicio, o con varias instancias, algún evento repetido puede contarse de más; es
 * el costo de no persistir la identidad del cliente.
 */
@Component
public class VentanaAntiRepeticion {

    private static final Duration INTERVALO_LIMPIEZA = Duration.ofMinutes(1);

    private final Duration ventana;
    private final Clock clock;
    private final Map<String, Instant> vistos = new ConcurrentHashMap<>();
    private volatile Instant proximaLimpieza = Instant.EPOCH;

    /** Crea la ventana con la duración configurada y el reloj UTC del sistema. */
    @Autowired
    public VentanaAntiRepeticion(@Value("${app.analytics.repeticion-minutos:30}") long minutos) {
        this(Duration.ofMinutes(minutos), Clock.systemUTC());
    }

    /** Constructor con reloj inyectable, usado en las pruebas. */
    VentanaAntiRepeticion(Duration ventana, Clock clock) {
        if (ventana.isNegative() || ventana.isZero()) {
            throw new IllegalArgumentException("La ventana anti-repetición debe ser mayor que cero");
        }
        this.ventana = ventana;
        this.clock = clock;
    }

    /**
     * Anota el evento y dice si debe contarse.
     *
     * @return {@code true} si es la primera vez en la ventana (queda anotado); {@code false} si es una repetición
     */
    public boolean registrarSiNuevo(String clave) {
        Instant ahora = clock.instant();
        limpiarSiCorresponde(ahora);
        boolean[] nuevo = {false};
        vistos.compute(clave, (k, previo) -> {
            if (previo == null || !ahora.isBefore(previo.plus(ventana))) {
                nuevo[0] = true;
                return ahora;
            }
            return previo;
        });
        return nuevo[0];
    }

    /** Cantidad de eventos recordados; solo para pruebas. */
    int tamano() {
        return vistos.size();
    }

    /** Descarta los eventos ya vencidos, como máximo una vez por minuto, para que el mapa no crezca sin límite. */
    private void limpiarSiCorresponde(Instant ahora) {
        if (ahora.isBefore(proximaLimpieza)) {
            return;
        }
        proximaLimpieza = ahora.plus(INTERVALO_LIMPIEZA);
        vistos.values().removeIf(desde -> !ahora.isBefore(desde.plus(ventana)));
    }
}
