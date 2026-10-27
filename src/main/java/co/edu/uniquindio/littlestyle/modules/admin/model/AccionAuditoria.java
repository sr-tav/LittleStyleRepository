package co.edu.uniquindio.littlestyle.modules.admin.model;

/**
 * Acciones administrativas trazables: toda operación del admin sobre cuentas y contenido.
 */
public enum AccionAuditoria {
    /** Cambio de estado de cuenta: de SUSPENDIDO/BLOQUEADO a ACTIVO. */
    HABILITAR_USUARIO,
    /** Deshabilitación temporal de una cuenta por decisión del administrador. */
    SUSPENDER_USUARIO,
    /** Deshabilitación por seguridad/incumplimiento grave. */
    BLOQUEAR_USUARIO,
    /** Aprobación de una prenda */
    APROBAR_PRODUCTO,
    /** Desactivación de una prenda: deja de ser visible con motivo. */
    DESACTIVAR_PRODUCTO,
    /** Restauración de una prenda desactivada. */
    RESTAURAR_PRODUCTO,
    /** Toma la incidencia a cargo */
    TOMAR_INCIDENCIA,
    /** Se marca la incidencia RESUELTA con respuesta */
    RESOLVER_INCIDENCIA,
    /** Incidencia RECHAZADA sin atender la petición */
    RECHAZAR_INCIDENCIA,
    /** Acción administrativa que termino en error: puede reintentarse sin perder trazabilidad */
    ACCION_FALLIDA
}
