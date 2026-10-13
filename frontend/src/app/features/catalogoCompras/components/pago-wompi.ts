import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, signal } from '@angular/core';
import { Router } from '@angular/router';
import { switchMap } from 'rxjs';

import { procesarErrorApi } from '../../auth/auth.validators';
import { PagoService } from '../services/pago.service';
import { WompiWidgetService } from '../services/wompi-widget.service';

/** Botón que inicia el pago de un pedido pendiente con el Widget de Wompi (UI-13/UI-14). */
@Component({
  selector: 'app-pago-wompi',
  template: `
    <div class="pago-wompi">
      <button
        type="button" class="btn-primary"
        [disabled]="procesando()"
        [attr.aria-label]="etiqueta() + ' pedido ' + pedidoId()"
        (click)="pagar()"
      >
        @if (procesando()) {
          <span class="spinner" aria-hidden="true"></span> Abriendo pasarela…
        } @else {
          {{ etiqueta() }}
        }
      </button>
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

  readonly pedidoId = input.required<number>();
  readonly etiqueta = input('Pagar');

  protected readonly procesando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected pagar(): void {
    if (this.procesando()) return;
    this.procesando.set(true);
    this.error.set(null);
    this.pagoService.iniciar(this.pedidoId()).pipe(
      // Si el cliente cierra el widget sin pagar, Wompi no invoca el callback; por eso el botón
      // se libera al abrirse la pasarela y no al terminar.
      switchMap((inicio) => this.widget.abrir(inicio, () => this.procesando.set(false))),
    ).subscribe({
      next: (transaccionId) => {
        void this.router.navigate(['/cliente/pago/resultado'], { queryParams: { id: transaccionId } });
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
