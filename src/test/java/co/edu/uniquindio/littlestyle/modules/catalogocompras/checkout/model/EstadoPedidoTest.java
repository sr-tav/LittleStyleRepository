package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model;

import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EstadoPedidoTest {

    @Test
    void permiteElFlujoNormalYTerminaEnEntregado() {
        Pedido pedido = Pedido.builder().estado(EstadoPedido.PENDIENTE_PAGO).build();

        pedido.transicionarA(EstadoPedido.PAGADO);
        pedido.transicionarA(EstadoPedido.EN_PREPARACION);
        pedido.transicionarA(EstadoPedido.ENVIADO);
        pedido.transicionarA(EstadoPedido.ENTREGADO);

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.ENTREGADO);
        assertThat(EstadoPedido.ENTREGADO.permiteTransicionA(EstadoPedido.CANCELADO)).isFalse();
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
}
