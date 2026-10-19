import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { DespachoVendedor } from '../models/pedido-vendedor.models';

/**
 * Paquetes del vendedor autenticado (US-12 multi-vendedor): solo su despacho.
 * @see US-12
 */
@Injectable({ providedIn: 'root' })
export class PedidosVendedorService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/vendedor/pedidos`;

  /**
   * Consulta los despachos asociados al vendedor autenticado.
   * @returns Flujo con los despachos del vendedor.
   * @see US-12
   */
  listar(): Observable<DespachoVendedor[]> {
    return this.http.get<DespachoVendedor[]>(this.baseUrl);
  }

  /**
   * Consulta el detalle de un despacho.
   * @param despachoId Identificador del despacho.
   * @returns Flujo con el despacho solicitado.
   * @see US-12
   */
  detalle(despachoId: number): Observable<DespachoVendedor> {
    return this.http.get<DespachoVendedor>(`${this.baseUrl}/${despachoId}`);
  }

  /**
   * El vendedor avanza su despacho a preparación o enviado (con guía).
   * @param despachoId Identificador del despacho que se actualiza.
   * @param nuevo Nuevo estado solicitado.
   * @param numeroGuia Número de guía opcional asociado al envío.
   * @returns Flujo con el despacho actualizado.
   * @see US-12
   */
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
