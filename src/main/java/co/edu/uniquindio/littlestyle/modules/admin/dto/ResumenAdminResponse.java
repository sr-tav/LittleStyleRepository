package co.edu.uniquindio.littlestyle.modules.admin.dto;

/**
 * KPIs del panel administrativo (US-14). {@code tasaResolucionPorcentaje} es el RNF-17: porcentaje de
 * incidencias cerradas en los ultimos 30 dias que se resolvieron en menos de 24 horas (meta &gt; 90%).
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
        double tasaResolucionPorcentaje) {
}
