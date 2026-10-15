package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AvanzarEstadoRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void guiaValidaPasa() {
        assertThat(validator.validate(new AvanzarEstadoRequest(EstadoPedido.ENVIADO, "COL123"))).isEmpty();
    }

    @Test
    void guiaDeMasDe60Rechazada() {
        var request = new AvanzarEstadoRequest(EstadoPedido.ENVIADO, "A".repeat(61));

        assertThat(validator.validate(request)).anyMatch(v -> v.getPropertyPath().toString().equals("numeroGuia"));
    }

    @Test
    void guiaConSaltoDeLineaRechazada() {
        var request = new AvanzarEstadoRequest(EstadoPedido.ENVIADO, "COL123\nBcc: x@y.com");

        assertThat(validator.validate(request)).anyMatch(v -> v.getPropertyPath().toString().equals("numeroGuia"));
    }
}
