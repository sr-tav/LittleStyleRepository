import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, input, signal } from '@angular/core';
import { Router } from '@angular/router';
import { switchMap } from 'rxjs';

import { procesarErrorApi } from '../../auth/auth.validators';
import { SolicitudPago } from '../models/pago.models';
import { PagoService } from '../services/pago.service';
import { WompiWidgetService } from '../services/wompi-widget.service';

/**
 * Inicia el pago del checkout actual con el Widget de Wompi (UI-13/UI-14).
 * @see UI-13
 * @see UI-14
 */
@Component({
  selector: 'app-pago-wompi',
  template: `
    <div class="pago-wompi">
      <button
        type="button" class="btn-primary"
        [disabled]="procesando()"
        [attr.aria-label]="etiquetaBoton() + ' de la compra'"
        (click)="pagar()"
      >
        @if (procesando()) {
          <span class="spinner" aria-hidden="true"></span> Abriendo pasarela…
        } @else {
          {{ etiquetaBoton() }}
        }
      </button>
      @if (pagoNoCompletado()) {
        <p class="alert alert-warning" role="status">
          No se completó el pago. Cerraste la ventana antes de terminar; tu carrito se conserva
          y puedes intentarlo de nuevo.
        </p>
      }
      <p class="pago-wompi-nota">
        Pago seguro procesado por Wompi. LittleStyle no almacena los datos de tu tarjeta o cuenta.
      </p>
      @if (error(); as mensaje) {
        <p class="alert alert-error" role="alert">{{ mensaje }}</p>
      }
    </div>
  `,
})
export class PagoWompi {
  private readonly pagoService = inject(PagoService);
  private readonly widget = inject(WompiWidgetService);
  private readonly router = inject(Router);

  readonly solicitud = input.required<SolicitudPago>();
  readonly claveIdempotencia = input.required<string>();
  readonly etiqueta = input('Pagar ahora');

  protected readonly procesando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly pagoNoCompletado = signal(false);
  protected readonly etiquetaBoton = computed(() => this.pagoNoCompletado() ? 'Reintentar pago' : this.etiqueta());

  /**
   * Inicia el pago, abre el widget y presenta el resultado o el error correspondiente.
   * @returns No devuelve un valor.
   * @see UI-13
   * @see UI-14
   */
  protected pagar(): void {
    if (this.procesando()) return;
    this.procesando.set(true);
    this.error.set(null);
    this.pagoNoCompletado.set(false);
    this.pagoService.iniciar(this.solicitud(), this.claveIdempotencia()).pipe(
      // Si el cliente cierra el widget sin pagar, Wompi no invoca el callback; por eso el botón
      // se libera al abrirse la pasarela y no al terminar.
      switchMap((inicio) => this.widget.abrir(inicio, () => this.procesando.set(false))),
    ).subscribe({
      next: (resultado) => {
        if (resultado.tipo === 'cerrado') {
          this.pagoNoCompletado.set(true);
          this.procesando.set(false);
          return;
        }
        void this.router.navigate(['/cliente/pago/resultado'], { queryParams: { id: resultado.id } });
      },
      error: (error: unknown) => {
        this.error.set(error instanceof HttpErrorResponse || !(error instanceof Error)
          ? procesarErrorApi(error)
          : error.message);
        this.procesando.set(false);
      },
    });
  }
}
