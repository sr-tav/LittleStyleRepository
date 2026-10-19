import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Navbar } from '../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../auth/auth.validators';
import { formatearPrecio } from '../../catalogoCompras/models/prenda-opciones';
import { ETIQUETAS_ESTADO, ORDEN_ESTADOS, PedidoDetalle } from '../models/pedido.models';
import { PedidosService } from '../services/pedidos.service';

/**
 * US-12 / UI-16: detalle del pedido con línea de tiempo y guía de envío.
 * @see US-12
 * @see UI-16
 */
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

  /**
   * Determina si un estado ya se alcanzó en la línea de tiempo; los pedidos cancelados no muestran pasos.
   * @param paso Estado de la línea de tiempo que se consulta.
   * @returns `true` si el estado fue alcanzado por el pedido vigente.
   */
  protected pasoAlcanzado(paso: PedidoDetalle['estado']): boolean {
    const p = this.pedido();
    if (!p || p.estado === 'CANCELADO') return false;
    return ORDEN_ESTADOS.indexOf(paso) <= ORDEN_ESTADOS.indexOf(p.estado);
  }

  /**
   * Carga el detalle identificado en los parámetros de ruta.
   * @returns No devuelve un valor.
   */
  ngOnInit(): void {
    this.cargar();
  }

  /**
   * Recarga el detalle para ver el estado actual ("tiempo real" manual).
   * @returns No devuelve un valor.
   */
  protected recargar(): void {
    this.cargar();
  }

  /**
   * Solicita el retracto solo si el pedido está pendiente de pago y tras confirmar la acción.
   * @returns No devuelve un valor.
   * @see UI-16
   */
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

  /**
   * Consulta el pedido indicado por la ruta y actualiza la vista.
   * @returns No devuelve un valor.
   */
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
