package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrendaVarianteTest {

    private static Prenda prenda(ColorPreferido... colores) {
        Prenda p = Prenda.builder().id(1L).build();
        for (ColorPreferido c : colores) {
            p.getTallas().add(TallaPrenda.builder().prenda(p).talla("4").color(c).stock(3).build());
        }
        return p;
    }

    @Test
    void conColorDevuelveEsaVarianteExacta() {
        Prenda p = prenda(ColorPreferido.AZUL, ColorPreferido.ROJO);
        assertThat(p.buscarVariante("4", ColorPreferido.ROJO)).get()
                .extracting(TallaPrenda::getColor).isEqualTo(ColorPreferido.ROJO);
        assertThat(p.buscarVariante("4", ColorPreferido.VERDE)).isEmpty();
    }

    @Test
    void sinColorSoloResuelveCuandoLaTallaTieneUnaVariante() {
        assertThat(prenda(ColorPreferido.AZUL).buscarVariante(" 4 ", null)).isPresent();
        assertThat(prenda(ColorPreferido.AZUL, ColorPreferido.ROJO).buscarVariante("4", null)).isEmpty();
    }

    @Test
    void laEtiquetaIncluyeElColor() {
        assertThat(TallaPrenda.etiqueta("4", ColorPreferido.ROJO)).isEqualTo("4 · Rojo");
        assertThat(TallaPrenda.etiqueta("4", null)).isEqualTo("4");
    }
}