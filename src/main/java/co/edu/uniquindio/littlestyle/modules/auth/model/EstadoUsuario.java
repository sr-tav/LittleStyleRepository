package co.edu.uniquindio.littlestyle.modules.auth.model;

/**
 * Estado de una cuenta. Solo las cuentas {@code ACTIVO} pueden iniciar sesión y usar sus tokens; {@code
 * ELIMINADO} corresponde a cuentas anonimizadas por su titular.
 */
public enum EstadoUsuario {
    ACTIVO,
    SUSPENDIDO,
    ELIMINADO
}
