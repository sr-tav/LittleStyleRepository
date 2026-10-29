import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';

export type DashboardSerie = {
  label: string;
  value: number;
  cantidad: number;
};

/** Detalle prenda + talla de la demanda: qué modelos y tallas se piden más. */
export type DashboardDemandaDetalle = {
  prenda: string;
  talla: string;
  unidades: number;
  valor: number;
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
  demandaDetalle: DashboardDemandaDetalle[];
};

/** Categorías reales del catálogo (enum {@code CategoriaPrenda} del backend). */
export const CATEGORIAS_DASHBOARD: { valor: string; etiqueta: string }[] = [
  { valor: 'VESTIDOS', etiqueta: 'Vestidos' },
  { valor: 'PANTALONES', etiqueta: 'Pantalones' },
  { valor: 'SHORTS', etiqueta: 'Shorts' },
  { valor: 'FALDAS', etiqueta: 'Faldas' },
  { valor: 'CAMISAS', etiqueta: 'Camisas' },
  { valor: 'CAMISETAS', etiqueta: 'Camisetas' },
  { valor: 'BUZOS', etiqueta: 'Buzos' },
  { valor: 'CHAQUETAS', etiqueta: 'Chaquetas' },
  { valor: 'CONJUNTOS', etiqueta: 'Conjuntos' },
  { valor: 'PIJAMAS', etiqueta: 'Pijamas' },
  { valor: 'ACCESORIOS', etiqueta: 'Accesorios' },
];

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
