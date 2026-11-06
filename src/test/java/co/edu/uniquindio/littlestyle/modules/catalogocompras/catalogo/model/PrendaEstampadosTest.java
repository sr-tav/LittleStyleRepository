package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Estampado;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Estampados que una prenda ofrece al cliente: la lista declarada o, en prendas antiguas, su único estampado. */
class PrendaEstampadosTest {

    @Test
    void ofreceLosEstampadosDeclaradosPorElVendedor() {
        Prenda prenda = CatalogoFixtures.prenda(1L, 2, 8, 4);
        prenda.getEstampados().add(Estampado.FLORES);
        prenda.getEstampados().add(Estampado.RAYAS);

        assertThat(prenda.estampadosDisponibles()).containsExactlyInAnyOrder(Estampado.FLORES, Estampado.RAYAS);
    }

    @Test
    void laListaTienePrioridadSobreElEstampadoUnicoAnterior() {
        Prenda prenda = CatalogoFixtures.prenda(1L, 2, 8, 4);
        prenda.setEstampado(Estampado.LISO);
        prenda.getEstampados().add(Estampado.LUNARES);

        assertThat(prenda.estampadosDisponibles()).containsExactly(Estampado.LUNARES);
    }

    @Test
    void unaPrendaAnteriorSoloOfreceSuEstampadoUnico() {
        Prenda prenda = CatalogoFixtures.prenda(1L, 2, 8, 4);
        prenda.setEstampado(Estampado.ESTRELLAS);

        assertThat(prenda.estampadosDisponibles()).containsExactly(Estampado.ESTRELLAS);
    }

    @Test
    void unaPrendaSinEstampadoNoOfreceNinguno() {
        assertThat(CatalogoFixtures.prenda(1L, 2, 8, 4).estampadosDisponibles()).isEmpty();
    }
}
