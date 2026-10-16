package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoPedidoDerivadoTest {

    @Test
    void todosRegistradoQuedaRegistrado() {
        assertThat(EstadoPedido.calcularDerivado(
                List.of(EstadoPedido.REGISTRADO, EstadoPedido.REGISTRADO)))
                .isEqualTo(EstadoPedido.REGISTRADO);
    }

    @Test
    void algunoEnPreparacionQuedaEnPreparacion() {
        assertThat(EstadoPedido.calcularDerivado(
                List.of(EstadoPedido.REGISTRADO, EstadoPedido.EN_PREPARACION)))
                .isEqualTo(EstadoPedido.EN_PREPARACION);
    }

    @Test
    void algunoEnviadoYOtroNoQuedaParcialmenteEnviado() {
        assertThat(EstadoPedido.calcularDerivado(
                List.of(EstadoPedido.ENVIADO, EstadoPedido.REGISTRADO)))
                .isEqualTo(EstadoPedido.PARCIALMENTE_ENVIADO);
        assertThat(EstadoPedido.calcularDerivado(
                List.of(EstadoPedido.ENVIADO, EstadoPedido.EN_PREPARACION)))
                .isEqualTo(EstadoPedido.PARCIALMENTE_ENVIADO);
    }

    @Test
    void todosEnviadosQuedaEnviado() {
        assertThat(EstadoPedido.calcularDerivado(
                List.of(EstadoPedido.ENVIADO, EstadoPedido.ENVIADO)))
                .isEqualTo(EstadoPedido.ENVIADO);
    }
}
