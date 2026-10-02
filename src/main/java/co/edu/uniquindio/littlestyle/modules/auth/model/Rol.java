package co.edu.uniquindio.littlestyle.modules.auth.model;

/**
 * Roles de la plataforma. El prefijo ROLE_ lo agrega Spring Security al construir las authorities.
 */
public enum Rol {
    CLIENTE,
    VENDEDOR,
    ADMINISTRADOR;

    public String authority() {
        return "ROLE_" + name();
    }
}
