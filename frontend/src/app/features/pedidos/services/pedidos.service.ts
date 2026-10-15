import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PedidoDetalle, PedidoResumen } from '../models/pedido.models';

/** Seguimiento de pedidos del cliente (US-12, RNF-13). */
@Injectable({ providedIn: 'root' })
export class PedidosService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/cliente/checkout/pedidos`;

  listar(): Observable<PedidoResumen[]> {
    return this.http.get<PedidoResumen[]>(this.base);
  }

  detalle(id: number): Observable<PedidoDetalle> {
    return this.http.get<PedidoDetalle>(`${this.base}/${id}`);
  }
}
