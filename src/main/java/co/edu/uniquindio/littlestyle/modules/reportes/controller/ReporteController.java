package co.edu.uniquindio.littlestyle.modules.reportes.controller;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.ArchivoReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.FormatoReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.MetricaExitoResponse;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.SolicitudReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.TelemetriaExportacionRequest;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.TipoReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.service.ReporteMetricas;
import co.edu.uniquindio.littlestyle.modules.reportes.service.ReporteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * Exportación de reportes analíticos (US-16). El alcance sale del token, nunca de un parámetro.
 *
 * <p>Los archivos se sirven como adjunto con {@code X-Content-Type-Options: nosniff} para que el
 * navegador no intente interpretar el contenido como HTML. Los formatos los resuelve la fábrica;
 * este controlador no decide por formato.</p>
 */
@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;
    private final ReporteMetricas metricas;

    @GetMapping("/{tipo}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VENDEDOR')")
    public ResponseEntity<byte[]> exportar(
            @AuthenticationPrincipal AuthenticatedUser usuario,
            @PathVariable String tipo,
            @RequestParam(defaultValue = "PDF") String formato,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String categoria) {

        ArchivoReporte archivo = reporteService.exportar(
                new SolicitudReporte(TipoReporte.desde(tipo), FormatoReporte.desde(formato),
                        startDate, endDate, categoria), usuario);

        ContentDisposition disposicion = ContentDisposition.attachment()
                .filename(archivo.nombre(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .contentType(archivo.tipoMedia())
                .contentLength(archivo.contenido().length)   // permite la barra de progreso real en el cliente
                .body(archivo.contenido());
    }

    /**
     * Telemetría de éxito del cliente. Es "fire and forget": el navegador no espera la respuesta,
     * pero un fallo de red nunca debe romper la experiencia de descarga.
     */
    @PostMapping("/telemetria")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VENDEDOR')")
    public void telemetria(@AuthenticationPrincipal AuthenticatedUser usuario,
                           @Valid @RequestBody TelemetriaExportacionRequest evento) {
        metricas.telemetria(usuario.id(), evento);
    }

    /** Estado actual del RNF-24 para auditoría. */
    @GetMapping("/metricas/exito")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public MetricaExitoResponse metricaExito() {
        return metricas.resumen();
    }
}
