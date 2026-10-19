import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Navbar } from '../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../auth/auth.validators';
import { formatearPrecio } from '../models/prenda-opciones';
import { ETIQUETAS_ESTADO } from '../../pedidos/models/pedido.models';
import { DespachoVendedor } from '../models/pedido-vendedor.models';
import { PedidosVendedorService } from '../services/pedidos-vendedor.service';

/**
 * Paquetes del vendedor (US-12 multi-vendedor): solo su despacho por pedido.
 * @see US-12
 */
@Component({
  selector: 'app-pedidos-vendedor',
  imports: [Navbar, RouterLink],
  templateUrl: './pedidos-vendedor.html',
})
export class PedidosVendedor implements OnInit {
  private readonly pedidos = inject(PedidosVendedorService);

  protected readonly items = signal<DespachoVendedor[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly estado = (p: DespachoVendedor) => ETIQUETAS_ESTADO[p.estado];
  protected readonly precio = (p: DespachoVendedor) =>
    formatearPrecio(p.items.reduce((t, i) => t + i.subtotal, 0));
  protected readonly fecha = (p: DespachoVendedor) =>
    new Date(p.fechaActualizacion).toLocaleDateString('es-CO', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
  protected readonly unidades = (p: DespachoVendedor) =>
    p.items.reduce((t, i) => t + i.cantidad, 0);

  /**
   * Carga los despachos cuando se inicializa la vista.
   * @returns No devuelve un valor.
   * @see US-12
   */
  ngOnInit(): void {
    this.cargar();
  }

  /**
   * Consulta los despachos del vendedor y actualiza la vista.
   * @returns No devuelve un valor.
   * @see US-12
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
