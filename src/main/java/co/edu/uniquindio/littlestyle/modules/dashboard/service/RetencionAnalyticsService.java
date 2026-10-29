package co.edu.uniquindio.littlestyle.modules.dashboard.service;

import co.edu.uniquindio.littlestyle.modules.dashboard.repository.BusquedaRepository;
import co.edu.uniquindio.littlestyle.modules.dashboard.repository.ProductoVistaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Depura las tablas de tracking ({@code busquedas} y {@code producto_vistas}) para que no crezcan sin límite
 * y el dashboard no se vuelva lento con el tiempo (RNF-04). Borra lo anterior a la retención configurada
 * (12 meses por defecto) todos los días de madrugada.
 *
 * <p>Es seguro con varias instancias: borrar dos veces lo mismo no tiene efecto. La primera corrida sobre
 * una tabla que ya acumuló mucho historial puede tardar; por eso corre de madrugada.
 */
@Slf4j
@Service
public class RetencionAnalyticsService {

    private final BusquedaRepository busquedaRepository;
    private final ProductoVistaRepository productoVistaRepository;
    private final Clock clock;
    private final int retencionMeses;

    public RetencionAnalyticsService(BusquedaRepository busquedaRepository,
                                     ProductoVistaRepository productoVistaRepository,
                                     Clock clock,
                                     @Value("${app.analytics.retencion-meses:12}") int retencionMeses) {
        // Con 0 o negativo el límite caería en el presente y se borraría todo el historial: se rechaza al arrancar.
        if (retencionMeses < 1) {
            throw new IllegalArgumentException("app.analytics.retencion-meses debe ser al menos 1");
        }
        this.busquedaRepository = busquedaRepository;
        this.productoVistaRepository = productoVistaRepository;
        this.clock = clock;
        this.retencionMeses = retencionMeses;
    }

    /** Tarea diaria; el horario se cambia con {@code app.analytics.purga-cron}. Un fallo se registra y no se propaga. */
    @Scheduled(cron = "${app.analytics.purga-cron:0 30 3 * * *}")
    public void purgarProgramado() {
        LocalDateTime limite = LocalDateTime.now(clock).minusMonths(retencionMeses);
        try {
            Resultado resultado = purgarAnterioresA(limite);
            log.info("Depuración de analíticas: {} búsquedas y {} vistas anteriores a {} eliminadas",
                    resultado.busquedas(), resultado.vistas(), limite);
        } catch (RuntimeException ex) {
            log.error("No se pudo depurar las tablas de analíticas (límite {})", limite, ex);
        }
    }

    /** Elimina lo estrictamente anterior al límite; un registro exactamente en el límite se conserva. */
    public Resultado purgarAnterioresA(LocalDateTime limite) {
        int busquedas = busquedaRepository.eliminarAnterioresA(limite);
        int vistas = productoVistaRepository.eliminarAnterioresA(limite);
        return new Resultado(busquedas, vistas);
    }

    /** Cantidad de filas eliminadas en cada tabla. */
    public record Resultado(int busquedas, int vistas) {
    }
}
