package co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class NivelStockTest {

    @ParameterizedTest(name = "stock {0} con mínimo {1} → {2}")
    @CsvSource({
            "0, 5, AGOTADO",
            "0, 0, AGOTADO",
            "1, 5, STOCK_BAJO",
            "5, 5, STOCK_BAJO",   // alcanzar el mínimo ya alerta (RNF-14)
            "6, 5, DISPONIBLE",
            "1, 0, DISPONIBLE"
    })
    void calculaElNivelSegunElMinimo(int stock, int minimo, NivelStock esperado) {
        assertThat(NivelStock.calcular(stock, minimo)).isEqualTo(esperado);
    }
}
