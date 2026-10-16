import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Navbar } from '../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../auth/auth.validators';
import { formatearPrecio } from '../../catalogoCompras/models/prenda-opciones';
import { ETIQUETAS_ESTADO, ORDEN_ESTADOS, PedidoDetalle } from '../models/pedido.models';
import { PedidosService } from '../services/pedidos.service';

/** US-12 / UI-16: detalle del pedido con línea de tiempo y guía de envío. */
@Component({
  selector: 'app-pedido-detalle',
  imports: [Navbar, RouterLink],
  templateUrl: './pedido-detalle.html',
})
export class PedidoDetallePage implements OnInit {
  private readonly ruta = inject(ActivatedRoute);
  private readonly pedidos = inject(PedidosService);

  protected readonly pedido = signal<PedidoDetalle | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly cancelando = signal(false);

  protected readonly estadoDe = (estado: PedidoDetalle['estado']) => ETIQUETAS_ESTADO[estado];
  protected readonly precio = (valor: number) => formatearPrecio(valor);
  protected readonly pasos = ORDEN_ESTADOS;
  protected readonly fechaDe = (iso: string | null) =>
    iso === null
      ? '—'
      : new Date(iso).toLocaleString('es-CO', {
          day: 'numeric',
          month: 'short',
          year: 'numeric',
          hour: '2-digit',
          minute: '2-digit',
        });

  protected pasoAlcanzado(paso: PedidoDetalle['estado']): boolean {
    const p = this.pedido();
    if (!p || p.estado === 'CANCELADO') return false;
    return ORDEN_ESTADOS.indexOf(paso) <= ORDEN_ESTADOS.indexOf(p.estado);
  }

  ngOnInit(): void {
    this.cargar();
  }

  /** Recarga el detalle para ver el estado actual ("tiempo real" manual). */
  protected recargar(): void {
    this.cargar();
  }

  /** Retracto del cliente antes del pago. */
  protected cancelar(): void {
    const p = this.pedido();
    if (!p || p.estado !== 'PENDIENTE_PAGO' || this.cancelando()) return;
    if (!window.confirm('¿Deseas cancelar este pedido? Aún no se ha pagado.')) return;
    this.cancelando.set(true);
    this.pedidos.cancelar(p.id).subscribe({
      next: () => {
        this.cancelando.set(false);
        this.cargar();
      },
      error: (err: unknown) => {
        this.cancelando.set(false);
        this.error.set(procesarErrorApi(err));
      },
    });
  }

  private cargar(): void {
    const id = Number(this.ruta.snapshot.paramMap.get('id'));
    this.cargando.set(true);
    this.error.set(null);
    this.pedidos.detalle(id).subscribe({
      next: (p) => {
        this.pedido.set(p);
        this.cargando.set(false);
      },
      error: (err: unknown) => {
        this.error.set(procesarErrorApi(err));
        this.cargando.set(false);
      },
    });
  }
}
