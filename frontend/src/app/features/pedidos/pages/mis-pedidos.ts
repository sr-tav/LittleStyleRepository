import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Navbar } from '../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../auth/auth.validators';
import { formatearPrecio } from '../../catalogoCompras/models/prenda-opciones';
import { ETIQUETAS_ESTADO, PedidoResumen } from '../models/pedido.models';
import { PedidosService } from '../services/pedidos.service';

/**
 * US-12 / UI-17: historial de pedidos del cliente.
 * @see US-12
 * @see UI-17
 */
@Component({
  selector: 'app-mis-pedidos',
  imports: [Navbar, RouterLink],
  templateUrl: './mis-pedidos.html',
})
export class MisPedidos implements OnInit {
  private readonly pedidos = inject(PedidosService);

  protected readonly items = signal<PedidoResumen[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly estado = (p: PedidoResumen) => ETIQUETAS_ESTADO[p.estado];
  protected readonly precioTotal = (p: PedidoResumen) => formatearPrecio(p.total);
  protected readonly fechaDe = (iso: string) =>
    new Date(iso).toLocaleDateString('es-CO', { day: 'numeric', month: 'short', year: 'numeric' });

  /**
   * Carga el historial al inicializar la página.
   * @returns No devuelve un valor.
   */
  ngOnInit(): void {
    this.cargar();
  }

  /**
   * Consulta el historial y actualiza los estados de carga y error.
   * @returns No devuelve un valor.
   * @see UI-17
   */
  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.pedidos.listar().subscribe({
      next: (res) => {
        this.items.set(res);
        this.cargando.set(false);
      },
      error: (err: unknown) => {
        this.error.set(procesarErrorApi(err));
        this.cargando.set(false);
      },
    });
  }
}
