package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.config.CheckoutProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;

/** Calcula el costo de envío según el destino. */
@Service
@RequiredArgsConstructor
public class TarifaEnvioService {

    private final CheckoutProperties properties;

    /**
     * Tarifa de Armenia, del resto de municipios del Quindío configurados o de otras ciudades. Compara sin
     * tildes ni mayúsculas.
     */
    public BigDecimal calcular(String departamento, String municipio) {
        String departamentoNormalizado = normalizar(departamento);
        String municipioNormalizado = normalizar(municipio);
        if (departamentoNormalizado.equals("QUINDIO") && municipioNormalizado.equals("ARMENIA")) {
            return properties.costoEnvioArmenia();
        }
        if (departamentoNormalizado.equals("QUINDIO") && properties.municipiosQuindio().stream()
                .map(TarifaEnvioService::normalizar)
                .anyMatch(municipioNormalizado::equals)) {
            return properties.costoEnvioQuindio();
        }
        return properties.costoEnvioOtrasCiudades();
    }

    /** Texto sin tildes, sin espacios sobrantes y en mayúsculas. */
    private static String normalizar(String valor) {
        return Normalizer.normalize(valor == null ? "" : valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
    }
}
