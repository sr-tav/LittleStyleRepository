package co.edu.uniquindio.littlestyle.shared.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Logger dedicado a eventos de seguridad (rechazos de autenticación y autorización), con un nombre
 * propio para poder filtrarlo o enviarlo a otro destino desde la configuración de logging.
 * Nunca se registran contraseñas ni tokens; el correo se enmascara.
 */
public final class SeguridadLog {

    public static final Logger LOG = LoggerFactory.getLogger("littlestyle.seguridad");

    private SeguridadLog() {
    }

    /** Conserva la inicial y el dominio: {@code maria@correo.com} → {@code m***@correo.com}. */
    public static String enmascararEmail(String email) {
        if (email == null || email.isBlank()) {
            return "-";
        }
        email = email.trim().toLowerCase(Locale.ROOT);
        int arroba = email.indexOf('@');
        if (arroba <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(arroba);
    }
}
