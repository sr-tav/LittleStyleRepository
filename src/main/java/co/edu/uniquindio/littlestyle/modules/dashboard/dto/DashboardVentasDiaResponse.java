package co.edu.uniquindio.littlestyle.modules.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Punto de la serie diaria de ventas del dashboard y de los reportes (US-16). */
public record DashboardVentasDiaResponse(LocalDate fecha, long pedidos, BigDecimal total) {
}
