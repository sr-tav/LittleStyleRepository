package co.edu.uniquindio.littlestyle.modules.dashboard.controller;

import co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardOverviewResponse;
import co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardSerieResponse;
import co.edu.uniquindio.littlestyle.modules.dashboard.service.DashboardAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Controller REST del dashboard analítico de operación. */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardAnalyticsService dashboardAnalyticsService;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VENDEDOR')")
    public DashboardOverviewResponse obtenerOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long categoryId) {
        return dashboardAnalyticsService.obtenerResumen(startDate, endDate, categoryId, null);
    }

    @GetMapping("/top-viewed-products")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VENDEDOR')")
    public List<DashboardSerieResponse> obtenerProductosMasConsultados(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long categoryId) {
        return dashboardAnalyticsService.obtenerProductosMasConsultados(startDate, endDate, categoryId, null);
    }

    @GetMapping("/top-sold-products")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VENDEDOR')")
    public List<DashboardSerieResponse> obtenerProductosMasVendidos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long categoryId) {
        return dashboardAnalyticsService.obtenerProductosMasVendidos(startDate, endDate, categoryId, null);
    }
}
