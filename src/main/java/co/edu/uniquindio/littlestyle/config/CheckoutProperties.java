package co.edu.uniquindio.littlestyle.config;

import jakarta.validation.constraints.DecimalMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app.checkout")
public record CheckoutProperties(
        @DefaultValue("7000")
        @DecimalMin(value = "0.00", message = "La tarifa de Armenia no puede ser negativa")
        BigDecimal costoEnvioArmenia,
        @DefaultValue("10000")
        @DecimalMin(value = "0.00", message = "La tarifa del Quindío no puede ser negativa")
        BigDecimal costoEnvioQuindio,
        @DefaultValue("15000")
        @DecimalMin(value = "0.00", message = "La tarifa de otras ciudades no puede ser negativa")
        BigDecimal costoEnvioOtrasCiudades,
        @DefaultValue({
                "CALARCA", "CIRCASIA", "CORDOBA", "FILANDIA", "GENOVA", "LA TEBAIDA",
                "MONTENEGRO", "PIJAO", "QUIMBAYA", "SALENTO", "BUENAVISTA"
        }) List<String> municipiosQuindio,
        @DefaultValue("PT30M") Duration tiempoExpiracionPedido
) {
    public CheckoutProperties {
        municipiosQuindio = municipiosQuindio == null
                ? List.of()
                : municipiosQuindio.stream().map(String::trim).map(String::toUpperCase).toList();
        if (tiempoExpiracionPedido == null || tiempoExpiracionPedido.isZero()
                || tiempoExpiracionPedido.isNegative()) {
            throw new IllegalArgumentException("El tiempo de expiración de pedidos debe ser positivo");
        }
    }
}
