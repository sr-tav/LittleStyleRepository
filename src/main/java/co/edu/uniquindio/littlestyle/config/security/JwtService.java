package co.edu.uniquindio.littlestyle.config.security;

import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Emisión y validación de JSON Web Tokens firmados con HMAC-SHA.
 */
@Service
public class JwtService {

    static final String CLAIM_ROL = "rol";
    static final String CLAIM_ID = "uid";
    static final String CLAIM_NOMBRE = "nombre";

    private final SecretKey key;
    private final long expirationMs;

    /**
     * Construye la llave HMAC a partir del secreto en Base64. Falla al arrancar si el secreto no está
     * configurado.
     */
    public JwtService(JwtProperties properties) {
        if (properties.secret() == null || properties.secret().isBlank()) {
            throw new IllegalStateException("app.jwt.secret no está configurado");
        }
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.expirationMs = properties.expirationMs();
    }

    /**
     * Emite un JWT con el correo como sujeto y el id, rol y nombre del usuario como claims, válido durante el
     * tiempo configurado.
     */
    public TokenEmitido generarToken(Usuario usuario) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expirationMs);
        String token = Jwts.builder()
                .subject(usuario.getEmail())
                .claim(CLAIM_ID, usuario.getId())
                .claim(CLAIM_ROL, usuario.getRol().name())
                .claim(CLAIM_NOMBRE, usuario.getNombre())
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(key)
                .compact();
        return new TokenEmitido(token, expiracion.getTime());
    }

    /**
     * Valida firma y expiración.
     *
     * @throws JwtException si el token es inválido o expiró
     */
    public Claims validarToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Convierte los claims de un token ya validado en el principal que usa Spring Security. */
    public AuthenticatedUser toAuthenticatedUser(Claims claims) {
        Number id = claims.get(CLAIM_ID, Number.class);
        return new AuthenticatedUser(
                id == null ? null : id.longValue(),
                claims.getSubject(),
                claims.get(CLAIM_NOMBRE, String.class),
                Rol.valueOf(claims.get(CLAIM_ROL, String.class))
        );
    }

    /** Token firmado y su instante de expiración en milisegundos desde epoch. */
    public record TokenEmitido(String token, long expiraEn) {
    }
}
