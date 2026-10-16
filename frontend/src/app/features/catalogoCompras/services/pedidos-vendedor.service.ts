import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { DespachoVendedor } from '../models/pedido-vendedor.models';

/** Paquetes del vendedor autenticado (US-12 multi-vendedor): solo su despacho. */
@Injectable({ providedIn: 'root' })
export class PedidosVendedorService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/vendedor/pedidos`;

  listar(): Observable<DespachoVendedor[]> {
    return this.http.get<DespachoVendedor[]>(this.baseUrl);
  }

  detalle(despachoId: number): Observable<DespachoVendedor> {
    return this.http.get<DespachoVendedor>(`${this.baseUrl}/${despachoId}`);
  }

  /** El vendedor avanza su despacho a preparación o enviado (con guía). */
  avanzarEstado(
    despachoId: number,
    nuevo: string,
    numeroGuia?: string | null,
  ): Observable<DespachoVendedor> {
    return this.http.put<DespachoVendedor>(`${this.baseUrl}/${despachoId}/estado`, {
      nuevo,
      numeroGuia: numeroGuia ?? null,
    });
  }
}
