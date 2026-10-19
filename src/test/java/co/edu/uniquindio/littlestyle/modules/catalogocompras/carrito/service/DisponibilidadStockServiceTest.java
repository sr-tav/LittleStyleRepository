package co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.service;

import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Suite sin mocks: DisponibilidadStockService es lógica pura, así que se prueba
 * directo. Cubre que la validación compare solo contra la existencia física
 * (sin descontar unidades reservadas por otros carritos) y los errores de conflicto.
 */
class DisponibilidadStockServiceTest {

    // Instancia directa del servicio porque no tiene dependencias externas
    private final DisponibilidadStockService servicio = new DisponibilidadStockService();

    // Pedir justo el stock disponible es válido, aunque otros carritos reserven unidades
    @Test
    void validaSoloContraExistenciaFisicaSinReservarUnidadesDeOtrosCarritos() {
        servicio.validarDisponibilidad(5, 5);
        assertThat(servicio.hayDisponibilidad(5, 5)).isTrue();
    }

    // Si la cantidad pedida supera el stock físico, el error debe ser un 409 Conflict
    @Test
    void rechazaCantidadMayorAlStockFisico() {
        assertThatThrownBy(() -> servicio.validarDisponibilidad(5, 6))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getStatus())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    // El chequeo booleano también debe bajar a false cuando el stock ya no alcanza
    @Test
    void informaNoDisponibleCuandoLaExistenciaFisicaBajaDeLaCantidadDelCarrito() {
        assertThat(servicio.hayDisponibilidad(1, 2)).isFalse();
    }
}
