package co.edu.uniquindio.littlestyle.modules.reportes.service;

import co.edu.uniquindio.littlestyle.modules.reportes.dto.EstadoRnf24;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.FormatoReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.MetricaExitoResponse;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.ResultadoExportacion;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.TelemetriaExportacionRequest;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.TipoReporte;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReporteMetricasTest {

    private SimpleMeterRegistry registro;
    private ReporteMetricas metricas;

    @BeforeEach
    void crearMetricas() {
        registro = new SimpleMeterRegistry();
        metricas = new ReporteMetricas(registro);
    }

    @Test
    void sinExportacionesElEstadoEsSinDatosYNoCumple() {
        MetricaExitoResponse r = metricas.resumen();

        assertThat(r.ok()).isZero();
        assertThat(r.error()).isZero();
        assertThat(r.rechazado()).isZero();
        assertThat(r.tasaExito()).isNull();
        assertThat(r.estado()).isEqualTo(EstadoRnf24.SIN_DATOS);
        assertThat(r.cumpleRnf24()).isFalse();
        assertThat(r.desde()).isNotNull();
    }

    @Test
    void unaExportacionExitosaMantieneLaTasaEnUno() {
        metricas.exito(TipoReporte.VENTAS, FormatoReporte.XLSX, 1_000_000);

        MetricaExitoResponse r = metricas.resumen();
        assertThat(r.ok()).isEqualTo(1);
        assertThat(r.error()).isZero();
        assertThat(r.tasaExito()).isEqualTo(1.0);
        assertThat(r.estado()).isEqualTo(EstadoRnf24.CUMPLE);
        assertThat(r.cumpleRnf24()).isTrue();
        assertThat(registro.get("reporte.exportaciones")
                .tags("tipo", "ventas", "formato", "xlsx", "resultado", "ok")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void cincuentaExportacionesSinErroresCumplen() {
        for (int i = 0; i < 50; i++) {
            metricas.exito(TipoReporte.VENTAS, FormatoReporte.PDF, 1);
        }

        MetricaExitoResponse r = metricas.resumen();
        assertThat(r.tasaExito()).isEqualTo(1.0);
        assertThat(r.estado()).isEqualTo(EstadoRnf24.CUMPLE);
        assertThat(r.cumpleRnf24()).isTrue();
    }

    @Test
    void noventaYNueveSobreCienCumplen() {
        for (int i = 0; i < 99; i++) {
            metricas.exito(TipoReporte.VENTAS, FormatoReporte.PDF, 1);
        }
        metricas.fallo(TipoReporte.VENTAS, FormatoReporte.PDF);

        MetricaExitoResponse r = metricas.resumen();
        assertThat(r.tasaExito()).isEqualTo(0.99);
        assertThat(r.estado()).isEqualTo(EstadoRnf24.CUMPLE);
        assertThat(r.cumpleRnf24()).isTrue();
    }

    @Test
    void losRechazosPorValidacionNoPenalizanLaTasa() {
        metricas.exito(TipoReporte.VENTAS, FormatoReporte.PDF, 1);
        metricas.rechazo(TipoReporte.VENTAS, FormatoReporte.PDF);

        MetricaExitoResponse r = metricas.resumen();
        assertThat(r.ok()).isEqualTo(1);
        assertThat(r.error()).isZero();
        assertThat(r.rechazado()).isEqualTo(1);
        assertThat(r.tasaExito()).isEqualTo(1.0);
        assertThat(r.estado()).isEqualTo(EstadoRnf24.CUMPLE);
    }

    @Test
    void unNoventaYOchoExactoNoCumplePorqueElUmbralEsEstricto() {
        for (int i = 0; i < 49; i++) {
            metricas.exito(TipoReporte.VENTAS, FormatoReporte.CSV, 1);
        }
        metricas.fallo(TipoReporte.VENTAS, FormatoReporte.CSV);

        MetricaExitoResponse r = metricas.resumen();
        assertThat(r.tasaExito()).isEqualTo(0.98);
        assertThat(r.estado()).isEqualTo(EstadoRnf24.NO_CUMPLE);
        assertThat(r.cumpleRnf24()).isFalse();

        metricas.exito(TipoReporte.VENTAS, FormatoReporte.CSV, 1);
        MetricaExitoResponse siguiente = metricas.resumen();
        assertThat(siguiente.tasaExito()).isGreaterThan(0.98);
        assertThat(siguiente.estado()).isEqualTo(EstadoRnf24.CUMPLE);
        assertThat(siguiente.cumpleRnf24()).isTrue();
    }

    @Test
    void laTelemetriaUsaSuPropioContadorYNoAlteraLaTasaDelServidor() {
        metricas.telemetria(7L, new TelemetriaExportacionRequest(
                TipoReporte.VENTAS, FormatoReporte.PDF, ResultadoExportacion.ERROR_RED,
                "El navegador no pudo guardar"));

        MetricaExitoResponse r = metricas.resumen();
        assertThat(r.ok()).isZero();
        assertThat(r.error()).isZero();
        assertThat(r.tasaExito()).isNull();
        assertThat(r.estado()).isEqualTo(EstadoRnf24.SIN_DATOS);
        assertThat(registro.get("reporte.exportaciones.cliente")
                .tags("tipo", "ventas", "formato", "pdf", "resultado", "error_red")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void laCancelacionSeCuentaPeroNoSeConfundeConUnError() {
        metricas.telemetria(7L, new TelemetriaExportacionRequest(
                TipoReporte.BUSQUEDAS, FormatoReporte.CSV, ResultadoExportacion.CANCELADO, null));

        assertThat(registro.get("reporte.exportaciones.cliente")
                .tags("tipo", "busquedas", "formato", "csv", "resultado", "cancelado")
                .counter().count()).isEqualTo(1.0);
        assertThat(metricas.resumen().error()).isZero();
    }

    @Test
    void laTelemetriaEstaLimitadaPorUsuarioYMinuto() {
        TelemetriaExportacionRequest evento = new TelemetriaExportacionRequest(
                TipoReporte.VENTAS, FormatoReporte.PDF, ResultadoExportacion.OK, null);
        for (int i = 0; i < 30; i++) {
            metricas.telemetria(9L, evento);
        }

        assertThatThrownBy(() -> metricas.telemetria(9L, evento))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        assertThat(registro.get("reporte.exportaciones.cliente")
                .tags("tipo", "ventas", "formato", "pdf", "resultado", "ok")
                .counter().count()).isEqualTo(30.0);
        metricas.telemetria(10L, evento);   // otro usuario conserva su propia ventana
    }
}
