import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { InicioPago, ResultadoPago } from '../models/pago.models';

@Injectable({ providedIn: 'root' })
export class PagoService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/cliente/pagos`;

  iniciar(pedidoId: number): Observable<InicioPago> {
    return this.http.post<InicioPago>(`${this.baseUrl}/pedidos/${pedidoId}`, {});
  }

  verificar(transaccionId: string): Observable<ResultadoPago> {
    return this.http.post<ResultadoPago>(`${this.baseUrl}/verificar`, { transaccionId });
  }
}
