import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, MonoTypeOperatorFunction, TimeoutError, catchError, throwError, timeout } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { SolicitudPago, InicioPago, ResultadoPago } from '../models/pago.models';

/**
 * Tiempo máximo de espera de una petición de pago. Es mayor que los límites del backend con Wompi
 * (3 s de conexión + 8 s de lectura), de modo que normalmente llega primero su mensaje.
 */
export const TIEMPO_MAXIMO_PAGO_MS = 15000;
export const MENSAJE_SIN_RESPUESTA = 'El pago está tardando en responder. Intenta más tarde; tu carrito se conserva.';

/** Inicia, verifica y cancela las operaciones de pago del cliente. */
@Injectable({ providedIn: 'root' })
export class PagoService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/cliente/pagos`;

  /**
   * Inicia un pago enviando la clave de idempotencia y aplica el límite de espera configurado.
   * @param solicitud Datos de pedido usados para iniciar el pago.
   * @param claveIdempotencia Clave que identifica el intento de forma idempotente.
   * @returns Flujo con los datos requeridos para abrir el widget.
   */
  iniciar(solicitud: SolicitudPago, claveIdempotencia: string): Observable<InicioPago> {
    return this.http.post<InicioPago>(`${this.baseUrl}/checkout`, solicitud, {
      headers: { 'Idempotency-Key': claveIdempotencia },
    }).pipe(conTiempoMaximo());
  }

  /**
   * Solicita la verificación de una transacción.
   * @param transaccionId Identificador de la transacción que se consulta.
   * @returns Flujo con el resultado del pago.
   */
  verificar(transaccionId: string): Observable<ResultadoPago> {
    return this.http.post<ResultadoPago>(`${this.baseUrl}/verificar`, { transaccionId }).pipe(conTiempoMaximo());
  }

  /**
   * Anula un pago aún en CREADA: libera la compra para poder iniciar otra.
   * @param referencia Referencia del pago que se anula.
   * @returns Flujo con el resultado actualizado del pago.
   */
  cancelar(referencia: string): Observable<ResultadoPago> {
    return this.http.delete<ResultadoPago>(`${this.baseUrl}/${referencia}`).pipe(conTiempoMaximo());
  }
}

/**
 * Corta la petición si no responde a tiempo y la convierte en un error 504 con mensaje para el cliente.
 * @returns Operador que aplica el límite de espera a cada observable de origen.
 */
function conTiempoMaximo<T>(): MonoTypeOperatorFunction<T> {
  return (origen) => origen.pipe(
    timeout(TIEMPO_MAXIMO_PAGO_MS),
    catchError((error: unknown) => throwError(() => error instanceof TimeoutError
      ? new HttpErrorResponse({ status: 504, error: { mensaje: MENSAJE_SIN_RESPUESTA } })
      : error)),
  );
}
