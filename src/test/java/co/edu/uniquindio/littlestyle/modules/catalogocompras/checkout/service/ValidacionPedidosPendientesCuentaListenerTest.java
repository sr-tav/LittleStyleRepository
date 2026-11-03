package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.DespachoRepository;
import co.edu.uniquindio.littlestyle.shared.event.CuentaEliminadaEvent;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ValidacionPedidosPendientesCuentaListenerTest {

    private final PrendaRepository prendaRepository = mock(PrendaRepository.class);
    private final DespachoRepository despachoRepository = mock(DespachoRepository.class);
    private final ValidacionPedidosPendientesCuentaListener listener =
            new ValidacionPedidosPendientesCuentaListener(despachoRepository, prendaRepository);

    @Test
    void bloqueaLasPrendasAntesDeConsultarLosDespachos() {
        when(despachoRepository.existsByVendedorIdAndEstadoIn(eqLong(21L), any())).thenReturn(false);

        listener.alEliminarCuenta(new CuentaEliminadaEvent(21L));

        var orden = org.mockito.Mockito.inOrder(prendaRepository, despachoRepository);
        orden.verify(prendaRepository).bloquearTodasDelVendedor(21L);
        orden.verify(despachoRepository).existsByVendedorIdAndEstadoIn(eqLong(21L), any());
    }

    @Test
    void bloqueaLaEliminacionSiHayDespachosPendientes() {
        when(despachoRepository.existsByVendedorIdAndEstadoIn(eqLong(21L), any())).thenReturn(true);

        assertThatThrownBy(() -> listener.alEliminarCuenta(new CuentaEliminadaEvent(21L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void permiteLaEliminacionSinDespachosPendientes() {
        when(despachoRepository.existsByVendedorIdAndEstadoIn(eqLong(21L), any())).thenReturn(false);

        assertThatCode(() -> listener.alEliminarCuenta(new CuentaEliminadaEvent(21L))).doesNotThrowAnyException();
    }

    private static Long eqLong(long valor) {
        return org.mockito.ArgumentMatchers.eq(valor);
    }
}