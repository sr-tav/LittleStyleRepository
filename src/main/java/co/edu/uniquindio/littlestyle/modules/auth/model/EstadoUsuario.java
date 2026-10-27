package co.edu.uniquindio.littlestyle.modules.auth.model;

/**
 * Estado de una cuenta. Solo las cuentas {@code ACTIVO} pueden iniciar sesión y usar sus tokens;
 * {@code SUSPENDIDO} es la deshabilitación temporal decidida por el administrador;
 * {@code BLOQUEADO} es la deshabilitación por seguridad o incumplimiento grave.
 * {@code ELIMINADO} corresponde a cuentas anonimizadas por su titular, sobre las que el administrador no opera.
 */
public enum EstadoUsuario {
    ACTIVO,
    SUSPENDIDO,
    BLOQUEADO,
    ELIMINADO
}
