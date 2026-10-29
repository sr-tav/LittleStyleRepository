package co.edu.uniquindio.littlestyle.modules.dashboard.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
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
import java.util.regex.Pattern;

/**
 * Registra las búsquedas y las vistas de prendas que alimentan el dashboard (US-15).
 *
 * <p>Para que la métrica sirva de decisión, un evento <b>no</b> se cuenta si:
 * <ul>
 *   <li>lo genera un cliente automatizado (rastreadores, scripts, navegadores sin interfaz);</li>
 *   <li>es una repetición del mismo cliente dentro de la ventana de {@link VentanaAntiRepeticion}
 *       (refrescos, volver atrás);</li>
 *   <li>es la vista del propio vendedor sobre una prenda suya;</li>
 *   <li>no hay un cliente autenticado que permita aplicar las reglas anteriores.</li>
 * </ul>
 * El filtro de agentes solo detecta a quien se declara automatizado; quien finge ser un navegador queda
 * limitado, no bloqueado, por la ventana anti-repetición.
 *
 * <p>Corre en segundo plano y nunca propaga errores: medir no puede tumbar ni demorar la navegación del
 * cliente. Si falla un registro, se pierde esa medición y queda un aviso en el log. Las tablas se depuran
 * con {@link RetencionAnalyticsService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingAnalyticsService {

    static final int LONGITUD_MINIMA = 2;
    static final int LONGITUD_MAXIMA = 120;

    /** Firmas habituales de rastreadores y clientes de scripts; "bot\b" no confunde marcas como "Cubot_X30". */
    private static final Pattern AGENTE_AUTOMATIZADO = Pattern.compile(
            "(?i)bot\\b|crawl|spider|slurp|headless|scrapy|curl/|wget|python-requests|http-?client|postman|insomnia|java/");

    private final BusquedaRepository busquedaRepository;
    private final ProductoVistaRepository productoVistaRepository;
    private final PrendaRepository prendaRepository;
    private final VentanaAntiRepeticion antiRepeticion;

    /**
     * Guarda el término buscado (normalizado) con la cantidad de resultados que devolvió.
     *
     * @param usuarioId id del cliente autenticado; solo se usa en memoria para no contar repeticiones
     */
    @Async
    public void registrarBusqueda(String texto, long totalResultados, Long usuarioId, String userAgent) {
        String termino = normalizar(texto);
        if (termino == null || usuarioId == null || esAutomatizado(userAgent)) {
            return;
        }
        if (!antiRepeticion.registrarSiNuevo("busqueda:" + usuarioId + ":" + termino)) {
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

    /**
     * Cuenta una consulta de la ficha de la prenda.
     *
     * @param usuarioId id del cliente autenticado; sirve para ignorar repeticiones y al dueño de la prenda
     */
    @Async
    public void registrarVista(Long prendaId, Long usuarioId, String userAgent) {
        if (prendaId == null || usuarioId == null || esAutomatizado(userAgent)) {
            return;
        }
        if (!antiRepeticion.registrarSiNuevo("vista:" + usuarioId + ":" + prendaId)) {
            return;
        }
        try {
            if (prendaRepository.existsByIdAndVendedorId(prendaId, usuarioId)) {
                return; // el vendedor mirando su propia prenda no es demanda
            }
            productoVistaRepository.save(ProductoVista.builder()
                    .prendaId(prendaId)
                    .fecha(LocalDateTime.now())
                    .build());
        } catch (RuntimeException ex) {
            log.warn("No se pudo registrar la vista de la prenda {} para el dashboard", prendaId, ex);
        }
    }

    /** {@code true} si el agente se declara automatizado o no se identifica (un navegador siempre lo envía). */
    static boolean esAutomatizado(String userAgent) {
        return userAgent == null || userAgent.isBlank() || AGENTE_AUTOMATIZADO.matcher(userAgent).find();
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
