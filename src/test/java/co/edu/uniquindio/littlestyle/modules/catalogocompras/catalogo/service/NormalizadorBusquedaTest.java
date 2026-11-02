package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NormalizadorBusquedaTest {

    @Test
    void quitaTildesMinusculasYColapsaEspacios() {
        assertThat(NormalizadorBusqueda.normalizar("  Vestido   Ñandú  Rojo ")).isEqualTo("vestido nandu rojo");
    }

    @Test
    void nuloOVacioSeConvierteEnCadenaVacia() {
        assertThat(NormalizadorBusqueda.normalizar(null)).isEmpty();
        assertThat(NormalizadorBusqueda.normalizar("   ")).isEmpty();
    }

    @Test
    void recortaAlAnchoDeLaColumna() {
        String largo = "a".repeat(NormalizadorBusqueda.LONGITUD_MAXIMA + 50);
        assertThat(NormalizadorBusqueda.normalizar(largo))
                .hasSize(NormalizadorBusqueda.LONGITUD_MAXIMA);
    }
}
