package co.edu.uniquindio.littlestyle.modules.reportes.factory;

import co.edu.uniquindio.littlestyle.modules.reportes.dto.FormatoReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.ReporteDatos;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.TipoReporte;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
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
        assertThatCode(() -> generador.generar(datos(TipoReporte.VENTAS, filas(TipoReporte.VENTAS, 1))))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(TipoReporte.class)
    void elTextoEsExtraibleYLasFuentesEstanEmbebidas(TipoReporte tipo) throws Exception {
        byte[] pdf = generador.generar(datos(tipo, filas(tipo, 3)));

        try (PDDocument documento = Loader.loadPDF(pdf)) {
            assertThat(documento.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            String texto = new PDFTextStripper().getText(documento);
            assertThat(texto).contains(tipo.getTitulo())
                    .contains("Camisa niña ñandú")
                    .contains("Categoría");

            for (PDPage pagina : documento.getPages()) {
                for (COSName nombre : pagina.getResources().getFontNames()) {
                    PDFont fuente = pagina.getResources().getFont(nombre);
                    assertThat(fuente).isNotNull();
                    assertThat(fuente.isEmbedded())
                            .as("la fuente %s debe viajar embebida en el PDF", fuente.getName())
                            .isTrue();
                }
            }
        }
    }

    @Test
    void laBarraDeVentasSeDibujaConBloquesSegunElNivel() throws Exception {
        byte[] pdf = generador.generar(datos(TipoReporte.VENTAS, filas(TipoReporte.VENTAS, 11)));

        try (PDDocument documento = Loader.loadPDF(pdf)) {
            String texto = new PDFTextStripper().getText(documento);
            assertThat(texto).contains("█".repeat(10));
            assertThat(texto).contains("█".repeat(7));
        }
    }

    @ParameterizedTest
    @EnumSource(TipoReporte.class)
    void unReporteDe500FilasOcupaVariasPaginasYNumeraLaPrimera(TipoReporte tipo) throws Exception {
        byte[] pdf = generador.generar(datos(tipo, filas(tipo, 500)));

        try (PDDocument documento = Loader.loadPDF(pdf)) {
            int paginas = documento.getNumberOfPages();
            assertThat(paginas).isGreaterThan(1);

            String texto = new PDFTextStripper().getText(documento).replaceAll("\\s+", " ");
            assertThat(texto).containsPattern("Página 1 de\\s*" + paginas);
        }
    }

    private static ReporteDatos datos(TipoReporte tipo, List<Map<String, ?>> filas) {
        Map<String, Object> p = new HashMap<>();
        p.put("TITULO", tipo.getTitulo());
        p.put("SUBTITULO", "Categoría: Vestidos · Camisa niña ñandú");
        p.put("ALCANCE", "Consolidado global");
        p.put("GENERADO_EN", "28/10/2026 10:00");
        p.put("GENERADO_POR", "Vendedor de prueba");
        p.put("TOTAL_REGISTROS", String.valueOf(filas.size()));
        p.put("TOTAL_VENTAS", "COP $ 1.500.000");
        p.put("TOTAL_PEDIDOS", "120");
        p.put("TICKET_PROMEDIO", "COP $ 12.500");
        p.put("TOTAL_UNIDADES", "450");
        p.put("TOTAL_BAJOS", "3");
        p.put("TOTAL_BUSQUEDAS", "80");
        p.put("TOTAL_SIN_RESULTADOS", "12");
        return new ReporteDatos(tipo, tipo.getTitulo(), "reporte-test", p, filas);
    }

    private static List<Map<String, ?>> filas(TipoReporte tipo, int n) {
        List<Map<String, ?>> filas = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            filas.add(switch (tipo) {
                case VENTAS -> Map.of(
                        "seccion", "Sección " + (i % 2),
                        "etiqueta", "Camisa niña ñandú " + i,
                        "cantidad", String.valueOf(i),
                        "valor", "$ " + i * 1000,
                        "nivelBarra", i % 11);
                case DEMANDA -> Map.of(
                        "seccion", "Camisa niña ñandú",
                        "etiqueta", "Camisa niña ñandú " + i,
                        "talla", "T" + (i % 8),
                        "unidades", String.valueOf(i + 1),
                        "valorMoneda", "COP $ " + (i + 1) * 1000,
                        "unidadesNum", (long) (i + 1),
                        "valorNum", BigDecimal.valueOf((i + 1) * 1000L));
                case INVENTARIO -> Map.of(
                        "producto", "Camisa niña ñandú " + i,
                        "tienda", "Tienda X",
                        "categoria", "Categoría",
                        "talla", "T" + (i % 8),
                        "stock", "10",
                        "minimo", "3",
                        "nivel", "Disponible");
                case BUSQUEDAS -> Map.of(
                        "termino", "camisa niña ñandú " + i,
                        "busquedas", String.valueOf(i + 1),
                        "sinResultados", String.valueOf(i % 3),
                        "ultima", "28/10/2026 10:00");
            });
        }
        return filas;
    }
}
