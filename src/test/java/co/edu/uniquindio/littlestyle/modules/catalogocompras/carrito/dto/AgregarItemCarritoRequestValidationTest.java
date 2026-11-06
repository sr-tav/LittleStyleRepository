package co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.dto;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgregarItemCarritoRequestValidationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void exigeColorAlAgregarAlCarrito() {
        var request = new AgregarItemCarritoRequest(
                7L, "4", 1, null, 11L, null, null);

        var violaciones = validator.validate(request);

        assertThat(violaciones).anySatisfy(violacion -> {
            assertThat(violacion.getPropertyPath().toString()).isEqualTo("color");
            assertThat(violacion.getMessage()).contains("Elige un color");
        });
    }

    @Test
    void aceptaUnaSolicitudConColor() {
        var request = new AgregarItemCarritoRequest(
                7L, "4", 1, null, 11L, ColorPreferido.AZUL, null);

        assertThat(validator.validate(request)).isEmpty();
    }
}