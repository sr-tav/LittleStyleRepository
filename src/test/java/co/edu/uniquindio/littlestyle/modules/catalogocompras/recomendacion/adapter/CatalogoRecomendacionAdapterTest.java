package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.adapter;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.ImagenPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.MaterialTextil;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.PrendaRecomendable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures.prenda;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoRecomendacionAdapterTest {

    @Mock
    private PrendaRepository prendaRepository;
    @InjectMocks
    private CatalogoRecomendacionAdapter adapter;

    @Test
    void convierteLaPrendaConTablaDeTallasComposicionYStockPorTalla() {
        Prenda p = prenda(10L, 5, 3, 0);
        p.setContieneNiquel(true);
        p.getImagenes().add(ImagenPrenda.builder().id(1L).orden(0).url("https://cdn/a.png").clave("a").build());
        when(prendaRepository.findAllByEstado(EstadoPrenda.ACTIVA)).thenReturn(List.of(p));

        PrendaRecomendable r = adapter.listarPrendasPublicadas().getFirst();

        assertThat(r.id()).isEqualTo(10L);
        assertThat(r.categoria()).isEqualTo("PANTALONES");
        assertThat(r.marca()).isEqualTo("Tienda Sol"); // sin marca propia usa el nombre de la tienda
        assertThat(r.imagenUrl()).isEqualTo("https://cdn/a.png");
        assertThat(r.tablaTallas().rangos()).extracting("talla").containsExactly("4", "6");
        assertThat(r.composicion()).extracting("material").containsExactly(MaterialTextil.ALGODON, MaterialTextil.POLIESTER);
        assertThat(r.contieneNiquel()).isTrue();
        assertThat(r.tieneStock("4")).isTrue();
        assertThat(r.tieneStock("6")).isFalse();
    }

    @Test
    void omiteUnaPrendaConDatosInvalidosSinAfectarALasDemas() {
        Prenda rota = prenda(11L, 5, 1, 1);
        rota.getTallas().getFirst().setEstaturaMinCm(new BigDecimal("150")); // mínimo > máximo
        when(prendaRepository.findAllByEstado(EstadoPrenda.ACTIVA)).thenReturn(List.of(rota, prenda(12L, 5, 1, 1)));

        assertThat(adapter.listarPrendasPublicadas()).extracting(PrendaRecomendable::id).containsExactly(12L);
    }
}
