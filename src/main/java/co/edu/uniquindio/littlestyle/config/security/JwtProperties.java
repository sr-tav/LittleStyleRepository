package co.edu.uniquindio.littlestyle.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de JWT (prefijo {@code app.jwt}).
 *
 * @param secret       clave HMAC codificada en Base64 (mínimo 256 bits)
 * @param expirationMs tiempo de vida del token en milisegundos
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMs) {
}
