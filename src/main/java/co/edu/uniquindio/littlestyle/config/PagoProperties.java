package co.edu.uniquindio.littlestyle.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Límites operativos de los pagos con Wompi (US-11).
 *
 * @param vigenciaEvento       antigüedad máxima del {@code timestamp} de un evento del webhook; uno más viejo
 *                             se rechaza aunque su firma sea válida, para que un aviso capturado no se reutilice.
 * @param toleranciaReloj      margen aceptado para eventos fechados en el futuro (desfase de relojes).
 * @param timeoutConexion      tiempo máximo para conectar con la API de Wompi.
 * @param timeoutLectura       tiempo máximo para recibir la respuesta de la API de Wompi.
 * @param maxIntentosPorPedido intentos de pago que se pueden iniciar para un mismo pedido.
 * @param eventosAsincronos    si el webhook responde de inmediato y aplica el evento en segundo plano.
 * @param hilosEventos         hilos que aplican eventos del webhook en paralelo.
 * @param colaEventos          eventos que pueden esperar turno; si se llena, el webhook responde 503 y Wompi reintenta.
 */
@ConfigurationProperties(prefix = "app.pagos")
public record PagoProperties(
        @DefaultValue("PT10M") Duration vigenciaEvento,
        @DefaultValue("PT1M") Duration toleranciaReloj,
        @DefaultValue("PT3S") Duration timeoutConexion,
        @DefaultValue("PT8S") Duration timeoutLectura,
        @DefaultValue("5") int maxIntentosPorPedido,
        @DefaultValue("true") boolean eventosAsincronos,
        @DefaultValue("4") int hilosEventos,
        @DefaultValue("100") int colaEventos
) {
    /** Valores por defecto, útiles en pruebas unitarias. */
    public static PagoProperties porDefecto() {
        return new PagoProperties(Duration.ofMinutes(10), Duration.ofMinutes(1), Duration.ofSeconds(3),
                Duration.ofSeconds(8), 5, true, 4, 100);
    }
}
