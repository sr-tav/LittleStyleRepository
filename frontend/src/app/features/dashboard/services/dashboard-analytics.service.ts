import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';

/**
 * Serie de un gráfico. `value` es el monto en COP en las series de ventas y `cantidad` son las unidades,
 * vistas o búsquedas según la serie (en las de conteo ambos coinciden).
 */
export type DashboardSerie = {
  label: string;
  value: number;
  cantidad: number;
};

export type DashboardOverview = {
  totalVentas: number;
  totalPedidos: number;
  ticketPromedio: number;
  productosVendidos: number;
  /** `null` mientras las prendas no registren su costo: sin costo no hay margen que calcular. */
  margenBruto: number | null;
  stockTotal: number;
  stockBajo: number;
  /** El `label` es el código de la categoría (p. ej. `VESTIDOS`), no su etiqueta en español. */
  ventasPorCategoria: DashboardSerie[];
  demandaPorTalla: DashboardSerie[];
  productosPopulares: DashboardSerie[];
  productosMasConsultados: DashboardSerie[];
  busquedasMasFrecuentes: DashboardSerie[];
};

/**
 * Cliente del dashboard analítico. El alcance (vendedor propio o plataforma completa) lo decide el backend
 * con el token; el cliente nunca envía un vendedor. `categoria` es el código del enum (`VESTIDOS`, ...).
 */
@Injectable({ providedIn: 'root' })
export class DashboardAnalyticsService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/v1/dashboard`;

  consultar(startDate?: string, endDate?: string, categoria?: string): Observable<DashboardOverview> {
    return this.http.get<DashboardOverview>(`${this.base}/overview`, {
      params: this.params(startDate, endDate, categoria),
    });
  }

  consultarTopMasConsultados(
    startDate?: string,
    endDate?: string,
    categoria?: string,
  ): Observable<DashboardSerie[]> {
    return this.http.get<DashboardSerie[]>(`${this.base}/top-viewed-products`, {
      params: this.params(startDate, endDate, categoria),
    });
  }

  consultarTopMasVendidos(
    startDate?: string,
    endDate?: string,
    categoria?: string,
  ): Observable<DashboardSerie[]> {
    return this.http.get<DashboardSerie[]>(`${this.base}/top-sold-products`, {
      params: this.params(startDate, endDate, categoria),
    });
  }

  private params(startDate?: string, endDate?: string, categoria?: string): HttpParams {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    if (categoria) params = params.set('categoria', categoria);
    return params;
  }
}
