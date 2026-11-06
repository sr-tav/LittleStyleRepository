import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Navbar } from '../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../auth/auth.validators';
import { NivelStockBadge } from '../components/nivel-stock-badge';
import { PrendaMiniatura } from '../components/prenda-miniatura';
import { ETIQUETAS_CATEGORIAS, OPCIONES_CATEGORIAS } from '../models/prenda-opciones';
import { ETIQUETAS_COLORES } from '../../perfilesInfantiles/models/perfil-opciones';
import { AlertaInventario, CategoriaPrenda } from '../models/prenda.models';
import { InventarioService } from '../services/inventario.service';

/**
 * UI-22: Alertas de inventario (productos que requieren reabastecimiento).
 * @see UI-22
 */
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

      @if (!cargando() && !error() && alertas().length > 0) {
        <div class="alert-filters" role="group" aria-label="Filtrar alertas">
          <button
            type="button"
            class="category-chip"
            [attr.aria-pressed]="filtroNivel() === 'AGOTADO'"
            (click)="filtroNivel.set('AGOTADO')"
          >
            Agotadas <span>{{ agotadas() }}</span>
          </button>
          <button
            type="button"
            class="category-chip"
            [attr.aria-pressed]="filtroNivel() === 'STOCK_BAJO'"
            (click)="filtroNivel.set('STOCK_BAJO')"
          >
            Stock bajo <span>{{ stockBajo() }}</span>
          </button>
          <button
            type="button"
            class="category-chip"
            [attr.aria-pressed]="filtroNivel() === 'TODAS'"
            (click)="filtroNivel.set('TODAS')"
          >
            Todas <span>{{ alertas().length }}</span>
          </button>
          <label class="inventory-color-filter-control">
            <span class="visually-hidden">Filtrar por categoría</span>
            <select
              aria-label="Filtrar por categoría"
              [value]="categoriaSeleccionada()"
              (change)="cambiarCategoria($event)"
            >
              <option value="TODAS">Todas las categorías</option>
              @for (opcion of categorias; track opcion.valor) {
                <option [value]="opcion.valor">{{ opcion.etiqueta }}</option>
              }
            </select>
          </label>
        </div>
      }

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
      } @else if (!error() && alertasFiltradas().length === 0) {
        <div class="catalog-card catalog-state">
          <p>No hay alertas para los filtros seleccionados.</p>
        </div>
      } @else {
        <div class="alert-grid">
          @for (alerta of alertasFiltradas(); track alerta.id) {
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
                <div><dt>Variante</dt><dd class="stat-actual">
                  {{ alerta.talla ? 'Talla ' + alerta.talla : '—' }}
                  @if (alerta.color) { · {{ ETIQUETAS_COLORES[alerta.color] }} }
                </dd></div>
                <div><dt>Stock actual</dt><dd class="stat-actual">{{ alerta.stockActual }}</dd></div>
                <div><dt>Stock mínimo</dt><dd>{{ alerta.stockMinimo }}</dd></div>
                <div><dt>Diferencia</dt><dd class="stat-diff">{{ alerta.diferencia }} ({{ textoDiferencia(alerta) }})</dd></div>
              </dl>
              <p class="product-meta">Emitida el {{ fechaEmision(alerta) }}</p>
              <p class="alert-card-message">
                {{
                  alerta.nivel === 'AGOTADO'
                    ? 'Sin stock para venta'
                    : 'Quedan ' + alerta.stockActual + ' unidades; tu mínimo es ' + alerta.stockMinimo
                }}
              </p>
              <a
                class="btn-primary btn-inline btn-small"
                [routerLink]="['/vendedor/inventario']"
                [queryParams]="{ prenda: alerta.prendaId }"
              >
                Actualizar stock
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
  protected readonly filtroNivel = signal<'AGOTADO' | 'STOCK_BAJO' | 'TODAS'>('TODAS');
  protected readonly categoriaSeleccionada = signal<CategoriaPrenda | 'TODAS'>('TODAS');
  protected readonly categorias = OPCIONES_CATEGORIAS;
  protected readonly agotadas = computed(
    () => this.alertas().filter((alerta) => alerta.nivel === 'AGOTADO').length,
  );
  protected readonly stockBajo = computed(
    () => this.alertas().filter((alerta) => alerta.nivel === 'STOCK_BAJO').length,
  );
  protected readonly alertasOrdenadas = computed(() =>
    [...this.alertas()].sort((a, b) => {
      const prioridad = (a.nivel === 'AGOTADO' ? 0 : 1) - (b.nivel === 'AGOTADO' ? 0 : 1);
      return prioridad || a.diferencia - b.diferencia;
    }),
  );
  protected readonly alertasFiltradas = computed(() =>
    this.alertasOrdenadas().filter(
      (alerta) =>
        (this.filtroNivel() === 'TODAS' || alerta.nivel === this.filtroNivel()) &&
        (this.categoriaSeleccionada() === 'TODAS' ||
          alerta.categoria === this.categoriaSeleccionada()),
    ),
  );
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly categoria = (a: AlertaInventario) => ETIQUETAS_CATEGORIAS[a.categoria];
  protected readonly ETIQUETAS_COLORES = ETIQUETAS_COLORES;

  /**
   * Formatea la fecha de emisión para mostrarla en la tarjeta de alerta.
   * @param a Alerta cuya fecha se presenta.
   * @returns Fecha localizada o su valor original si no puede interpretarse.
   */
  protected fechaEmision(a: AlertaInventario): string {
    const fecha = new Date(a.fechaEmision);
    return Number.isNaN(fecha.getTime())
      ? a.fechaEmision
      : fecha.toLocaleDateString('es-CO', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  /**
   * Describe la diferencia de stock respecto del mínimo configurado.
   * @param a Alerta de inventario que se presenta.
   * @returns Texto que indica si el stock está en el mínimo o cuánto falta.
   */
  protected textoDiferencia(a: AlertaInventario): string {
    if (a.diferencia === 0) return 'en el mínimo permitido';
    return `te faltan ${-a.diferencia} para el mínimo`;
  }

  protected cambiarCategoria(event: Event): void {
    const valor = (event.target as HTMLSelectElement).value;
    const categoria = OPCIONES_CATEGORIAS.find((opcion) => opcion.valor === valor);
    this.categoriaSeleccionada.set(categoria?.valor ?? 'TODAS');
  }

  ngOnInit(): void {
    this.cargar();
  }

  /**
   * Consulta de nuevo las alertas y actualiza los estados de carga y error.
   * @returns No devuelve un valor.
   * @see UI-22
   */
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
