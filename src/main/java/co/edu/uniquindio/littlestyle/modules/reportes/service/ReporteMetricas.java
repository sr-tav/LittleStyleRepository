package co.edu.uniquindio.littlestyle.modules.reportes.service;

import co.edu.uniquindio.littlestyle.modules.reportes.dto.EstadoRnf24;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.FormatoReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.MetricaExitoResponse;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.ResultadoExportacion;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.TelemetriaExportacionRequest;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.TipoReporte;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Métricas auditables del RNF-24.
 *
 * <p>Se usan dos contadores independientes, con nombres estables para Actuator:</p>
 * <ul>
 *   <li>{@code reporte_exportaciones_total{tipo,formato,resultado}} con {@code resultado} en
 *       {@code ok | rechazado | error}: mide lo que hizo el servidor.</li>
 *   <li>{@code reporte_exportaciones_cliente_total{tipo,formato,resultado}} con {@code resultado}
 *       en {@code ok | error_red | blob_vacio | error_cliente | cancelado}: mide lo que el navegador
 *       pudo hacer con el archivo.</li>
 * </ul>
 *
 * <p>Los rechazos de validación ({@code rechazado}) y las cancelaciones ({@code cancelado}) se
 * cuentan pero se excluyen del denominador: son decisiones del solicitante, no fallos del sistema.
 * La tasa del servidor es {@code ok / (ok + error)} y cumple solo cuando es estrictamente mayor que
 * 0.98. La tasa de punta a punta se calcula con el contador del cliente:
 * {@code ok / (ok + error_red + blob_vacio + error_cliente)}.</p>
 *
 * <p>Cuando todavía no hay evidencia ({@code ok + error == 0}) el resumen queda en
 * {@link EstadoRnf24#SIN_DATOS} con tasa nula y sin cumplimiento: reportar 1.0 haría pasar por
 * «cumple» un requisito que nadie ha ejercitado.</p>
 *
 * <p>Los contadores viven en memoria del proceso y se reinician con cada despliegue. Para histórico
 * se debe consultar {@code /actuator/metrics} o conectar un registro persistente; el endpoint
 * devuelve una foto del proceso actual.</p>
 */
@Service
public class ReporteMetricas {

    static final String CONTADOR = "reporte.exportaciones";
    static final String CONTADOR_CLIENTE = "reporte.exportaciones.cliente";
    static final String TEMPORIZADOR = "reporte.exportacion.duracion";

    static final double UMBRAL_RNF24 = 0.98;
    private static final int MAX_TELEMETRIAS_POR_MINUTO = 30;
    private static final int MAX_USUARIOS_RASTREADOS = 10_000;

    private final MeterRegistry registro;
    private final Instant desde = Instant.now();
    private final Map<Long, Ventana> ventanas = new ConcurrentHashMap<>();

    public ReporteMetricas(MeterRegistry registro) {
        this.registro = registro;
    }

    /** Exportación completada por el backend. */
    public void exito(TipoReporte tipo, FormatoReporte formato, long nanos) {
        contar("ok", tipo, formato);
        registro.timer(TEMPORIZADOR, "tipo", etiqueta(tipo), "formato", etiqueta(formato))
                .record(nanos, TimeUnit.NANOSECONDS);
    }

    /** Error interno durante la generación. */
    public void fallo(TipoReporte tipo, FormatoReporte formato) {
        contar("error", tipo, formato);
    }

    /** Solicitud rechazada por validación: se cuenta, pero no penaliza la tasa. */
    public void rechazo(TipoReporte tipo, FormatoReporte formato) {
        contar("rechazado", tipo, formato);
    }

    /** Evento enviado por el navegador tras guardar, abrir, imprimir o cancelar la exportación. */
    public void telemetria(Long usuarioId, TelemetriaExportacionRequest evento) {
        limitar(usuarioId);
        registro.counter(CONTADOR_CLIENTE,
                        "tipo", etiqueta(evento.tipo()),
                        "formato", etiqueta(evento.formato()),
                        "resultado", evento.resultado().valor())
                .increment();
    }

    /**
     * Tasa del servidor. Sin evidencia ({@code ok + error == 0}) el estado es
     * {@link EstadoRnf24#SIN_DATOS}, la tasa es {@code null} y no se declara cumplimiento. Con
     * evidencia, el umbral es estrictamente mayor que 0.98.
     */
    public MetricaExitoResponse resumen() {
        long ok = contarResultado("ok");
        long error = contarResultado("error");
        long rechazado = contarResultado("rechazado");
        long total = ok + error;
        if (total == 0) {
            return new MetricaExitoResponse(ok, error, rechazado, null,
                    EstadoRnf24.SIN_DATOS, false, desde);
        }
        double tasa = (double) ok / total;
        EstadoRnf24 estado = tasa > UMBRAL_RNF24 ? EstadoRnf24.CUMPLE : EstadoRnf24.NO_CUMPLE;
        return new MetricaExitoResponse(ok, error, rechazado, redondear(tasa), estado,
                estado == EstadoRnf24.CUMPLE, desde);
    }

    private void contar(String resultado, TipoReporte tipo, FormatoReporte formato) {
        Counter.builder(CONTADOR)
                .tag("tipo", etiqueta(tipo))
                .tag("formato", etiqueta(formato))
                .tag("resultado", resultado)
                .register(registro)
                .increment();
    }

    private long contarResultado(String resultado) {
        return (long) registro.find(CONTADOR).tag("resultado", resultado).counters().stream()
                .mapToDouble(Counter::count)
                .sum();
    }

    private static String etiqueta(Enum<?> valor) {
        return valor.name().toLowerCase(Locale.ROOT);
    }

    private static double redondear(double valor) {
        return Math.round(valor * 10_000d) / 10_000d;
    }

    /**
     * Ventana deslizante por minuto: un cliente con un error de red no debe poder inundar el
     * contador. El evento que excede el límite no se cuenta. Se descarta el mapa completo si crece
     * demasiado para no retener memoria de usuarios ya inactivos.
     */
    private void limitar(Long usuarioId) {
        if (usuarioId == null) {
            return;
        }
        long minuto = System.currentTimeMillis() / 60_000L;
        if (ventanas.size() > MAX_USUARIOS_RASTREADOS) {
            ventanas.clear();
        }
        ventanas.compute(usuarioId, (id, previa) -> {
            if (previa == null || previa.minuto() != minuto) {
                return new Ventana(minuto, 1);
            }
            if (previa.eventos() >= MAX_TELEMETRIAS_POR_MINUTO) {
                throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS,
                        "Demasiados eventos de telemetría. Espera un minuto antes de reintentar.",
                        "telemetria");
            }
            return new Ventana(minuto, previa.eventos() + 1);
        });
    }

    private record Ventana(long minuto, int eventos) {
    }
}
