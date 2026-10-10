package co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.service;

import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DisponibilidadStockServiceTest {

    private final DisponibilidadStockService servicio = new DisponibilidadStockService();

    @Test
    void validaSoloContraExistenciaFisicaSinReservarUnidadesDeOtrosCarritos() {
        servicio.validarDisponibilidad(5, 5);
        assertThat(servicio.hayDisponibilidad(5, 5)).isTrue();
    }

    @Test
    void rechazaCantidadMayorAlStockFisico() {
        assertThatThrownBy(() -> servicio.validarDisponibilidad(5, 6))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getStatus())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void informaNoDisponibleCuandoLaExistenciaFisicaBajaDeLaCantidadDelCarrito() {
        assertThat(servicio.hayDisponibilidad(1, 2)).isFalse();
    }
}
