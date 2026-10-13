import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { concatMap, from, last, switchMap, take, takeWhile, timer } from 'rxjs';

import { Navbar } from '../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../auth/auth.validators';
import { PagoWompi } from '../components/pago-wompi';
import { ResultadoPago } from '../models/pago.models';
import { formatearPrecio } from '../models/prenda-opciones';
import { CarritoService } from '../services/carrito.service';
import { PagoService } from '../services/pago.service';

/** Estados de Wompi que aún pueden cambiar; mientras duren se vuelve a consultar. */
const ESTADOS_EN_PROCESO = new Set(['CREADA', 'PENDIENTE']);
export const INTERVALO_CONSULTA_MS = 4000;
export const MAX_CONSULTAS = 15;

/** UI-15: confirmación del pago tras volver del Widget o de la redirección de Wompi. */
@Component({
  selector: 'app-pago-resultado',
  imports: [Navbar, RouterLink, PagoWompi],
  templateUrl: './pago-resultado.html',
})
export class PagoResultadoPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly pagoService = inject(PagoService);
  private readonly carrito = inject(CarritoService);

  protected readonly resultado = signal<ResultadoPago | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly recomprando = signal(false);
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

    timer(0, INTERVALO_CONSULTA_MS).pipe(
      take(MAX_CONSULTAS),
      switchMap(() => this.pagoService.verificar(transaccionId)),
      takeWhile((resultado) => ESTADOS_EN_PROCESO.has(resultado.estadoPago), true),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: (resultado) => {
        this.resultado.set(resultado);
        this.cargando.set(false);
      },
      error: (error: unknown) => {
        this.error.set(procesarErrorApi(error));
        this.cargando.set(false);
      },
    });
  }

  /** Vuelve a agregar al carrito las prendas que sí tenían stock y regresa al checkout. */
  protected continuarSinAgotadas(): void {
    const lineas = this.resultado()?.lineasDisponibles ?? [];
    if (lineas.length === 0 || this.recomprando()) return;
    this.recomprando.set(true);
    this.error.set(null);
    from(lineas).pipe(
      concatMap((linea) => this.carrito.agregar({
        prendaId: linea.prendaId,
        talla: linea.talla,
        cantidad: linea.cantidad,
      })),
      last(),
    ).subscribe({
      next: () => void this.router.navigate(['/cliente/checkout']),
      error: (error: unknown) => {
        this.error.set(procesarErrorApi(error));
        this.recomprando.set(false);
      },
    });
  }
}
