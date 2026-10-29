import {
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import Chart from 'chart.js/auto';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, forkJoin, of, switchMap, tap } from 'rxjs';

import {
  CATEGORIAS_DASHBOARD,
  DashboardAnalyticsService,
  DashboardOverview,
  DashboardSerie,
} from '../../services/dashboard-analytics.service';
import { Navbar } from '../../../../shared/components/navbar/navbar';
import { AuthService } from '../../../../core/services/auth.service';
import { ExportarReporte } from '../../../reportes/components/exportar-reporte';

const NUMERO_CO = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 0 });
const VALOR_COP = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 0 });
const COMPACTO_CO = new Intl.NumberFormat('es-CO', { notation: 'compact', maximumFractionDigits: 1 });

/**
 * Dashboard analítico de US-15/16. Administrador y vendedor comparten la misma lectura:
 * el alcance (global o tienda) lo resuelve el backend a partir del token, y aquí solo se
 * muestran datos reales filtrados por período y categoría.
 */
@Component({
  selector: 'app-dashboard-analytics',
  standalone: true,
  imports: [CommonModule, Navbar, FormsModule, ExportarReporte],
  template: `
    <app-navbar />
    <main class="dashboard-shell">
      <header class="page-head">
        <div class="page-title">
          <span class="eyebrow">Analítica</span>
          <h1>{{ tituloVista() }}</h1>
          <p>{{ subtituloVista() }}</p>
        </div>
        <button type="button" class="btn-ghost" (click)="cargarDatosDashboard()" [disabled]="cargando()">
          Actualizar
        </button>
      </header>

      <section class="panel" aria-labelledby="filtros-titulo">
        <div class="panel-head">
          <h2 id="filtros-titulo">Filtros</h2>
          <span class="panel-hint">Los indicadores se recalculan con cada cambio</span>
        </div>

        <div class="filters-grid">
          <label class="field">
            <span>Período</span>
            <select [(ngModel)]="periodoSeleccionado" (change)="onPeriodoChange()">
              <option value="30_DIAS">Últimos 30 días</option>
              <option value="MES_ACTUAL">Este mes</option>
              <option value="6_MESES">Últimos 6 meses</option>
            </select>
          </label>

          <label class="field">
            <span>Desde</span>
            <input type="date" [(ngModel)]="fechaDesde" (change)="onFechaManualChange()" />
          </label>

          <label class="field">
            <span>Hasta</span>
            <input type="date" [(ngModel)]="fechaHasta" (change)="onFechaManualChange()" />
          </label>

          <label class="field">
            <span>Categoría</span>
            <select [ngModel]="categoriaSeleccionada" (ngModelChange)="onCategoriaChange($event)">
              <option value="TODAS">Todas las categorías</option>
              @for (cat of categorias; track cat.valor) {
                <option [value]="cat.valor">{{ cat.etiqueta }}</option>
              }
            </select>
          </label>

          <div class="field field-action">
            <span class="sr-only">&nbsp;</span>
            <button type="button" class="btn-secondary" (click)="limpiarFiltros()" [disabled]="cargando()">
              Limpiar
            </button>
          </div>
        </div>

        @if (errorFechas()) {
          <p class="inline-error" role="alert">{{ errorFechas() }}</p>
        }
      </section>

      @if (errorGraficos()) {
        <p class="inline-error" role="alert">{{ errorGraficos() }}</p>
      }

      <section class="panel" aria-labelledby="exportar-titulo">
        <div class="panel-head">
          <h2 id="exportar-titulo">Exportar reporte</h2>
          <span class="panel-hint">Se genera en PDF con los filtros activos y el alcance de tu rol</span>
        </div>
        <app-exportar-reporte
          [startDate]="startDate()"
          [endDate]="endDate()"
          [categoria]="categoriaSeleccionada === 'TODAS' ? null : categoriaSeleccionada"
        />
      </section>

      @if (cargando()) {
        <div class="loader" role="status" aria-live="polite">
          <div class="spinner" aria-hidden="true"></div>
          <span>Cargando métricas…</span>
        </div>
      } @else if (error()) {
        <div class="empty-state error-state" role="alert">
          <h2>No pudimos cargar el dashboard</h2>
          <p>{{ error() }}</p>
          <button type="button" class="btn-primary" (click)="cargarDatosDashboard()">Reintentar</button>
        </div>
      } @else {
        <section class="kpi-grid" aria-label="Indicadores del período">
          <article class="kpi-card">
            <span class="kpi-label">Ventas totales</span>
            <strong class="kpi-value">{{ formatCurrency(overviewData()?.totalVentas ?? 0) }}</strong>
            <span class="kpi-hint">COP · pedidos con pago confirmado</span>
          </article>
          <article class="kpi-card">
            <span class="kpi-label">Pedidos</span>
            <strong class="kpi-value">{{ formatNumber(overviewData()?.totalPedidos ?? 0) }}</strong>
            <span class="kpi-hint">Órdenes con venta en el período</span>
          </article>
          <article class="kpi-card">
            <span class="kpi-label">Unidades vendidas</span>
            <strong class="kpi-value">{{ formatNumber(overviewData()?.productosVendidos ?? 0) }}</strong>
            <span class="kpi-hint">{{ formatNumber(overviewData()?.stockBajo ?? 0) }} tallas con stock bajo</span>
          </article>
          <article class="kpi-card">
            <span class="kpi-label">Ticket promedio</span>
            <strong class="kpi-value">{{ formatCurrency(overviewData()?.ticketPromedio ?? 0) }}</strong>
            <span class="kpi-hint">Valor medio por pedido</span>
          </article>
        </section>

        <section class="charts-grid" aria-label="Gráficos del período">
          <article class="card">
            <header class="card-head">
              <div>
                <h2>{{ tituloVentas() }}</h2>
                <p>{{ subtituloVentas() }}</p>
              </div>
            </header>
            @if (hayVentasGrafico()) {
              <div class="chart-wrapper"><canvas #ventasCanvas></canvas></div>
            } @else {
              <p class="empty-cell">Sin ventas registradas para este filtro.</p>
            }
          </article>

          <article class="card">
            <header class="card-head">
              <div>
                <h2>Demanda por talla</h2>
                <p>Unidades vendidas por talla</p>
              </div>
            </header>
            @if (demandaTalla().length) {
              <div class="chart-wrapper"><canvas #demandaCanvas></canvas></div>
            } @else {
              <p class="empty-cell">Sin demanda registrada para este filtro.</p>
            }
          </article>
        </section>

        <section class="detail-grid">
          <article class="card table-card" aria-labelledby="detalle-titulo">
            <header class="card-head">
              <div>
                <h2 id="detalle-titulo">Prendas y tallas más pedidas</h2>
                <p>Detalle del filtro aplicado</p>
              </div>
            </header>
            <div class="table-scroll">
              <table class="data-table">
                <thead>
                  <tr>
                    <th scope="col">Prenda</th>
                    <th scope="col">Talla</th>
                    <th scope="col" class="num">Unidades</th>
                    <th scope="col" class="num">Ventas</th>
                  </tr>
                </thead>
                <tbody>
                  @for (item of demandaDetalle(); track $index) {
                    <tr>
                      <td>{{ item.prenda }}</td>
                      <td><span class="size-chip">{{ item.talla }}</span></td>
                      <td class="num">{{ formatNumber(item.unidades) }}</td>
                      <td class="num">{{ formatCurrency(item.valor) }}</td>
                    </tr>
                  } @empty {
                    <tr>
                      <td colspan="4" class="empty-cell">Sin demanda registrada para este filtro.</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </article>

          <div class="list-stack">
            <article class="card" aria-labelledby="vendidos-titulo">
              <header class="card-head">
                <div>
                  <h2 id="vendidos-titulo">Más vendidos</h2>
                  <p>Unidades e ingresos en el período filtrado</p>
                </div>
              </header>
              <ul class="product-list">
                @for (item of productosPopulares(); track $index) {
                  <li>
                    <span class="product-name">{{ item.label }}</span>
                    <span class="product-meta">{{ formatNumber(item.cantidad) }} uds · {{ formatCurrency(item.value) }}</span>
                  </li>
                } @empty {
                  <li class="empty-cell">Sin ventas registradas.</li>
                }
              </ul>
            </article>

            <article class="card" aria-labelledby="solicitados-titulo">
              <header class="card-head">
                <div>
                  <h2 id="solicitados-titulo">Más solicitados</h2>
                  <p>Productos con más pedidos</p>
                </div>
              </header>
              <ul class="product-list">
                @for (item of masConsultados(); track $index) {
                  <li>
                    <span class="product-name">{{ item.label }}</span>
                    <span class="product-meta">{{ formatNumber(item.cantidad) }} pedidos</span>
                  </li>
                } @empty {
                  <li class="empty-cell">Sin pedidos registrados.</li>
                }
              </ul>
            </article>
          </div>
        </section>
      }
    </main>
  `,
  styles: [
    `
      :host {
        display: block;
        --ls-radius: 14px;
        --ls-gap: 1.25rem;
        --ls-line: #e7e5e4;
        --ls-ink: #14532d;
        --ls-muted: #6b7280;
        --ls-brand: #15803d;
        --ls-brand-dark: #14532d;
      }

      .dashboard-shell {
        box-sizing: border-box;
        min-height: 100vh;
        max-width: 1240px;
        margin: 0 auto;
        padding: 2rem 1.5rem 3rem;
        background: #fafaf9;
        color: #1f2937;
        font-family: var(--ls-font, 'Nunito', 'Segoe UI', sans-serif);
      }

      .page-head {
        display: flex;
        flex-wrap: wrap;
        align-items: flex-end;
        justify-content: space-between;
        gap: 1rem;
        margin-bottom: var(--ls-gap);
      }

      .eyebrow {
        color: var(--ls-brand);
        font-size: 0.72rem;
        font-weight: 800;
        letter-spacing: 0.12em;
        text-transform: uppercase;
      }

      .page-title h1 {
        margin: 0.25rem 0 0.35rem;
        color: var(--ls-ink);
        font-size: clamp(1.7rem, 2.4vw, 2.3rem);
        font-weight: 800;
        letter-spacing: -0.03em;
        line-height: 1.1;
      }

      .page-title p {
        margin: 0;
        color: var(--ls-muted);
        font-size: 0.92rem;
      }

      .btn-ghost,
      .btn-secondary,
      .btn-primary {
        min-height: 42px;
        border-radius: 10px;
        padding: 0 1.1rem;
        font: inherit;
        font-size: 0.85rem;
        font-weight: 700;
        cursor: pointer;
      }

      .btn-ghost {
        border: 1px solid var(--ls-line);
        background: #fff;
        color: var(--ls-ink);
      }

      .btn-secondary {
        border: 1px solid var(--ls-brand);
        background: #fff;
        color: var(--ls-brand);
      }

      .btn-primary {
        border: 1px solid var(--ls-brand);
        background: var(--ls-brand);
        color: #fff;
      }

      button:disabled {
        cursor: not-allowed;
        opacity: 0.55;
      }

      .panel,
      .card,
      .kpi-card,
      .loader,
      .empty-state {
        background: #fff;
        border: 1px solid var(--ls-line);
        border-radius: var(--ls-radius);
        box-shadow: 0 4px 12px rgba(15, 23, 42, 0.03);
      }

      .panel {
        padding: 1.25rem 1.5rem;
        margin-bottom: var(--ls-gap);
      }

      .panel-head {
        display: flex;
        flex-wrap: wrap;
        align-items: baseline;
        justify-content: space-between;
        gap: 0.5rem;
        margin-bottom: 1rem;
      }

      .panel-head h2,
      .card-head h2 {
        margin: 0;
        color: var(--ls-ink);
        font-size: 1rem;
        font-weight: 800;
      }

      .panel-hint,
      .card-head p {
        margin: 0;
        color: var(--ls-muted);
        font-size: 0.8rem;
      }

      .filters-grid {
        display: grid;
        grid-template-columns: repeat(4, minmax(150px, 1fr)) minmax(100px, auto);
        align-items: end;
        gap: clamp(0.65rem, 1.1vw, 0.9rem);
      }

      .field {
        display: flex;
        min-width: 0;
        flex-direction: column;
        gap: 0.35rem;
      }

      .field > span {
        color: var(--ls-muted);
        font-size: 0.7rem;
        font-weight: 800;
        letter-spacing: 0.06em;
        text-transform: uppercase;
      }

      .field select,
      .field input {
        box-sizing: border-box;
        width: 100%;
        height: 42px;
        border: 1px solid #d6d3d1;
        border-radius: 10px;
        background: #fff;
        color: var(--ls-ink);
        padding: 0 0.75rem;
        font: inherit;
        font-size: 0.86rem;
        font-weight: 600;
      }

      .field select:focus,
      .field input:focus {
        border-color: var(--ls-brand);
        box-shadow: 0 0 0 3px rgba(21, 128, 61, 0.14);
        outline: none;
      }

      .field-action {
        justify-self: end;
      }

      .inline-error {
        margin: 0.75rem 0 0;
        color: #b91c1c;
        font-size: 0.82rem;
        font-weight: 600;
      }

      .sr-only {
        position: absolute;
        width: 1px;
        height: 1px;
        overflow: hidden;
        clip: rect(0 0 0 0);
        white-space: nowrap;
      }

      .kpi-grid {
        display: grid;
        grid-template-columns: repeat(4, minmax(0, 1fr));
        gap: var(--ls-gap);
        margin-bottom: var(--ls-gap);
      }

      .kpi-card {
        display: flex;
        flex-direction: column;
        gap: 0.35rem;
        padding: 1.25rem;
      }

      .kpi-label {
        color: var(--ls-muted);
        font-size: 0.72rem;
        font-weight: 800;
        letter-spacing: 0.06em;
        text-transform: uppercase;
      }

      .kpi-value {
        color: var(--ls-ink);
        font-size: clamp(1.35rem, 1.9vw, 1.7rem);
        font-weight: 800;
        font-variant-numeric: tabular-nums;
      }

      .kpi-hint {
        color: var(--ls-muted);
        font-size: 0.78rem;
      }

      .charts-grid {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: var(--ls-gap);
        margin-bottom: var(--ls-gap);
      }

      .detail-grid {
        display: grid;
        grid-template-columns: minmax(0, 1.5fr) minmax(0, 1fr);
        align-items: start;
        gap: var(--ls-gap);
      }

      .card {
        padding: 1.25rem;
      }

      .card-head {
        margin-bottom: 0.9rem;
      }

      .chart-wrapper {
        position: relative;
        width: 100%;
        height: 260px;
      }

      .chart-wrapper canvas {
        width: 100% !important;
        height: 100% !important;
      }

      .table-scroll {
        overflow-x: auto;
      }

      .data-table {
        width: 100%;
        border-collapse: collapse;
        font-size: 0.85rem;
      }

      .data-table th,
      .data-table td {
        padding: 0.6rem 0.5rem;
        border-bottom: 1px solid #f5f5f4;
        text-align: left;
        white-space: nowrap;
      }

      .data-table th {
        color: var(--ls-muted);
        font-size: 0.7rem;
        font-weight: 800;
        letter-spacing: 0.05em;
        text-transform: uppercase;
      }

      .data-table td.num,
      .data-table th.num {
        text-align: right;
        font-variant-numeric: tabular-nums;
      }

      .size-chip {
        display: inline-block;
        padding: 0.1rem 0.5rem;
        border-radius: 999px;
        background: #dcfce7;
        color: var(--ls-brand-dark);
        font-size: 0.75rem;
        font-weight: 800;
      }

      .list-stack {
        display: flex;
        flex-direction: column;
        gap: var(--ls-gap);
      }

      .product-list {
        display: flex;
        flex-direction: column;
        margin: 0;
        padding: 0;
        list-style: none;
      }

      .product-list li {
        display: flex;
        align-items: baseline;
        justify-content: space-between;
        gap: 0.75rem;
        padding: 0.55rem 0;
        border-bottom: 1px solid #f5f5f4;
        font-size: 0.85rem;
      }

      .product-list li:last-child {
        border-bottom: none;
        padding-bottom: 0;
      }

      .product-name {
        min-width: 0;
        overflow: hidden;
        color: #1f2937;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .product-meta {
        flex-shrink: 0;
        color: var(--ls-muted);
        font-size: 0.78rem;
        font-variant-numeric: tabular-nums;
      }

      .empty-cell {
        padding: 1rem 0.5rem;
        color: #9ca3af;
        font-size: 0.85rem;
        font-style: italic;
        text-align: center;
      }

      .loader {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 0.85rem;
        padding: 2.5rem;
        color: var(--ls-ink);
      }

      .spinner {
        width: 20px;
        height: 20px;
        border: 3px solid #d1fae5;
        border-top-color: var(--ls-brand);
        border-radius: 50%;
        animation: spin 0.8s linear infinite;
      }

      @keyframes spin {
        to {
          transform: rotate(360deg);
        }
      }

      .empty-state {
        padding: 2rem;
        text-align: center;
      }

      .empty-state h2 {
        margin: 0 0 0.5rem;
        color: var(--ls-ink);
      }

      .empty-state p {
        margin: 0 0 1rem;
        color: var(--ls-muted);
      }

      @media (max-width: 1024px) {
        .filters-grid {
          grid-template-columns: repeat(2, minmax(0, 1fr));
        }

        .field-action {
          justify-self: stretch;
        }

        .field-action .btn-secondary {
          width: 100%;
        }

        .kpi-grid {
          grid-template-columns: repeat(2, minmax(0, 1fr));
        }

        .detail-grid {
          grid-template-columns: 1fr;
        }
      }

      @media (max-width: 760px) {
        .dashboard-shell {
          padding: 1.5rem 1rem 2.5rem;
        }

        .charts-grid {
          grid-template-columns: 1fr;
        }
      }

      @media (max-width: 520px) {
        .filters-grid,
        .kpi-grid {
          grid-template-columns: 1fr;
        }

        .panel {
          padding: 1.1rem;
        }
      }
    `,
  ],
})
export class DashboardAnalyticsPage implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly dashboardService = inject(DashboardAnalyticsService);

  @ViewChild('ventasCanvas') ventasCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('demandaCanvas') demandaCanvas?: ElementRef<HTMLCanvasElement>;

  readonly categorias = CATEGORIAS_DASHBOARD;

  periodoSeleccionado = '30_DIAS';
  fechaDesde = '';
  fechaHasta = '';
  categoriaSeleccionada = 'TODAS';

  readonly startDate = signal('');
  readonly endDate = signal('');

  readonly overviewData = signal<DashboardOverview | null>(null);
  readonly masConsultados = signal<DashboardSerie[]>([]);
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly errorFechas = signal('');
  readonly errorGraficos = signal('');
  readonly esVendedor = signal(false);

  readonly ventasCategoria = computed(() => this.overviewData()?.ventasPorCategoria ?? []);
  readonly demandaTalla = computed(() => this.overviewData()?.demandaPorTalla ?? []);
  readonly productosPopulares = computed(() => this.overviewData()?.productosPopulares ?? []);
  readonly demandaDetalle = computed(() => this.overviewData()?.demandaDetalle ?? []);

  readonly tituloVista = computed(() =>
    this.esVendedor() ? 'Reportes de mi tienda' : 'Reportes administrativos',
  );
  readonly subtituloVista = computed(() =>
    this.esVendedor()
      ? 'Ventas, demanda e inventario de tu tienda. Solo ves tu propia información.'
      : 'Consolidado de la plataforma: ventas, demanda e inventario de todas las tiendas.',
  );

  private ventasChart?: Chart;
  private demandaChart?: Chart;

  private readonly recarga$ = new Subject<void>();

  constructor() {
    this.inicializarFechas();

    this.recarga$
      .pipe(
        tap(() => {
          this.cargando.set(true);
          this.error.set('');
        }),
        // switchMap cancela la petición anterior: solo cuenta el último filtro aplicado
        switchMap(() => {
          const categoria =
            this.categoriaSeleccionada === 'TODAS' ? undefined : this.categoriaSeleccionada;
          return forkJoin({
            overview: this.dashboardService.consultar(this.startDate(), this.endDate(), categoria),
            masConsultados: this.dashboardService.consultarTopMasConsultados(
              this.startDate(), this.endDate(), categoria),
          }).pipe(
            catchError((err: unknown) => {
              this.overviewData.set(null);
              this.masConsultados.set([]);
              this.error.set(this.mensajeError(err));
              this.cargando.set(false);
              return of(null);
            }),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((res) => {
        if (!res) return;
        this.overviewData.set(res.overview);
        this.masConsultados.set(res.masConsultados);
        this.cargando.set(false);
      });

    effect(() => {
      const overview = this.overviewData();
      if (this.cargando() || !overview) return;
      queueMicrotask(() => setTimeout(() => this.renderCharts(overview), 0));
    });
  }

  ngOnInit(): void {
    this.authService.perfil().subscribe({
      next: (perfil) => this.esVendedor.set(perfil.rol === 'VENDEDOR'),
      error: () => this.esVendedor.set(false),
    });
    this.cargarDatosDashboard();
  }

  ngOnDestroy(): void {
    this.destruirGraficos();
  }

  onPeriodoChange(): void {
    this.inicializarFechas();
    this.errorFechas.set('');
    this.cargarDatosDashboard();
  }

  onFechaManualChange(): void {
    if (!this.fechaDesde || !this.fechaHasta) {
      this.errorFechas.set('Selecciona ambas fechas para aplicar el filtro.');
      return;
    }
    if (this.fechaDesde > this.fechaHasta) {
      this.errorFechas.set('La fecha inicial no puede ser posterior a la final.');
      return;
    }
    this.errorFechas.set('');
    this.startDate.set(this.fechaDesde);
    this.endDate.set(this.fechaHasta);
    this.cargarDatosDashboard();
  }

  limpiarFiltros(): void {
    this.periodoSeleccionado = '30_DIAS';
    this.categoriaSeleccionada = 'TODAS';
    this.errorFechas.set('');
    this.inicializarFechas();
    this.cargarDatosDashboard();
  }

  cargarDatosDashboard(): void {
    if (!this.startDate() || !this.endDate()) return;
    this.recarga$.next();
  }

  onCategoriaChange(valor: string): void {
    this.categoriaSeleccionada = valor;
    this.cargarDatosDashboard();
  }

  formatCurrency(value: number): string {
    return `COP $ ${VALOR_COP.format(value ?? 0)}`;
  }

  formatNumber(value: number): string {
    return NUMERO_CO.format(value ?? 0);
  }

  tituloVentas(): string {
    return this.categoriaSeleccionada === 'TODAS' ? 'Ventas por categoría' : 'Ventas por prenda';
  }

  subtituloVentas(): string {
    return this.categoriaSeleccionada === 'TODAS'
      ? 'Distribución de los ingresos del período'
      : `Ingresos por prenda en ${this.etiquetaCategoria(this.categoriaSeleccionada)}`;
  }

  hayVentasGrafico(): boolean {
    return this.ventasParaGrafico().length > 0;
  }

  private ventasParaGrafico(): DashboardSerie[] {
    return this.categoriaSeleccionada === 'TODAS' ? this.ventasCategoria() : this.productosPopulares();
  }

  private inicializarFechas(): void {
    const hoy = new Date();
    let desde: Date;

    switch (this.periodoSeleccionado) {
      case 'MES_ACTUAL':
        desde = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
        break;
      case '6_MESES':
        desde = new Date(hoy.getFullYear(), hoy.getMonth() - 6, hoy.getDate());
        break;
      default:
        desde = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate() - 30);
        break;
    }

    this.fechaDesde = this.formatISODate(desde);
    this.fechaHasta = this.formatISODate(hoy);
    this.startDate.set(this.fechaDesde);
    this.endDate.set(this.fechaHasta);
  }

  private renderCharts(overview: DashboardOverview): void {
    this.destruirGraficos();
    this.errorGraficos.set('');

    const ventas =
      this.categoriaSeleccionada === 'TODAS' ? overview.ventasPorCategoria ?? [] : overview.productosPopulares ?? [];
    const demanda = overview.demandaPorTalla ?? [];

    try {
      this.crearGraficoVentas(ventas);
      this.crearGraficoDemanda(demanda);
    } catch (error: unknown) {
      this.destruirGraficos();
      this.errorGraficos.set(
        error instanceof Error && error.message
          ? `No se pudieron mostrar los gráficos: ${error.message}`
          : 'No se pudieron mostrar los gráficos. Los datos detallados siguen disponibles.',
      );
    }
  }

  private crearGraficoVentas(ventas: DashboardSerie[]): void {
    if (!this.ventasCanvas?.nativeElement || !ventas.length) {
      return;
    }

    this.ventasChart = new Chart(this.ventasCanvas.nativeElement, {
      type: 'bar',
      data: {
        labels: ventas.map((item) =>
          this.categoriaSeleccionada === 'TODAS' ? this.etiquetaCategoria(item.label) : item.label,
        ),
        datasets: [
          {
            data: ventas.map((item) => item.value),
            backgroundColor: '#15803d',
            borderRadius: 8,
            maxBarThickness: 48,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: (ctx) => this.formatCurrency(Number(ctx.raw)),
            },
          },
        },
        scales: {
          x: { grid: { display: false }, ticks: { color: '#6b7280' } },
          y: {
            beginAtZero: true,
            grid: { color: '#f5f5f4' },
            ticks: { color: '#6b7280', callback: (value) => this.formatCompact(Number(value)) },
          },
        },
      },
    });
  }

  private crearGraficoDemanda(demanda: DashboardSerie[]): void {
    if (!this.demandaCanvas?.nativeElement || !demanda.length) {
      return;
    }

    this.demandaChart = new Chart(this.demandaCanvas.nativeElement, {
      type: 'bar',
      data: {
        labels: demanda.map((item) => `Talla ${item.label}`),
        datasets: [
          {
            data: demanda.map((item) => item.cantidad),
            backgroundColor: '#0ea5e9',
            borderRadius: 8,
            maxBarThickness: 48,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: (ctx) => `${this.formatNumber(Number(ctx.raw))} unidades`,
            },
          },
        },
        scales: {
          x: { grid: { display: false }, ticks: { color: '#6b7280' } },
          y: { beginAtZero: true, grid: { color: '#f5f5f4' }, ticks: { color: '#6b7280' } },
        },
      },
    });
  }

  private destruirGraficos(): void {
    this.ventasChart?.destroy();
    this.ventasChart = undefined;
    this.demandaChart?.destroy();
    this.demandaChart = undefined;
  }

  private etiquetaCategoria(valor: string): string {
    return this.categorias.find((cat) => cat.valor === valor)?.etiqueta ?? valor;
  }

  private formatCompact(value: number): string {
    return COMPACTO_CO.format(value);
  }

  private mensajeError(err: unknown): string {
    if (err instanceof Error && err.message) return err.message;
    return 'No pudimos cargar los indicadores. Inténtalo de nuevo.';
  }

  private formatISODate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

}
