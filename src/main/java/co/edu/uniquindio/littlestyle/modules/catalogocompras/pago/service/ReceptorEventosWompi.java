package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.service;

import co.edu.uniquindio.littlestyle.config.PagoProperties;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto.EventoWompiRequest;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * Recibe los eventos del webhook de Wompi. La firma y la vigencia se validan en la petición (401 si
 * fallan) y la aplicación del evento, que bloquea el pedido y descuenta stock, corre en un grupo
 * acotado de hilos. Así Wompi recibe el 200 de inmediato y varios avisos simultáneos no esperan en
 * fila uno detrás de otro (RNF-05, RNF-21). Si la cola se llena se responde 503 para que Wompi
 * reintente; si la aplicación falla, el pago sigue confirmándose con la consulta directa
 * {@link PagoService#verificar} desde la página de resultado (RNF-28).
 */
@Slf4j
@Service
public class ReceptorEventosWompi implements DisposableBean {

    static final int MAX_REINTENTOS = 3;

    private final PagoService pagoService;
    private final TaskExecutor ejecutor;

    /**
     * Usa un grupo de hilos acotado o, si los eventos asíncronos están desactivados, los aplica en la misma
     * petición.
     */
    public ReceptorEventosWompi(PagoService pagoService, PagoProperties pagoProperties) {
        this.pagoService = pagoService;
        this.ejecutor = pagoProperties.eventosAsincronos() ? crearEjecutor(pagoProperties) : new SyncTaskExecutor();
    }

    /**
     * Valida firma y vigencia (401 si fallan) y encola la aplicación del evento; 503 si la cola está llena.
     */
    public void recibir(EventoWompiRequest evento) {
        pagoService.validarEvento(evento);
        try {
            ejecutor.execute(() -> aplicarConReintentos(evento));
        } catch (TaskRejectedException e) {
            log.warn("Cola de eventos de Wompi llena; se solicita reintento (evento={})", evento.event());
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Hay muchos eventos en proceso. Reintenta el envío más tarde.");
        }
    }

    /**
     * Aplica el evento hasta {@link #MAX_REINTENTOS} veces ante errores inesperados; los errores de negocio
     * lo descartan.
     */
    private void aplicarConReintentos(EventoWompiRequest evento) {
        for (int intento = 1; intento <= MAX_REINTENTOS; intento++) {
            try {
                pagoService.aplicarEvento(evento);
                return;
            } catch (BusinessException e) {
                log.warn("Evento de Wompi descartado: {}", e.getMessage());
                return;
            } catch (RuntimeException e) {
                if (intento == MAX_REINTENTOS) {
                    log.error("No fue posible aplicar el evento de Wompi tras {} intentos; el pago se "
                            + "confirmará con la consulta directa a Wompi", MAX_REINTENTOS, e);
                    return;
                }
                log.warn("Fallo al aplicar el evento de Wompi (intento {}): {}", intento, e.getMessage());
                esperar(intento);
            }
        }
    }

    /** Espera creciente entre reintentos (200 ms por intento). */
    private static void esperar(int intento) {
        try {
            Thread.sleep(200L * intento);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Grupo de hilos con el tamaño y la cola configurados. */
    private static ThreadPoolTaskExecutor crearEjecutor(PagoProperties propiedades) {
        ThreadPoolTaskExecutor ejecutor = new ThreadPoolTaskExecutor();
        ejecutor.setCorePoolSize(propiedades.hilosEventos());
        ejecutor.setMaxPoolSize(propiedades.hilosEventos());
        ejecutor.setQueueCapacity(propiedades.colaEventos());
        ejecutor.setThreadNamePrefix("wompi-eventos-");
        ejecutor.setWaitForTasksToCompleteOnShutdown(true);
        ejecutor.setAwaitTerminationSeconds(30);
        ejecutor.initialize();
        return ejecutor;
    }

    /** Detiene el grupo de hilos al cerrar la aplicación, esperando los eventos en curso. */
    @Override
    public void destroy() {
        if (ejecutor instanceof ThreadPoolTaskExecutor grupo) {
            grupo.shutdown();
        }
    }
}
