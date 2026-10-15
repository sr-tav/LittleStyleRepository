package co.edu.uniquindio.littlestyle.shared.correo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Correo transaccional por Gmail SMTP (US-12, ADR-07). Todo secreto sale de
 * variables de entorno; en Gmail la clave es una contraseña de aplicación.
 */
@ConfigurationProperties(prefix = "app.correo")
public record CorreoProperties(
        @DefaultValue("smtp.gmail.com") String host,
        @DefaultValue("587") int puerto,
        String usuario,
        String clave,
        @DefaultValue("LittleStyle") String remitente) {

    /** Sin usuario o clave no se envía nada: se registra y se continúa. */
    public boolean enviosHabilitados() {
        return usuario != null && !usuario.isBlank() && clave != null && !clave.isBlank();
    }
}
