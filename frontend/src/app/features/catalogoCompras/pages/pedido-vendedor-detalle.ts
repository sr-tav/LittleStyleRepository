import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Navbar } from '../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../auth/auth.validators';
import { ETIQUETAS_ESTADO } from '../../pedidos/models/pedido.models';
import { formatearPrecio } from '../models/prenda-opciones';
import { DespachoVendedor } from '../models/pedido-vendedor.models';
import { PedidosVendedorService } from '../services/pedidos-vendedor.service';

/**
 * Detalle del despacho del vendedor: su paquete, su estado y sus productos.
 * @see US-12
 */
@Component({
  selector: 'app-pedido-vendedor-detalle',
  imports: [Navbar, RouterLink],
  templateUrl: './pedido-vendedor-detalle.html',
})
export class PedidoVendedorDetallePage implements OnInit {
  private readonly ruta = inject(ActivatedRoute);
  private readonly pedidos = inject(PedidosVendedorService);

  protected readonly despacho = signal<DespachoVendedor | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly guia = signal('');
  protected readonly avanzando = signal(false);
  protected readonly errorAvance = signal<string | null>(null);

  protected readonly estadoDe = (estado: DespachoVendedor['estado']) => ETIQUETAS_ESTADO[estado];
  protected readonly precio = (valor: number) => formatearPrecio(valor);
  protected readonly totalPropio = () =>
    (this.despacho()?.items ?? []).reduce((total, item) => total + item.subtotal, 0);
  protected readonly fechaActualizacion = () => {
    const fecha = this.despacho()?.fechaActualizacion ?? null;
    return fecha === null
      ? '—'
      : new Date(fecha).toLocaleDateString('es-CO', { day: 'numeric', month: 'short', year: 'numeric' });
  };

  /**
   * Carga el despacho identificado en los parámetros de la ruta.
   * @returns No devuelve un valor.
   * @see US-12
   */
  ngOnInit(): void {
    this.cargar();
  }

  /**
   * Consulta el despacho actual y actualiza los estados de carga y error.
   * @returns No devuelve un valor.
   * @see US-12
   */
  protected cargar(): void {
    const id = Number(this.ruta.snapshot.paramMap.get('id'));
    this.cargando.set(true);
    this.error.set(null);
    this.pedidos.detalle(id).subscribe({
      next: (p) => {
        this.despacho.set(p);
        this.cargando.set(false);
      },
      error: (err: unknown) => {
        this.error.set(procesarErrorApi(err));
        this.cargando.set(false);
      },
    });
  }

  /**
   * Comprueba si el despacho está en estado registrado y puede pasar a preparación.
   * @returns `true` cuando el estado actual es `REGISTRADO`.
   */
  protected puedePreparar(): boolean {
    return this.despacho()?.estado === 'REGISTRADO';
  }

  /**
   * Comprueba si el despacho está en preparación y puede marcarse como enviado.
   * @returns `true` cuando el estado actual es `EN_PREPARACION`.
   */
  protected puedeEnviar(): boolean {
    return this.despacho()?.estado === 'EN_PREPARACION';
  }

  /**
   * Avanza el estado del despacho; exige número de guía al marcarlo como enviado.
   * @param nuevo Estado siguiente permitido para el despacho.
   * @returns No devuelve un valor.
   * @see US-12
   */
  protected avanzar(nuevo: 'EN_PREPARACION' | 'ENVIADO'): void {
    const p = this.despacho();
    if (!p || this.avanzando()) return;
    const numeroGuia = nuevo === 'ENVIADO' ? this.guia().trim() : null;
    if (nuevo === 'ENVIADO' && !numeroGuia) {
      this.errorAvance.set('Indica el número de guía para enviar tu paquete.');
      return;
    }
    this.avanzando.set(true);
    this.errorAvance.set(null);
    this.pedidos.avanzarEstado(p.despachoId, nuevo, numeroGuia).subscribe({
      next: (actualizado) => {
        this.despacho.set(actualizado);
        this.guia.set('');
        this.avanzando.set(false);
      },
      error: (err: unknown) => {
        this.avanzando.set(false);
        this.errorAvance.set(procesarErrorApi(err));
      },
    });
  }
}
