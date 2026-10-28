package co.edu.uniquindio.littlestyle.modules.dashboard.service;

import co.edu.uniquindio.littlestyle.modules.dashboard.model.Busqueda;
import co.edu.uniquindio.littlestyle.modules.dashboard.model.ProductoVista;
import co.edu.uniquindio.littlestyle.modules.dashboard.repository.BusquedaRepository;
import co.edu.uniquindio.littlestyle.modules.dashboard.repository.ProductoVistaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Registra las búsquedas y las vistas de prendas que alimentan el dashboard (US-15).
 *
 * <p>Corre en segundo plano y nunca propaga errores: medir no puede tumbar ni demorar la navegación del
 * cliente. Si falla un registro, se pierde esa medición y queda un aviso en el log.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingAnalyticsService {

    static final int LONGITUD_MINIMA = 2;
    static final int LONGITUD_MAXIMA = 120;

    private final BusquedaRepository busquedaRepository;
    private final ProductoVistaRepository productoVistaRepository;

    /** Guarda el término buscado (normalizado) con la cantidad de resultados que devolvió. */
    @Async
    public void registrarBusqueda(String texto, long totalResultados) {
        String termino = normalizar(texto);
        if (termino == null) {
            return;
        }
        try {
            busquedaRepository.save(Busqueda.builder()
                    .termino(termino)
                    .totalResultados(totalResultados)
                    .fecha(LocalDateTime.now())
                    .build());
        } catch (RuntimeException ex) {
            log.warn("No se pudo registrar la búsqueda para el dashboard", ex);
        }
    }

    /** Cuenta una consulta de la ficha de la prenda. */
    @Async
    public void registrarVista(Long prendaId) {
        if (prendaId == null) {
            return;
        }
        try {
            productoVistaRepository.save(ProductoVista.builder()
                    .prendaId(prendaId)
                    .fecha(LocalDateTime.now())
                    .build());
        } catch (RuntimeException ex) {
            log.warn("No se pudo registrar la vista de la prenda {} para el dashboard", prendaId, ex);
        }
    }

    /** Minúsculas, espacios colapsados y recorte a 120; nulo si queda vacío o demasiado corto. */
    static String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        if (limpio.length() < LONGITUD_MINIMA) {
            return null;
        }
        return limpio.length() > LONGITUD_MAXIMA ? limpio.substring(0, LONGITUD_MAXIMA) : limpio;
    }
}
