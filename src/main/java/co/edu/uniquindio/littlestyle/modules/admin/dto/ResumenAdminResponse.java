package co.edu.uniquindio.littlestyle.modules.admin.dto;

/**
 * KPIs del panel administrativo (US-14). {@code tasaResolucionPorcentaje} es el RNF-17: porcentaje de
 * incidencias cerradas en los ultimos 30 dias que se resolvieron en menos de 24 horas (meta &gt; 90%).
 * Es {@code null} cuando no hubo incidencias cerradas en la ventana: sin evidencia no se reporta 0 %.
 */
public record ResumenAdminResponse(
        long usuariosActivos,
        long usuariosSuspendidos,
        long usuariosBloqueados,
        long usuariosEliminados,
        long prendasSinRevisar,
        long prendasPublicadas,
        long prendasRechazadas,
        long incidenciasAbiertas,
        long incidenciasEnRevision,
        Double tasaResolucionPorcentaje) {
}
