import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { EMPTY, catchError, switchMap, take, takeWhile, throwError, timer } from 'rxjs';

import { Navbar } from '../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../auth/auth.validators';
import { ResultadoPago } from '../models/pago.models';
import { formatearPrecio } from '../models/prenda-opciones';
import { CarritoService } from '../services/carrito.service';
import { PagoService } from '../services/pago.service';

/** Estados de Wompi que aún pueden cambiar; mientras duren se vuelve a consultar. */
const ESTADOS_EN_PROCESO = new Set(['CREADA', 'PENDIENTE']);
export const INTERVALO_CONSULTA_MS = 4000;
export const MAX_CONSULTAS = 15;

/**
 * RNF-28: sin conexión o con Wompi lento el backend responde 502/503/504. Esas fallas son momentáneas,
 * así que no detienen la verificación: se omite esa consulta y se reintenta en la siguiente.
 */
const ESTADOS_TRANSITORIOS = new Set([0, 502, 503, 504]);
const esTransitorio = (error: unknown) =>
  error instanceof HttpErrorResponse && ESTADOS_TRANSITORIOS.has(error.status);

/** UI-15: confirmación del pago tras volver del Widget o de la redirección de Wompi. */
@Component({
  selector: 'app-pago-resultado',
  imports: [Navbar, RouterLink],
  templateUrl: './pago-resultado.html',
})
export class PagoResultadoPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly pagoService = inject(PagoService);
  private readonly carrito = inject(CarritoService);

  protected readonly resultado = signal<ResultadoPago | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly precio = formatearPrecio;

  protected readonly vista = computed(() => {
    const resultado = this.resultado();
    if (!resultado) return null;
    switch (resultado.estadoPago) {
      case 'APROBADA':
        return 'aprobado';
      case 'REEMBOLSO_PENDIENTE':
        return resultado.motivoReembolso === 'STOCK_INSUFICIENTE' ? 'sin-stock' : 'reembolso';
      case 'RECHAZADA':
      case 'ANULADA':
      case 'ERROR':
        return 'rechazado';
      default:
        return 'pendiente';
    }
  });

  ngOnInit(): void {
    const transaccionId = this.route.snapshot.queryParamMap.get('id');
    if (!transaccionId) {
      this.error.set('No encontramos la transacción de pago. Revisa el estado de tu pedido en tu cuenta.');
      this.cargando.set(false);
      return;
    }

    let ultimoErrorTransitorio: unknown = null;
    timer(0, INTERVALO_CONSULTA_MS).pipe(
      take(MAX_CONSULTAS),
      switchMap(() => this.pagoService.verificar(transaccionId).pipe(
        catchError((error: unknown) => {
          if (!esTransitorio(error)) return throwError(() => error);
          ultimoErrorTransitorio = error;
          return EMPTY;
        }),
      )),
      takeWhile((resultado) => ESTADOS_EN_PROCESO.has(resultado.estadoPago), true),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: (resultado) => {
        this.resultado.set(resultado);
        this.cargando.set(false);
        if (resultado.estadoPago === 'APROBADA') {
          this.carrito.cargar().subscribe({
            error: (error: unknown) => this.error.set(procesarErrorApi(error)),
          });
        }
      },
      error: (error: unknown) => {
        this.error.set(procesarErrorApi(error));
        this.cargando.set(false);
      },
      complete: () => {
        // Se agotaron las consultas sin que ninguna respondiera.
        if (!this.resultado() && ultimoErrorTransitorio) {
          this.error.set(procesarErrorApi(ultimoErrorTransitorio));
          this.cargando.set(false);
        }
      },
    });
  }

}
