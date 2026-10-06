import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../../../core/services/auth.service';
import { Navbar } from '../../../../shared/components/navbar/navbar';
import { NivelStockBadge } from '../../../catalogoCompras/components/nivel-stock-badge';
import { AlertaInventario, InventarioItem } from '../../../catalogoCompras/models/prenda.models';
import { InventarioService } from '../../../catalogoCompras/services/inventario.service';

/**
 * UI-18: Panel del vendedor. Productos e inventario son reales (US-08); ventas, pedidos y reportes
 * se activan con sus historias.
 */
@Component({
  selector: 'app-vendedor-panel',
  imports: [Navbar, RouterLink, NivelStockBadge],
  template: `
    <app-navbar />
    <main class="home">
      <h1>Panel del vendedor</h1>
      <p class="home-sub">{{ auth.usuario()?.nombreTienda }}</p>

      <section class="kpi-grid" aria-label="Resumen del inventario">
        <a class="kpi-card" routerLink="/vendedor/productos">
          <span class="kpi-icon kpi-icon-products" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M16 3 20 8l-2 2v11H6V10L4 8l4-5 4 3 4-3Z" /></svg>
          </span>
          <span class="kpi-label">Productos activos</span>
          <strong class="kpi-value">{{ cargando() ? '—' : productosActivos() }}</strong>
          <span class="kpi-hint">{{ cargando() ? '' : inventario().length + ' en total' }}</span>
        </a>
        <a class="kpi-card" routerLink="/vendedor/inventario/alertas">
          <span class="kpi-icon kpi-icon-alert" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 3 2 20h20L12 3Zm0 6v5m0 3v.01" /></svg>
          </span>
          <span class="kpi-label">Stock bajo</span>
          <strong class="kpi-value">{{ cargando() ? '—' : alertas().length }}</strong>
          <span class="kpi-hint">Requieren atención</span>
        </a>
      </section>

      <div class="home-grid">
        <a class="home-tile" routerLink="/vendedor/productos">
          <span aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M16 3 20 8l-2 2v11H6V10L4 8l4-5 4 3 4-3Z" /><path d="M8 3c0 2 1.5 3 4 3s4-1 4-3" /></svg></span>
          <h2>Mis productos</h2><p>Gestiona tu catálogo</p>
        </a>
        <a class="home-tile" routerLink="/vendedor/inventario">
          <span aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M3 7 12 3l9 4-9 4-9-4ZM3 7v10l9 4 9-4V7M12 11v10" /></svg></span>
          <h2>Inventario</h2><p>Stock y alertas</p>
        </a>
        <article class="home-tile home-tile-disabled">
          <span aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M5 3h14v18H5zM8 7h8M8 11h8M8 15h5" /></svg></span>
          <h2>Pedidos</h2><p>Próximamente</p>
        </article>
        <article class="home-tile home-tile-disabled">
          <span aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 20V4M4 20h17M7 16l4-5 3 2 6-8M16 5h4v4" /></svg></span>
          <h2>Reportes</h2><p>Próximamente</p>
        </article>
      </div>

      @if (alertas().length) {
        <section class="panel-alerts" aria-labelledby="panel-alerts-title">
          <header>
            <h2 id="panel-alerts-title">Alertas de inventario</h2>
            <a routerLink="/vendedor/inventario/alertas">Ver todas →</a>
          </header>
          <ul>
            @for (alerta of alertas().slice(0, 3); track alerta.id) {
              <li>
                <span>{{ alerta.nombre }}</span>
                <span class="panel-alert-stock">{{ alerta.stockActual }} / mín. {{ alerta.stockMinimo }}</span>
                <app-nivel-stock-badge [nivel]="alerta.nivel" />
              </li>
            }
          </ul>
        </section>
      }
    </main>
  `,
})
export class VendedorPanel implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly inventarioService = inject(InventarioService);

  protected readonly inventario = signal<InventarioItem[]>([]);
  protected readonly alertas = signal<AlertaInventario[]>([]);
  protected readonly cargando = signal(true);
  protected readonly productosActivos = computed(
    () => this.inventario().filter((i) => i.estado === 'ACTIVA').length,
  );

  ngOnInit(): void {
    this.inventarioService.listar().subscribe({
      next: (items) => {
        this.inventario.set(items);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
    this.inventarioService.alertas().subscribe({
      next: (alertas) => this.alertas.set(alertas),
      error: () => this.alertas.set([]),
    });
  }
}
