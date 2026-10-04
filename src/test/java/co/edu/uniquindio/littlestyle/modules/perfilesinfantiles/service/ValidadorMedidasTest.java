package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.service;

import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidadorMedidasTest {

    private final ValidadorMedidas validador = new ValidadorMedidas();

    @Test
    void aceptaMedidasCoherentesConLaEdad() {
        assertThatCode(() -> validador.validar(LocalDate.now().minusYears(5),
                LocalDate.now(), new BigDecimal("110"), new BigDecimal("22")))
                .doesNotThrowAnyException();
    }

    @Test
    void rechazaMedicionAnteriorAlNacimientoEnElCampoCorrespondiente() {
        assertThatThrownBy(() -> validador.validar(LocalDate.now().minusYears(5),
                LocalDate.now().minusYears(6), new BigDecimal("110"), new BigDecimal("22")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException error = (BusinessException) ex;
                    org.assertj.core.api.Assertions.assertThat(error.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    org.assertj.core.api.Assertions.assertThat(error.getCampo()).isEqualTo("fechaMedicion");
                });
    }

    @Test
    void rechazaEstaturaFueraDelRangoDelTramoEtario() {
        assertThatThrownBy(() -> validador.validar(LocalDate.now().minusYears(3),
                LocalDate.now(), new BigDecimal("150"), new BigDecimal("20")))
                .isInstanceOf(BusinessException.class)
                .extracting("campo").isEqualTo("estaturaCm");
    }
}
