package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrendaModeracionTest {

    @Test
    void nuevaPrendaNaceSinRevisar() {
        Prenda prenda = new Prenda();

        prenda.moderacionInicial();

        assertThat(prenda.getEstadoModeracion()).isEqualTo(EstadoModeracion.SIN_REVISAR);
    }
}
