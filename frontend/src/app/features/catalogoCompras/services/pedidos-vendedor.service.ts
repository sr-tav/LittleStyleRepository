import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PedidoVendedorDetalle, PedidoVendedorResumen } from '../models/pedido-vendedor.models';

/** Pedidos con líneas del vendedor autenticado, para preparar despachos. */
@Injectable({ providedIn: 'root' })
export class PedidosVendedorService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/vendedor/pedidos`;

  listar(): Observable<PedidoVendedorResumen[]> {
    return this.http.get<PedidoVendedorResumen[]>(this.baseUrl);
  }

  detalle(id: number): Observable<PedidoVendedorDetalle> {
    return this.http.get<PedidoVendedorDetalle>(`${this.baseUrl}/${id}`);
  }

  /** El vendedor avanza sus pedidos a preparación o enviado (con guía). */
  avanzarEstado(id: number, nuevo: string, numeroGuia?: string | null): Observable<PedidoVendedorDetalle> {
    return this.http.put<PedidoVendedorDetalle>(`${this.baseUrl}/${id}/estado`, {
      nuevo,
      numeroGuia: numeroGuia ?? null,
    });
  }
}
