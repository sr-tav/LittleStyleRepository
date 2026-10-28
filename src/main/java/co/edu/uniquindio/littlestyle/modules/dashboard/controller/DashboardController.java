package co.edu.uniquindio.littlestyle.modules.dashboard.controller;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardOverviewResponse;
import co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardSerieResponse;
import co.edu.uniquindio.littlestyle.modules.dashboard.service.DashboardAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller REST del dashboard analítico de operación.
 *
 * <p>El alcance de los datos sale del token, nunca de un parámetro: un vendedor solo ve su negocio y el
 * administrador ve la plataforma completa (RNF-15).
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardAnalyticsService dashboardAnalyticsService;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VENDEDOR')")
    public DashboardOverviewResponse obtenerOverview(
            @AuthenticationPrincipal AuthenticatedUser usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String categoria) {
        return dashboardAnalyticsService.obtenerResumen(startDate, endDate, categoria, alcance(usuario));
    }

    @GetMapping("/top-viewed-products")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VENDEDOR')")
    public List<DashboardSerieResponse> obtenerProductosMasConsultados(
            @AuthenticationPrincipal AuthenticatedUser usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String categoria) {
        return dashboardAnalyticsService.obtenerProductosMasConsultados(startDate, endDate, categoria,
                alcance(usuario));
    }

    @GetMapping("/top-sold-products")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VENDEDOR')")
    public List<DashboardSerieResponse> obtenerProductosMasVendidos(
            @AuthenticationPrincipal AuthenticatedUser usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String categoria) {
        return dashboardAnalyticsService.obtenerProductosMasVendidos(startDate, endDate, categoria,
                alcance(usuario));
    }

    /**
     * Vendedor al que se limitan los datos: el propio id si es vendedor, {@code null} (global) si es
     * administrador. Cualquier otro caso se rechaza, de modo que un fallo de configuración nunca abre el
     * alcance global por accidente.
     */
    static Long alcance(AuthenticatedUser usuario) {
        if (usuario == null || usuario.rol() == null) {
            throw new AccessDeniedException("Usuario no autenticado");
        }
        return switch (usuario.rol()) {
            case VENDEDOR -> usuario.id();
            case ADMINISTRADOR -> null;
            case CLIENTE -> throw new AccessDeniedException("El dashboard no está disponible para clientes");
        };
    }
}
