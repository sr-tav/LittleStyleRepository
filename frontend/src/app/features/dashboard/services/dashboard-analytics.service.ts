import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';

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
  margenBruto: number;
  stockTotal: number;
  stockBajo: number;
  ventasPorCategoria: DashboardSerie[];
  demandaPorTalla: DashboardSerie[];
  productosPopulares: DashboardSerie[];
  busquedasMasFrecuentes: DashboardSerie[];
};

@Injectable({ providedIn: 'root' })
export class DashboardAnalyticsService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/v1/dashboard`;

  consultar(startDate?: string, endDate?: string, categoryId?: number): Observable<DashboardOverview> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    if (categoryId != null) params = params.set('categoryId', categoryId.toString());

    return this.http.get<DashboardOverview>(`${this.base}/overview`, { params });
  }

  consultarTopMasConsultados(startDate?: string, endDate?: string, categoryId?: number): Observable<DashboardSerie[]> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    if (categoryId != null) params = params.set('categoryId', categoryId.toString());

    return this.http.get<DashboardSerie[]>(`${this.base}/top-viewed-products`, { params });
  }

  consultarTopMasVendidos(startDate?: string, endDate?: string, categoryId?: number): Observable<DashboardSerie[]> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    if (categoryId != null) params = params.set('categoryId', categoryId.toString());

    return this.http.get<DashboardSerie[]>(`${this.base}/top-sold-products`, { params });
  }
}
