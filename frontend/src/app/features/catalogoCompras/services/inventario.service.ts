import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { ActualizarStockRequest, AlertaInventario, InventarioItem } from '../models/prenda.models';

/** Consulta el inventario del vendedor, actualiza existencias y obtiene sus alertas. */
@Injectable({ providedIn: 'root' })
export class InventarioService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/vendedor/inventario`;

  /**
   * Consulta los productos y existencias del inventario del vendedor.
   * @returns Flujo con las líneas de inventario.
   */
  listar(): Observable<InventarioItem[]> {
    return this.http.get<InventarioItem[]>(this.baseUrl);
  }

  /**
   * Actualiza las existencias y mínimos de alerta de una prenda.
   * @param prendaId Identificador de la prenda.
   * @param request Existencias y mínimos por talla.
   * @returns Flujo con la línea de inventario actualizada.
   */
  actualizarStock(prendaId: number, request: ActualizarStockRequest): Observable<InventarioItem> {
    return this.http.put<InventarioItem>(`${this.baseUrl}/${prendaId}`, request);
  }

  /**
   * Consulta las alertas de inventario del vendedor.
   * @returns Flujo con las alertas emitidas.
   */
  alertas(): Observable<AlertaInventario[]> {
    return this.http.get<AlertaInventario[]>(`${this.baseUrl}/alertas`);
  }
}
