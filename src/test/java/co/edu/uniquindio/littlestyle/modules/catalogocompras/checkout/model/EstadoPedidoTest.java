package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model;

import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EstadoPedidoTest {

    @Test
    void permiteElFlujoNormalYTerminaEnEnviado() {
        Pedido pedido = Pedido.builder().estado(EstadoPedido.PENDIENTE_PAGO).build();

        pedido.transicionarA(EstadoPedido.PAGADO);
        pedido.transicionarA(EstadoPedido.REGISTRADO);
        pedido.transicionarA(EstadoPedido.EN_PREPARACION);
        pedido.transicionarA(EstadoPedido.ENVIADO);

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.ENVIADO);
        assertThat(EstadoPedido.ENVIADO.permiteTransicionA(EstadoPedido.CANCELADO)).isFalse();
    }

    @Test
    void permiteCancelarAntesDeDespacharYNoPermiteReabrirPedidoCancelado() {
        Pedido pedido = Pedido.builder().estado(EstadoPedido.PENDIENTE_PAGO).build();

        pedido.transicionarA(EstadoPedido.CANCELADO);

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CANCELADO);
        assertThatThrownBy(() -> pedido.transicionarA(EstadoPedido.PAGADO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No se puede cambiar el pedido");
    }

    @Test
    void rechazaSaltarEstadosDelFlujo() {
        Pedido pedido = Pedido.builder().estado(EstadoPedido.PENDIENTE_PAGO).build();

        assertThatThrownBy(() -> pedido.transicionarA(EstadoPedido.ENVIADO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PENDIENTE_PAGO")
                .hasMessageContaining("ENVIADO");
    }

    @Test
    void pagadoSoloRegistraYRegistradoSoloPrepara() {
        assertThat(EstadoPedido.PAGADO.permiteTransicionA(EstadoPedido.EN_PREPARACION)).isFalse();
        assertThat(EstadoPedido.PAGADO.permiteTransicionA(EstadoPedido.REGISTRADO)).isTrue();
        assertThat(EstadoPedido.REGISTRADO.permiteTransicionA(EstadoPedido.EN_PREPARACION)).isTrue();
        assertThat(EstadoPedido.REGISTRADO.etiqueta()).isEqualTo("Registrado");
    }
}
