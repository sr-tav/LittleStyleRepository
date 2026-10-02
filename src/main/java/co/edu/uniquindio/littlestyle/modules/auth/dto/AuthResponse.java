package co.edu.uniquindio.littlestyle.modules.auth.dto;

/**
 * Respuesta de login/registro.
 *
 * @param token    JWT firmado
 * @param tipo     siempre "Bearer"
 * @param expiraEn instante de expiración en milisegundos desde epoch
 * @param usuario  datos públicos del usuario autenticado
 */
public record AuthResponse(
        String token,
        String tipo,
        long expiraEn,
        UsuarioResponse usuario
) {
    public static AuthResponse bearer(String token, long expiraEn, UsuarioResponse usuario) {
        return new AuthResponse(token, "Bearer", expiraEn, usuario);
    }
}
