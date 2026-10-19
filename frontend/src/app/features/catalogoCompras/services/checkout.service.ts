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

/** Consulta y guarda la dirección de entrega y calcula el resumen del checkout. */
@Injectable({ providedIn: 'root' })
export class CheckoutService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/cliente/checkout`;

  /**
   * Consulta la dirección de envío guardada del cliente.
   * @returns Flujo con el estado y los datos de la dirección.
   */
  obtenerDireccion(): Observable<DireccionGuardada> {
    return this.http.get<DireccionGuardada>(`${this.baseUrl}/direccion`);
  }

  /**
   * Guarda la dirección de envío proporcionada.
   * @param direccion Datos de la dirección de entrega.
   * @returns Flujo con el estado y los datos de la dirección guardada.
   */
  guardarDireccion(direccion: DireccionEnvio): Observable<DireccionGuardada> {
    return this.http.put<DireccionGuardada>(`${this.baseUrl}/direccion`, direccion);
  }

  /**
   * Solicita el cálculo del resumen de compra para el perfil y la dirección indicados.
   * @param request Perfil infantil y dirección que se usarán en el checkout.
   * @returns Flujo con las líneas e importes calculados.
   */
  calcularResumen(request: ResumenCheckoutRequest): Observable<ResumenCheckout> {
    return this.http.post<ResumenCheckout>(`${this.baseUrl}/resumen`, request);
  }

}
