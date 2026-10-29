package co.edu.uniquindio.littlestyle.modules.reportes.factory;

import co.edu.uniquindio.littlestyle.modules.reportes.dto.*;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

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
}