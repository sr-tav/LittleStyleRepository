import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Navbar } from '../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../auth/auth.validators';
import { ETIQUETAS_ESTADO } from '../../pedidos/models/pedido.models';
import { formatearPrecio } from '../models/prenda-opciones';
import { PedidoVendedorDetalle } from '../models/pedido-vendedor.models';
import { PedidosVendedorService } from '../services/pedidos-vendedor.service';

/** Detalle del pedido para el vendedor: sus líneas, dirección e historial. */
@Component({
  selector: 'app-pedido-vendedor-detalle',
  imports: [Navbar, RouterLink],
  templateUrl: './pedido-vendedor-detalle.html',
})
export class PedidoVendedorDetallePage implements OnInit {
  private readonly ruta = inject(ActivatedRoute);
  private readonly pedidos = inject(PedidosVendedorService);

  protected readonly pedido = signal<PedidoVendedorDetalle | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly estadoDe = (estado: PedidoVendedorDetalle['estado']) => ETIQUETAS_ESTADO[estado];
  protected readonly precio = (valor: number) => formatearPrecio(valor);

  ngOnInit(): void {
    const id = Number(this.ruta.snapshot.paramMap.get('id'));
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
