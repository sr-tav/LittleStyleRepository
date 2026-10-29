package co.edu.uniquindio.littlestyle.modules.reportes.factory;

import co.edu.uniquindio.littlestyle.modules.reportes.dto.ReporteDatos;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.TipoReporte;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

class GeneradorReportePdfTest {

    private static GeneradorReportePdf generador;

    @BeforeAll
    static void compilarPlantillas() {
        PlantillasReporte plantillas = new PlantillasReporte();
        plantillas.precompilar();                       // falla aquí si un JRXML está mal
        generador = new GeneradorReportePdf(plantillas);
    }

    @ParameterizedTest
    @EnumSource(TipoReporte.class)
    void generaUnPdfValidoParaCadaTipo(TipoReporte tipo) {
        byte[] pdf = generador.generar(datos(tipo, filas(tipo, 3)));

        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(pdf.length).isGreaterThan(1_000);
    }

    @ParameterizedTest
    @EnumSource(TipoReporte.class)
    void sinFilasTambienGeneraUnPdfValido(TipoReporte tipo) {
        byte[] pdf = generador.generar(datos(tipo, List.of()));
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    void soportaTildesYEnie() {
        Map<String, ?> fila = Map.of("seccion", "Categorías", "etiqueta", "Camisa niña ñandú",
                "cantidad", "1", "valor", "$ 10.000");
        assertThatCode(() -> generador.generar(datos(TipoReporte.VENTAS, List.of(fila)))).doesNotThrowAnyException();
    }

    @Test
    void unReporteGrandeOcupaMasBytesQueUnoPequeno() {
        byte[] chico = generador.generar(datos(TipoReporte.INVENTARIO, filas(TipoReporte.INVENTARIO, 5)));
        byte[] grande = generador.generar(datos(TipoReporte.INVENTARIO, filas(TipoReporte.INVENTARIO, 500)));
        assertThat(grande.length).isGreaterThan(chico.length);
    }

    private static ReporteDatos datos(TipoReporte tipo, List<Map<String, ?>> filas) {
        Map<String, Object> p = new HashMap<>();
        p.put("TITULO", tipo.getTitulo());
        p.put("SUBTITULO", "Mi tienda · Del 01/10/2026 al 28/10/2026");
        p.put("GENERADO_EN", "28/10/2026 10:00");
        p.put("GENERADO_POR", "Vendedor de prueba");
        return new ReporteDatos(tipo, tipo.getTitulo(), "reporte-test", p, filas);
    }

    private static List<Map<String, ?>> filas(TipoReporte tipo, int n) {
        List<Map<String, ?>> filas = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            filas.add(tipo == TipoReporte.INVENTARIO
                    ? Map.of("producto", "Vestido " + i, "tienda", "Tienda X", "categoria", "Vestidos",
                    "talla", "4T", "stock", "10", "minimo", "3", "nivel", "Disponible")
                    : Map.of("seccion", "Sección " + (i % 2), "etiqueta", "Concepto " + i,
                    "cantidad", String.valueOf(i), "valor", "$ " + i * 1000));
        }
        return filas;
    }
}