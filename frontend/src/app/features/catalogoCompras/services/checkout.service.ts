import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  DireccionEnvio,
  DireccionGuardada,
  ResumenCheckout,
  ResumenCheckoutRequest,
} from '../models/checkout.models';

@Injectable({ providedIn: 'root' })
export class CheckoutService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/cliente/checkout`;

  obtenerDireccion(): Observable<DireccionGuardada> {
    return this.http.get<DireccionGuardada>(`${this.baseUrl}/direccion`);
  }

  guardarDireccion(direccion: DireccionEnvio): Observable<DireccionGuardada> {
    return this.http.put<DireccionGuardada>(`${this.baseUrl}/direccion`, direccion);
  }

  calcularResumen(request: ResumenCheckoutRequest): Observable<ResumenCheckout> {
    return this.http.post<ResumenCheckout>(`${this.baseUrl}/resumen`, request);
  }

}
