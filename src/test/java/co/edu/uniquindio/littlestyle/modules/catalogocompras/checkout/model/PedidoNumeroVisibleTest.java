package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Auditoría 4.2: el cliente ve su consecutivo, no el id de la base de datos. */
class PedidoNumeroVisibleTest {

    @Test
    void elClienteVeSuConsecutivoYNoElIdInterno() {
        Pedido pedido = Pedido.builder().id(4521L).numeroCliente(3).build();

        assertThat(pedido.numeroVisible()).isEqualTo(3L);
        assertThat(pedido.getId()).isEqualTo(4521L);
    }

    @Test
    void sinConsecutivoNoExponeElIdInterno() {
        Pedido anteriorALaMigracion = Pedido.builder().id(12L).build();

        assertThat(anteriorALaMigracion.numeroVisible()).isNull();
    }
}
