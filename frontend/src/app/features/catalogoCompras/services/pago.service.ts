import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, MonoTypeOperatorFunction, TimeoutError, catchError, throwError, timeout } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { InicioPago, ResultadoPago } from '../models/pago.models';

/**
 * Tiempo máximo de espera de una petición de pago. Es mayor que los límites del backend con Wompi
 * (3 s de conexión + 8 s de lectura), de modo que normalmente llega primero su mensaje.
 */
export const TIEMPO_MAXIMO_PAGO_MS = 15000;
export const MENSAJE_SIN_RESPUESTA = 'El pago está tardando en responder. Intenta más tarde; tu pedido se conserva.';

@Injectable({ providedIn: 'root' })
export class PagoService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/cliente/pagos`;

  iniciar(pedidoId: number): Observable<InicioPago> {
    return this.http.post<InicioPago>(`${this.baseUrl}/pedidos/${pedidoId}`, {}).pipe(conTiempoMaximo());
  }

  verificar(transaccionId: string): Observable<ResultadoPago> {
    return this.http.post<ResultadoPago>(`${this.baseUrl}/verificar`, { transaccionId }).pipe(conTiempoMaximo());
  }
}

/** Corta la petición si no responde a tiempo y la convierte en un error 504 con mensaje para el cliente. */
function conTiempoMaximo<T>(): MonoTypeOperatorFunction<T> {
  return (origen) => origen.pipe(
    timeout(TIEMPO_MAXIMO_PAGO_MS),
    catchError((error: unknown) => throwError(() => error instanceof TimeoutError
      ? new HttpErrorResponse({ status: 504, error: { mensaje: MENSAJE_SIN_RESPUESTA } })
      : error)),
  );
}
