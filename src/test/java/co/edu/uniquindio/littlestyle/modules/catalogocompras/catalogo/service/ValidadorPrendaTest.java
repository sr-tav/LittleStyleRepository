package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.ComponenteRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.MaterialTextil;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures.prendaRequest;
import static co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures.prendaRequestValida;
import static co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures.tallaRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidadorPrendaTest {

    private final ValidadorPrenda validador = new ValidadorPrenda();
    private static final List<ComponenteRequest> ALGODON = List.of(new ComponenteRequest(MaterialTextil.ALGODON, 100));

    @Test
    void aceptaUnaPrendaValida() {
        assertThatCode(() -> validador.validar(prendaRequestValida())).doesNotThrowAnyException();
    }

    @Test
    void exigeQueLaComposicionSume100() {
        var request = prendaRequest(List.of(new ComponenteRequest(MaterialTextil.ALGODON, 80),
                new ComponenteRequest(MaterialTextil.POLIESTER, 15)), List.of(tallaRequest("4", 100, 110, 15, 20, 1)));

        assertThatThrownBy(() -> validador.validar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("100 %")
                .extracting("campo").isEqualTo("composicion");
    }

    @Test
    void rechazaMaterialesRepetidos() {
        var request = prendaRequest(List.of(new ComponenteRequest(MaterialTextil.ALGODON, 50),
                new ComponenteRequest(MaterialTextil.ALGODON, 50)), List.of(tallaRequest("4", 100, 110, 15, 20, 1)));

        assertThatThrownBy(() -> validador.validar(request)).hasMessageContaining("repetido");
    }

    @Test
    void rechazaTallasRepetidasAunqueDifieranEnMayusculasYEspacios() {
        var request = prendaRequest(ALGODON,
                List.of(tallaRequest("xs", 90, 99, 12, 14, 1), tallaRequest(" XS ", 100, 110, 15, 20, 1)));

        assertThatThrownBy(() -> validador.validar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("La talla XS está repetida")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rechazaRangosInvertidos() {
        var estatura = prendaRequest(ALGODON, List.of(tallaRequest("4", 120, 110, 15, 20, 1)));
        var peso = prendaRequest(ALGODON, List.of(tallaRequest("4", 100, 110, 25, 20, 1)));

        assertThatThrownBy(() -> validador.validar(estatura)).hasMessageContaining("estatura mínima");
        assertThatThrownBy(() -> validador.validar(peso)).hasMessageContaining("peso mínimo");
    }

    @Test
    void normalizaElNombreDeLaTalla() {
        assertThat(ValidadorPrenda.normalizarTalla("  2  t ")).isEqualTo("2 T");
    }
}
