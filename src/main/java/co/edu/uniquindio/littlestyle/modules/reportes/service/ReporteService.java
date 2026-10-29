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
 * Orquesta la exportación (US-16). Cada intento deja una línea {@code reporte.exportacion resultado=OK|ERROR}
 * para medir la tasa de éxito de RNF-24 (> 98 %).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteService {

    private final ConstructorDatosReporte constructor;
    private final GeneradorReporteFactory factory;

    public ArchivoReporte exportar(SolicitudReporte solicitud, AuthenticatedUser usuario) {
        Long vendedorId = AlcanceDatos.vendedorId(usuario);
        long inicio = System.nanoTime();
        try {
            ReporteDatos datos = constructor.construir(solicitud, vendedorId, usuario);
            ArchivoReporte archivo = factory.obtener(solicitud.formato()).exportar(datos);
            log.info("reporte.exportacion resultado=OK tipo={} formato={} filas={} bytes={} ms={} usuarioId={}",
                    solicitud.tipo(), solicitud.formato(), datos.filas().size(), archivo.contenido().length,
                    (System.nanoTime() - inicio) / 1_000_000, usuario.id());
            return archivo;
        } catch (BusinessException ex) {
            log.warn("reporte.exportacion resultado=RECHAZADO tipo={} motivo={}", solicitud.tipo(), ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            log.error("reporte.exportacion resultado=ERROR tipo={} formato={} usuarioId={}",
                    solicitud.tipo(), solicitud.formato(), usuario.id(), ex);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible generar el reporte. Inténtalo de nuevo.");
        }
    }
}