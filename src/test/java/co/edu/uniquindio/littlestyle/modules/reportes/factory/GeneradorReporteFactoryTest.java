package co.edu.uniquindio.littlestyle.modules.reportes.factory;

import co.edu.uniquindio.littlestyle.modules.reportes.dto.*;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// src/test/.../reportes/factory/GeneradorReporteFactoryTest.java
class GeneradorReporteFactoryTest {

    @Test
    void devuelveElCreadorDelFormatoSolicitado() {
        CreadorGeneradorReporte pdf = mock(CreadorGeneradorReporte.class);
        when(pdf.formato()).thenReturn(FormatoReporte.PDF);

        GeneradorReporteFactory factory = new GeneradorReporteFactory(List.of(pdf));

        assertThat(factory.obtener(FormatoReporte.PDF)).isSameAs(pdf);
    }

    @Test
    void formatoSinCreadorOFormatoNuloDevuelve400() {
        GeneradorReporteFactory factory = new GeneradorReporteFactory(List.of());

        assertThatThrownBy(() -> factory.obtener(FormatoReporte.PDF))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> factory.obtener(null)).isInstanceOf(BusinessException.class);
    }

    @Test
    void dosCreadoresParaElMismoFormatoFallanAlArrancar() {
        CreadorGeneradorReporte a = mock(CreadorGeneradorReporte.class);
        CreadorGeneradorReporte b = mock(CreadorGeneradorReporte.class);
        when(a.formato()).thenReturn(FormatoReporte.PDF);
        when(b.formato()).thenReturn(FormatoReporte.PDF);

        assertThatThrownBy(() -> new GeneradorReporteFactory(List.of(a, b)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void elCreadorConcretoPdfInstanciaUnGeneradorPdf() {
        CreadorGeneradorPdf creador = new CreadorGeneradorPdf(mock(PlantillasReporte.class));
        assertThat(creador.formato()).isEqualTo(FormatoReporte.PDF);
        assertThat(creador.crearGenerador()).isInstanceOf(GeneradorReportePdf.class);
    }

    @Test
    void cadaFormatoTieneSuPropioCreadorYGenerador() {
        CreadorGeneradorXlsx xlsx = new CreadorGeneradorXlsx();
        CreadorGeneradorCsv csv = new CreadorGeneradorCsv();

        assertThat(xlsx.formato()).isEqualTo(FormatoReporte.XLSX);
        assertThat(xlsx.crearGenerador()).isInstanceOf(GeneradorReporteXlsx.class);
        assertThat(csv.formato()).isEqualTo(FormatoReporte.CSV);
        assertThat(csv.crearGenerador()).isInstanceOf(GeneradorReporteCsv.class);

        GeneradorReporteFactory factory = new GeneradorReporteFactory(List.of(
                new CreadorGeneradorPdf(mock(PlantillasReporte.class)), xlsx, csv));
        assertThat(factory.obtener(FormatoReporte.PDF)).isInstanceOf(CreadorGeneradorPdf.class);
        assertThat(factory.obtener(FormatoReporte.XLSX)).isInstanceOf(CreadorGeneradorXlsx.class);
        assertThat(factory.obtener(FormatoReporte.CSV)).isInstanceOf(CreadorGeneradorCsv.class);
    }

    @Test
    void elNombreDelArchivoUsaLaExtensionDelFormato() {
        ReporteDatos datos = new ReporteDatos(TipoReporte.VENTAS, "Ventas", "reporte-ventas", Map.of(), List.of());
        CreadorGeneradorCsv csv = new CreadorGeneradorCsv();
        CreadorGeneradorXlsx xlsx = new CreadorGeneradorXlsx();

        assertThat(csv.exportar(datos).nombre()).isEqualTo("reporte-ventas.csv");
        assertThat(xlsx.exportar(datos).nombre()).isEqualTo("reporte-ventas.xlsx");
    }
}