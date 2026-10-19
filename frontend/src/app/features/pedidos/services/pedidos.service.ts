import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PedidoDetalle, PedidoResumen } from '../models/pedido.models';

/**
 * Seguimiento de pedidos del cliente (US-12, RNF-13).
 * @see US-12
 * @see RNF-13
 */
@Injectable({ providedIn: 'root' })
export class PedidosService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/cliente/checkout/pedidos`;

  /**
   * Consulta el historial de pedidos del cliente.
   * @returns Flujo con los resúmenes de pedido.
   * @see UI-17
   */
  listar(): Observable<PedidoResumen[]> {
    return this.http.get<PedidoResumen[]>(this.base);
  }

  /**
   * Consulta el detalle de un pedido.
   * @param id Identificador del pedido.
   * @returns Flujo con el pedido, despachos e historial.
   * @see UI-16
   */
  detalle(id: number): Observable<PedidoDetalle> {
    return this.http.get<PedidoDetalle>(`${this.base}/${id}`);
  }

  /**
   * Solicita el retracto del cliente antes del pago.
   * @param id Identificador del pedido que se cancela.
   * @returns Flujo con el estado actualizado y el número de guía, si existe.
   */
  cancelar(id: number): Observable<{ id: number; estado: string; numeroGuia: string | null }> {
    return this.http.post<{ id: number; estado: string; numeroGuia: string | null }>(
      `${this.base}/${id}/cancelar`,
      {},
    );
  }
}
