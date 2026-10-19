package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.ComponenteRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.TallaRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.MaterialTextil;
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

/**
 * Suite de lógica pura para las reglas del validador de prendas (sin Spring): composición
 * al 100 %, materiales y tallas únicos, rangos coherentes y normalización del nombre de talla.
 */
class ValidadorPrendaTest {

    // Validador directo, no tiene dependencias que mockear
    private final ValidadorPrenda validador = new ValidadorPrenda();
    // Composiciónválida reutilizada (100 % algodón) para concentrarse en lo que se está probando
    private static final List<ComponenteRequest> ALGODON = List.of(new ComponenteRequest(MaterialTextil.ALGODON, 100));

    // Caso base: un request bien armado pasa limpio, sin excepciones
    @Test
    void aceptaUnaPrendaValida() {
        assertThatCode(() -> validador.validar(prendaRequestValida())).doesNotThrowAnyException();
    }

    // Los porcentajes de composición deben llegar exactamente a 100 %, ni más ni menos
    @Test
    void exigeQueLaComposicionSume100() {
        var request = prendaRequest(List.of(new ComponenteRequest(MaterialTextil.ALGODON, 80),
                new ComponenteRequest(MaterialTextil.POLIESTER, 15)), List.of(tallaRequest("4", 100, 110, 15, 20, 1)));

        assertThatThrownBy(() -> validador.validar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("100 %")
                .extracting("campo").isEqualTo("composicion");
    }

    // No vale sumar 100 % repitiendo el mismo material: cada uno puede aparecer una sola vez
    @Test
    void rechazaMaterialesRepetidos() {
        var request = prendaRequest(List.of(new ComponenteRequest(MaterialTextil.ALGODON, 50),
                new ComponenteRequest(MaterialTextil.ALGODON, 50)), List.of(tallaRequest("4", 100, 110, 15, 20, 1)));

        assertThatThrownBy(() -> validador.validar(request)).hasMessageContaining("repetido");
    }

    // La comparación de tallas es tras los trim y mayúsculas: "xs" y " XS " son la misma
    @Test
    void rechazaTallasRepetidasAunqueDifieranEnMayusculasYEspacios() {
        var request = prendaRequest(ALGODON,
                List.of(tallaRequest("xs", 90, 99, 12, 14, 1), tallaRequest(" XS ", 100, 110, 15, 20, 1)));

        assertThatThrownBy(() -> validador.validar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("La talla XS está repetida")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // Estatura y peso no pueden tener el mínimo mayor que el máximo: rangos invertidos fuera
    @Test
    void rechazaRangosInvertidos() {
        var estatura = prendaRequest(ALGODON, List.of(tallaRequest("4", 120, 110, 15, 20, 1)));
        var peso = prendaRequest(ALGODON, List.of(tallaRequest("4", 100, 110, 25, 20, 1)));

        assertThatThrownBy(() -> validador.validar(estatura)).hasMessageContaining("estatura mínima");
        assertThatThrownBy(() -> validador.validar(peso)).hasMessageContaining("peso mínimo");
    }

    // El rango de edad también debe ir en orden: edad mínima > máxima se rechaza
    @Test
    void rechazaEdadMinimaMayorQueLaMaxima() {
        var base = tallaRequest("4", 100, 110, 15, 20, 1);
        var invertida = new TallaRequest(base.talla(), base.estaturaMinCm(), base.estaturaMaxCm(),
                base.pesoMinKg(), base.pesoMaxKg(), base.stock(), 72, 24, null);

        assertThatThrownBy(() -> validador.validar(prendaRequest(ALGODON, List.of(invertida))))
                .hasMessageContaining("edad mínima");
    }

    // Normalización consistente: recorta espacios y pone la talla en mayúsculas
    @Test
    void normalizaElNombreDeLaTalla() {
        assertThat(ValidadorPrenda.normalizarTalla("  2  t ")).isEqualTo("2 T");
    }
}
