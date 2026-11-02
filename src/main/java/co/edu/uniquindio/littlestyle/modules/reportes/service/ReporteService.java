package co.edu.uniquindio.littlestyle.modules.reportes.service;

import co.edu.uniquindio.littlestyle.config.security.AlcanceDatos;
import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.ArchivoReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.ReporteDatos;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.SolicitudReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.factory.GeneradorReporteFactory;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Orquesta la exportación (US-16). Cada intento deja una línea {@code reporte.exportacion} con
 * resultado {@code OK}, {@code RECHAZADO} o {@code ERROR} y alimenta los contadores del RNF-24
 * ({@link ReporteMetricas}).
 *
 * <p>Los errores de validación del solicitante ({@code 4xx}) se cuentan como {@code rechazado} y
 * quedan fuera de la tasa. Las excepciones internas ({@code 5xx}) se cuentan como {@code error} y
 * sí restan. Detalle de la decisión y consultas de auditoría:
 * <a href="../../../../../../../docs/us-16/metricas-rnf24.md">docs/us-16/metricas-rnf24.md</a>.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteService {

    private final ConstructorDatosReporte constructor;
    private final GeneradorReporteFactory factory;
    private final ReporteMetricas metricas;

    public ArchivoReporte exportar(SolicitudReporte solicitud, AuthenticatedUser usuario) {
        Long vendedorId = AlcanceDatos.vendedorId(usuario);
        long inicio = System.nanoTime();
        try {
            ReporteDatos datos = constructor.construir(solicitud, vendedorId, usuario);
            ArchivoReporte archivo = factory.obtener(solicitud.formato()).exportar(datos);
            long duracion = System.nanoTime() - inicio;
            metricas.exito(solicitud.tipo(), solicitud.formato(), duracion);
            log.info("reporte.exportacion resultado=OK tipo={} formato={} filas={} bytes={} ms={} usuarioId={}",
                    solicitud.tipo(), solicitud.formato(), datos.filas().size(), archivo.contenido().length,
                    duracion / 1_000_000, usuario.id());
            return archivo;
        } catch (BusinessException ex) {
            if (ex.getStatus().is4xxClientError()) {
                metricas.rechazo(solicitud.tipo(), solicitud.formato());
                log.warn("reporte.exportacion resultado=RECHAZADO tipo={} formato={} motivo={}",
                        solicitud.tipo(), solicitud.formato(), ex.getMessage());
            } else {
                metricas.fallo(solicitud.tipo(), solicitud.formato());
                log.error("reporte.exportacion resultado=ERROR tipo={} formato={} usuarioId={}",
                        solicitud.tipo(), solicitud.formato(), usuario.id(), ex);
            }
            throw ex;
        } catch (RuntimeException ex) {
            metricas.fallo(solicitud.tipo(), solicitud.formato());
            log.error("reporte.exportacion resultado=ERROR tipo={} formato={} usuarioId={}",
                    solicitud.tipo(), solicitud.formato(), usuario.id(), ex);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible generar el reporte. Inténtalo de nuevo.");
        }
    }
}
