import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Navbar } from '../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../auth/auth.validators';
import { ETIQUETAS_ESTADO } from '../../pedidos/models/pedido.models';
import { PedidoVendedorResumen } from '../models/pedido-vendedor.models';
import { PedidosVendedorService } from '../services/pedidos-vendedor.service';

/** Pedidos con productos del vendedor para preparar despachos. */
@Component({
  selector: 'app-pedidos-vendedor',
  imports: [Navbar, RouterLink],
  templateUrl: './pedidos-vendedor.html',
})
export class PedidosVendedor implements OnInit {
  private readonly pedidos = inject(PedidosVendedorService);

  protected readonly items = signal<PedidoVendedorResumen[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly estado = (p: PedidoVendedorResumen) => ETIQUETAS_ESTADO[p.estado];

  ngOnInit(): void {
    this.cargar();
  }

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
