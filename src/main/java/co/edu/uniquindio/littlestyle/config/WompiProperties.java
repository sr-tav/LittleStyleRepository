package co.edu.uniquindio.littlestyle.config;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración de la pasarela Wompi (US-11). Las llaves y secretos se leen de variables de entorno
 * y nunca se versionan; si faltan, el inicio de pagos responde que la pasarela no está configurada.
 */
@ConfigurationProperties(prefix = "app.wompi")
public record WompiProperties(
        @DefaultValue("https://sandbox.wompi.co/v1") String apiUrl,
        String llavePublica,
        String secretoIntegridad,
        String secretoEventos,
        String urlRedireccion,
        @DefaultValue("COP") String moneda
) {
    public WompiProperties {
        llavePublica = limpiar(llavePublica);
        secretoIntegridad = limpiar(secretoIntegridad);
        secretoEventos = limpiar(secretoEventos);
        urlRedireccion = limpiar(urlRedireccion);
    }

    /**
     * URL de redirección que se envía al Widget, o {@code null} si no aplica. Wompi responde 403 al
     * abrir el checkout con una redirect-url local (localhost/127.0.0.1); en ese caso se omite y el
     * callback del Widget lleva al cliente a la página de resultado.
     */
    public String urlRedireccionPublica() {
        if (urlRedireccion.isEmpty()) {
            return null;
        }
        try {
            String host = URI.create(urlRedireccion).getHost();
            if (host == null || host.equalsIgnoreCase("localhost") || host.startsWith("127.")
                    || host.equals("0.0.0.0") || host.equals("[::1]")) {
                return null;
            }
            return urlRedireccion;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean pagosConfigurados() {
        return !llavePublica.isEmpty() && !secretoIntegridad.isEmpty();
    }

    public boolean eventosConfigurados() {
        return !secretoEventos.isEmpty();
    }

    private static String limpiar(String valor) {
        return valor == null ? "" : valor.trim();
    }

    /** Evita que los secretos terminen en logs al imprimir la configuración. */
    @Override
    public String toString() {
        return "WompiProperties[apiUrl=" + apiUrl + ", llavePublica=" + llavePublica
                + ", secretoIntegridad=****, secretoEventos=****, urlRedireccion=" + urlRedireccion
                + ", moneda=" + moneda + "]";
    }
}
