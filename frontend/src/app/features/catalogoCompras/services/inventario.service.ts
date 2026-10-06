import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { ActualizarStockRequest, AlertaInventario, InventarioItem } from '../models/prenda.models';

@Injectable({ providedIn: 'root' })
export class InventarioService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/vendedor/inventario`;

  listar(): Observable<InventarioItem[]> {
    return this.http.get<InventarioItem[]>(this.baseUrl);
  }

  actualizarStock(prendaId: number, request: ActualizarStockRequest): Observable<InventarioItem> {
    return this.http.put<InventarioItem>(`${this.baseUrl}/${prendaId}`, request);
  }

  alertas(): Observable<AlertaInventario[]> {
    return this.http.get<AlertaInventario[]>(`${this.baseUrl}/alertas`);
  }
}
