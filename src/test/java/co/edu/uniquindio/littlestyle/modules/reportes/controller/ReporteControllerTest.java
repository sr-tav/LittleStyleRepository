package co.edu.uniquindio.littlestyle.modules.reportes.controller;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.ArchivoReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.EstadoRnf24;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.FormatoReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.MetricaExitoResponse;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.ResultadoExportacion;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.TelemetriaExportacionRequest;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.TipoReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.service.ReporteMetricas;
import co.edu.uniquindio.littlestyle.modules.reportes.service.ReporteService;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteControllerTest {

    @Mock private ReporteService service;
    @Mock private ReporteMetricas metricas;
    @InjectMocks private ReporteController controller;

    @Test
    void respondeConPdfComoAdjuntoConLongitudSinCacheYNosniff() {
        AuthenticatedUser admin = new AuthenticatedUser(1L, "a@t.com", "Admin", Rol.ADMINISTRADOR);
        byte[] pdf = "%PDF-1.7 contenido".getBytes(StandardCharsets.ISO_8859_1);
        when(service.exportar(any(), eq(admin)))
                .thenReturn(new ArchivoReporte(pdf, "reporte-ventas_2026-10-01_2026-10-28.pdf", MediaType.APPLICATION_PDF));

        ResponseEntity<byte[]> r = controller.exportar(admin, "ventas", "PDF",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 28), null);

        assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(r.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(r.getHeaders().getContentDisposition().isAttachment()).isTrue();
        assertThat(r.getHeaders().getContentDisposition().getFilename()).isEqualTo("reporte-ventas_2026-10-01_2026-10-28.pdf");
        assertThat(r.getHeaders().getContentLength()).isEqualTo(pdf.length);
        assertThat(r.getHeaders().getCacheControl()).contains("no-store");
        assertThat(r.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
    }

    @Test
    void respondeConElTipoMediaDelXlsxYDelCsv() {
        AuthenticatedUser admin = new AuthenticatedUser(1L, "a@t.com", "Admin", Rol.ADMINISTRADOR);
        byte[] contenido = new byte[]{1, 2, 3};
        MediaType xlsx = MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        when(service.exportar(any(), eq(admin)))
                .thenReturn(new ArchivoReporte(contenido, "reporte.xlsx", xlsx))
                .thenReturn(new ArchivoReporte(contenido, "reporte.csv", FormatoReporte.CSV.getTipoMedia()));

        ResponseEntity<byte[]> xlsxResponse = controller.exportar(admin, "ventas", "XLSX", null, null, null);
        assertThat(xlsxResponse.getHeaders().getContentType()).isEqualTo(xlsx);
        assertThat(xlsxResponse.getHeaders().getContentDisposition().getFilename()).isEqualTo("reporte.xlsx");
        ResponseEntity<byte[]> csv = controller.exportar(admin, "ventas", "csv", null, null, null);
        assertThat(csv.getHeaders().getContentType()).isEqualTo(FormatoReporte.CSV.getTipoMedia());
        assertThat(csv.getHeaders().getContentDisposition().getFilename()).isEqualTo("reporte.csv");
    }

    @Test
    void tipoOFormatoInvalidosDevuelven400SinGenerarNada() {
        AuthenticatedUser admin = new AuthenticatedUser(1L, "a@t.com", "Admin", Rol.ADMINISTRADOR);
        assertThatThrownBy(() -> controller.exportar(admin, "ganancias", "PDF", null, null, null))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> controller.exportar(admin, "ventas", "DOCX", null, null, null))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(service);
    }

    @Test
    void elEndpointExigeAdministradorOVendedor() throws NoSuchMethodException {
        var metodo = ReporteController.class.getDeclaredMethod("exportar", AuthenticatedUser.class, String.class,
                String.class, LocalDate.class, LocalDate.class, String.class);
        PreAuthorize regla = metodo.getAnnotation(PreAuthorize.class);
        assertThat(regla.value()).contains("ADMINISTRADOR").contains("VENDEDOR").doesNotContain("CLIENTE");
    }

    @Test
    void laTelemetriaSeDelegaEnElServicioDeMetricas() {
        AuthenticatedUser vendedor = new AuthenticatedUser(7L, "v@t.com", "Vendedor", Rol.VENDEDOR);
        TelemetriaExportacionRequest evento = new TelemetriaExportacionRequest(
                TipoReporte.VENTAS, FormatoReporte.XLSX, ResultadoExportacion.OK, null);

        controller.telemetria(vendedor, evento);

        verify(metricas).telemetria(7L, evento);
    }

    @Test
    void laMetricaDeExitoDevuelveElResumenYEsSoloParaAdministrador() throws NoSuchMethodException {
        MetricaExitoResponse resumen = new MetricaExitoResponse(98, 1, 2, 0.9899,
                EstadoRnf24.CUMPLE, true, java.time.Instant.parse("2026-10-31T10:00:00Z"));
        when(metricas.resumen()).thenReturn(resumen);

        assertThat(controller.metricaExito()).isSameAs(resumen);

        var metodo = ReporteController.class.getDeclaredMethod("metricaExito");
        assertThat(metodo.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMINISTRADOR')");
    }
}
