package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures.prendaRequestValida;
import static org.assertj.core.api.Assertions.assertThat;

/** Bean Validation real sobre {@link PrendaRequest}: nombre con letras, descripción y color únicos. */
class PrendaRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private PrendaRequest con(String nombre, String descripcion, Set<ColorPreferido> colores) {
        PrendaRequest b = prendaRequestValida();
        return new PrendaRequest(nombre, b.categoria(), descripcion, b.instruccionesCuidado(), b.marca(),
                b.precio(), colores, b.estampado(), b.genero(), b.tintesSinteticos(), b.brochesMetalicos(),
                b.contieneNiquel(), b.tratamientoFormaldehido(), b.composicion(), b.tallas());
    }

    private boolean falla(PrendaRequest r, String propiedad) {
        return validator.validate(r).stream().anyMatch(v -> v.getPropertyPath().toString().equals(propiedad));
    }

    @Test
    void laSolicitudValidaNoTieneViolaciones() {
        assertThat(validator.validate(prendaRequestValida())).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "123 456", "---", "007"})
    void rechazaNombresSinLetras(String nombre) {
        assertThat(falla(con(nombre, "Cómodo", Set.of(ColorPreferido.AZUL)), "nombre")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Camisa 2", "Body 3M", "Ñandú", "Vestido"})
    void aceptaNombresConLetras(String nombre) {
        assertThat(falla(con(nombre, "Cómodo", Set.of(ColorPreferido.AZUL)), "nombre")).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void descripcionEsObligatoria(String descripcion) {
        assertThat(falla(con("Camisa 2", descripcion, Set.of(ColorPreferido.AZUL)), "descripcion")).isTrue();
    }

    @Test
    void exigeEntreUnoYDiezColores() {
        assertThat(falla(con("Camisa 2", "Cómodo", null), "colores")).isTrue();
        assertThat(falla(con("Camisa 2", "Cómodo", Set.of()), "colores")).isTrue();
        assertThat(falla(con("Camisa 2", "Cómodo", Set.of(ColorPreferido.AZUL, ColorPreferido.ROJO)), "colores")).isFalse();
    }
}