import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Navbar } from '../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../auth/auth.validators';
import { NivelStockBadge } from '../components/nivel-stock-badge';
import { PrendaMiniatura } from '../components/prenda-miniatura';
import { ETIQUETAS_CATEGORIAS } from '../models/prenda-opciones';
import { AlertaInventario } from '../models/prenda.models';
import { InventarioService } from '../services/inventario.service';

/** UI-22: Alertas de inventario (productos que requieren reabastecimiento). */
@Component({
  selector: 'app-alertas-stock',
  imports: [Navbar, RouterLink, NivelStockBadge, PrendaMiniatura],
  template: `
    <app-navbar />
    <main class="catalog-page">
      <a class="back-link" routerLink="/vendedor/inventario">← Inventario</a>
      <header class="catalog-heading">
        <div>
          <h1>Alertas de inventario</h1>
          <p class="catalog-subtitle">Productos que requieren reabastecimiento</p>
        </div>
      </header>

      @if (error()) {
        <div class="alert alert-error" role="alert">
          {{ error() }}
          <button type="button" class="link-button" (click)="cargar()">Reintentar</button>
        </div>
      }

      @if (cargando()) {
        <div class="catalog-state" role="status"><span class="spinner spinner-dark"></span> Cargando alertas...</div>
      } @else if (!error() && alertas().length === 0) {
        <div class="catalog-card catalog-state">
          <p>Todo en orden: ningún producto está en su stock mínimo.</p>
        </div>
      } @else {
        <div class="alert-grid">
          @for (alerta of alertas(); track alerta.id) {
            <article class="alert-card" [class]="'alert-card alert-card-' + alerta.nivel">
              <div class="alert-card-top">
                <app-prenda-miniatura [url]="alerta.imagenUrl" [nombre]="alerta.nombre" />
                <div class="alert-card-title">
                  <h2>{{ alerta.nombre }}</h2>
                  <span class="product-meta">{{ categoria(alerta) }}</span>
                </div>
                <app-nivel-stock-badge [nivel]="alerta.nivel" />
              </div>
              <dl class="alert-card-stats">
                <div><dt>Actual</dt><dd class="stat-actual">{{ alerta.stockActual }}</dd></div>
                <div><dt>Mínimo</dt><dd>{{ alerta.stockMinimo }}</dd></div>
                <div><dt>Diferencia</dt><dd class="stat-diff">{{ alerta.diferencia }} ({{ textoDiferencia(alerta) }})</dd></div>
              </dl>
              <p class="product-meta">Emitida el {{ fechaEmision(alerta) }}</p>
              <p class="alert-card-message">
                {{ alerta.nivel === 'AGOTADO' ? 'Sin stock para venta' : 'Requiere reabastecimiento' }}
              </p>
              <a class="link-button" [routerLink]="['/vendedor/inventario']" [queryParams]="{ prenda: alerta.prendaId }">
                Actualizar stock →
              </a>
            </article>
          }
        </div>
      }
    </main>
  `,
})
export class AlertasStock implements OnInit {
  private readonly inventarioService = inject(InventarioService);

  protected readonly alertas = signal<AlertaInventario[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly categoria = (a: AlertaInventario) => ETIQUETAS_CATEGORIAS[a.categoria];

  protected fechaEmision(a: AlertaInventario): string {
    const fecha = new Date(a.fechaEmision);
    return Number.isNaN(fecha.getTime())
      ? a.fechaEmision
      : fecha.toLocaleDateString('es-CO', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  protected textoDiferencia(a: AlertaInventario): string {
    if (a.diferencia === 0) return 'en el mínimo permitido';
    return `te faltan ${-a.diferencia} para el mínimo`;
  }

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.inventarioService.alertas().subscribe({
      next: (alertas) => {
        this.alertas.set(alertas);
        this.cargando.set(false);
      },
      error: (err) => {
        this.error.set(procesarErrorApi(err));
        this.cargando.set(false);
      },
    });
  }
}
