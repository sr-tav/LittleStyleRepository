package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.ComponenteRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.TallaRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.MaterialTextil;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

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

    private static TallaRequest variante(String talla, int estMin, int estMax, ColorPreferido color) {
        return new TallaRequest(talla, BigDecimal.valueOf(estMin), BigDecimal.valueOf(estMax),
                BigDecimal.valueOf(15), BigDecimal.valueOf(20), 5, null, null, null, color);
    }

    private PrendaRequest conVariantes(Set<ColorPreferido> colores, List<TallaRequest> tallas) {
        PrendaRequest b = prendaRequestValida();
        return new PrendaRequest(b.nombre(), b.categoria(), b.descripcion(), b.instruccionesCuidado(), b.marca(),
                b.precio(), colores, b.estampado(), b.genero(), b.tintesSinteticos(), b.brochesMetalicos(),
                b.contieneNiquel(), b.tratamientoFormaldehido(), b.composicion(), tallas);
    }

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
                .hasMessage("La talla XS en color ROSA está repetida")
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

    @Test
    void rechazaNombresSinLetras() {
        for (String nombre : List.of("12345", "123 456", "---", "007")) {
            var base = prendaRequestValida();
            var request = new co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaRequest(
                    nombre, base.categoria(), base.descripcion(), base.instruccionesCuidado(), base.marca(),
                    base.precio(), base.colores(), base.estampado(), base.genero(), base.tintesSinteticos(),
                    base.brochesMetalicos(), base.contieneNiquel(), base.tratamientoFormaldehido(),
                    base.composicion(), base.tallas());
            assertThatThrownBy(() -> validador.validar(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("campo").isEqualTo("nombre");
        }
    }

    @Test
    void aceptaNombresAlfanumericosConLetras() {
        for (String nombre : List.of("Camisa 2", "Body 3M", "Ñandú", "Vestido")) {
            var base = prendaRequestValida();
            var request = new co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaRequest(
                    nombre, base.categoria(), base.descripcion(), base.instruccionesCuidado(), base.marca(),
                    base.precio(), base.colores(), base.estampado(), base.genero(), base.tintesSinteticos(),
                    base.brochesMetalicos(), base.contieneNiquel(), base.tratamientoFormaldehido(),
                    base.composicion(), base.tallas());
            assertThatCode(() -> validador.validar(request)).doesNotThrowAnyException();
        }
    }

    @Test
    void rechazaDescripcionVacia() {
        var base = prendaRequestValida();
        var request = new co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaRequest(
                base.nombre(), base.categoria(), "   ", base.instruccionesCuidado(), base.marca(),
                base.precio(), base.colores(), base.estampado(), base.genero(), base.tintesSinteticos(),
                base.brochesMetalicos(), base.contieneNiquel(), base.tratamientoFormaldehido(),
                base.composicion(), base.tallas());
        assertThatThrownBy(() -> validador.validar(request))
                .extracting("campo").isEqualTo("descripcion");
    }

    @Test
    void aceptaLaMismaTallaEnVariosColoresConLosMismosRangos() {
        var request = conVariantes(Set.of(ColorPreferido.AZUL, ColorPreferido.ROJO), List.of(
                variante("4", 100, 110, ColorPreferido.AZUL), variante("4", 100, 110, ColorPreferido.ROJO)));
        assertThatCode(() -> validador.validar(request)).doesNotThrowAnyException();
    }

    @Test
    void rechazaLaMismaTallaConRangosDistintosEntreColores() {
        var request = conVariantes(Set.of(ColorPreferido.AZUL, ColorPreferido.ROJO), List.of(
                variante("4", 100, 110, ColorPreferido.AZUL), variante("4", 100, 115, ColorPreferido.ROJO)));
        assertThatThrownBy(() -> validador.validar(request)).hasMessageContaining("mismos rangos");
    }

    @Test
    void rechazaVarianteRepetidaYVarianteSinColorConVariosColores() {
        var repetida = conVariantes(Set.of(ColorPreferido.AZUL), List.of(
                variante("4", 100, 110, ColorPreferido.AZUL), variante("4", 100, 110, ColorPreferido.AZUL)));
        assertThatThrownBy(() -> validador.validar(repetida)).hasMessageContaining("repetida");

        var sinColor = conVariantes(Set.of(ColorPreferido.AZUL, ColorPreferido.ROJO),
                List.of(variante("4", 100, 110, null)));
        assertThatThrownBy(() -> validador.validar(sinColor)).hasMessageContaining("color");
    }

    @Test
    void exigeAlMenosUnaTallaPorCadaColorYSoloColoresDeclarados() {
        var colorSinTalla = conVariantes(Set.of(ColorPreferido.AZUL, ColorPreferido.ROJO),
                List.of(variante("4", 100, 110, ColorPreferido.AZUL)));
        assertThatThrownBy(() -> validador.validar(colorSinTalla)).hasMessageContaining("al menos una talla");

        var noDeclarado = conVariantes(Set.of(ColorPreferido.AZUL),
                List.of(variante("4", 100, 110, ColorPreferido.ROJO)));
        assertThatThrownBy(() -> validador.validar(noDeclarado)).hasMessageContaining("no está entre los colores");
    }
}
