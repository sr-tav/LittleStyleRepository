package co.edu.uniquindio.littlestyle.modules.dashboard.controller;

import co.edu.uniquindio.littlestyle.config.security.AlcanceDatos;
import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardOverviewResponse;
import co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardSerieResponse;
import co.edu.uniquindio.littlestyle.modules.dashboard.service.DashboardAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller REST del dashboard analítico de operación. El alcance sale del token: el administrador
 * ve toda la plataforma y el vendedor solo su tienda.
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
            @RequestParam(required = false) CategoriaPrenda categoria) {
        return dashboardAnalyticsService.obtenerResumen(startDate, endDate, categoria, AlcanceDatos.vendedorId(usuario));
    }

    @GetMapping("/top-viewed-products")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VENDEDOR')")
    public List<DashboardSerieResponse> obtenerProductosMasConsultados(
            @AuthenticationPrincipal AuthenticatedUser usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) CategoriaPrenda categoria) {
        return dashboardAnalyticsService.obtenerProductosMasConsultados(
                startDate, endDate, categoria, AlcanceDatos.vendedorId(usuario));
    }

    @GetMapping("/top-sold-products")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VENDEDOR')")
    public List<DashboardSerieResponse> obtenerProductosMasVendidos(
            @AuthenticationPrincipal AuthenticatedUser usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) CategoriaPrenda categoria) {
        return dashboardAnalyticsService.obtenerProductosMasVendidos(
                startDate, endDate, categoria, AlcanceDatos.vendedorId(usuario));
    }
}
