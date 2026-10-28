import {
  AfterViewInit,
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
import { Chart, registerables } from 'chart.js';
import { forkJoin } from 'rxjs';

import {
  DashboardAnalyticsService,
  DashboardOverview,
  DashboardSerie,
} from '../../services/dashboard-analytics.service';
import { Navbar } from '../../../../shared/components/navbar/navbar';
import { AuthService } from '../../../../core/services/auth.service';

Chart.register(...registerables);

@Component({
  selector: 'app-dashboard-analytics',
  standalone: true,
  imports: [CommonModule, Navbar, FormsModule],
  template: `
    <app-navbar />
    <section class="dashboard-shell">
      <!-- HEADER CON TÍTULOS Y FILTROS -->
      <header class="dashboard-header">
        <div class="header-copy">
          <h2>{{ tituloVista() }}</h2>
          <p>{{ subtituloVista() }}</p>
        </div>

        <div class="header-filters" aria-label="Filtros del dashboard">
          <div class="filter-field">
            <span>PERIODO</span>
            <select [(ngModel)]="periodoSeleccionado" (change)="onPeriodoChange()">
              <option value="30_DIAS">Últimos 30 días</option>
              <option value="MES_ACTUAL">Este mes</option>
              <option value="6_MESES">Últimos 6 meses</option>
            </select>
          </div>

          <div class="filter-field date-field">
            <span>DESDE</span>
            <input type="date" [(ngModel)]="fechaDesde" (change)="onFechaManualChange()" />
          </div>

          <div class="filter-field date-field">
            <span>HASTA</span>
            <input type="date" [(ngModel)]="fechaHasta" (change)="onFechaManualChange()" />
          </div>

          <div class="filter-field category-field">
            <span>CATEGORÍAS</span>
            <select [(ngModel)]="categoriaSeleccionada" (change)="onCategoriaChange()">
              <option value="TODAS">Todas las categorías</option>
              <option value="Vestidos">Vestidos</option>
              <option value="Conjuntos">Conjuntos</option>
              <option value="Pantalones">Pantalones</option>
              <option value="Camisetas">Camisetas</option>
              <option value="Chaquetas">Chaquetas</option>
            </select>
          </div>
        </div>
      </header>

      @if (cargando()) {
        <div class="loader" role="status" aria-live="polite">
          <div class="spinner" aria-hidden="true"></div>
          <span>Cargando métricas de reportes...</span>
        </div>
      } @else if (error()) {
        <div class="empty-state error-state">
          <h3>Error al cargar datos</h3>
          <p>{{ error() }}</p>
        </div>
      } @else if (esVendedor()) {
        <!-- VISTA EXCLUSIVA DEL VENDEDOR (KPIS + VENTAS Y DEMANDA) -->
        <section class="reports-grid-layout">
          <!-- FILA SUPERIOR: KPIS DE VENDEDOR (3 COLUMNAS) -->
          <div class="kpi-grid">
            <article class="kpi-card">
              <span>Ventas totales</span>
              <strong>{{ formatCurrency(overviewData()?.totalVentas ?? 488000) }}</strong>
            </article>

            <article class="kpi-card">
              <span>Unidades vendidas</span>
              <strong>{{ formatUnits(overviewData()?.productosVendidos ?? 42) }}</strong>
            </article>

            <article class="kpi-card">
              <span>Ticket promedio</span>
              <strong>{{ formatCurrency(overviewData()?.ticketPromedio ?? 27111) }}</strong>
            </article>
          </div>

          <!-- FILA INFERIOR: GRÁFICOS DE VENDEDOR -->
          <div class="sales-grid">
            <article class="chart-card">
              <div class="chart-header">
                <h3>Ventas por período</h3>
              </div>
              <div class="chart-wrapper">
                <canvas #sellerLineChartCanvas></canvas>
              </div>
            </article>

            <article class="chart-card">
              <div class="chart-header">
                <h3>Demanda por talla</h3>
              </div>
              <div class="chart-wrapper">
                <canvas #sellerBarChartCanvas></canvas>
              </div>
            </article>
          </div>
        </section>
      } @else {
        <!-- VISTA EXCLUSIVA DEL ADMINISTRADOR (LISTAS DE DEMANDA + CATEGORÍAS) -->
        <section class="reports-grid-layout">
          <!-- FILA SUPERIOR: LISTAS DE PRODUCTOS (3 COLUMNAS) -->
          <div class="summary-grid">
            <article class="metric-card blue-card">
              <div class="card-header">
                <h3>Más consultados</h3>
              </div>
              <ul class="product-list">
                @for (item of mostViewedProducts(); track item.label) {
                  <li>
                    <span>{{ item.label }}</span>
                    <strong>{{ formatMetric(item.value, 'visitas') }}</strong>
                  </li>
                }
                @if (mostViewedProducts().length === 0) {
                  <li class="empty-list">Sin registros para esta categoría</li>
                }
              </ul>
            </article>

            <article class="metric-card green-card">
              <div class="card-header">
                <h3>Más vendidos</h3>
              </div>
              <ul class="product-list">
                @for (item of bestSellerProducts(); track item.label) {
                  <li>
                    <span>{{ item.label }}</span>
                    <strong>{{ formatMetric(item.value, 'uds') }}</strong>
                  </li>
                }
                @if (bestSellerProducts().length === 0) {
                  <li class="empty-list">Sin registros para esta categoría</li>
                }
              </ul>
            </article>

            <article class="metric-card red-card">
              <div class="card-header">
                <h3>Baja rotación</h3>
              </div>
              <ul class="product-list">
                @for (item of lowRotationProducts(); track item.label) {
                  <li>
                    <span>{{ item.label }}</span>
                    <strong>{{ formatMetric(item.value, 'uds') }}</strong>
                  </li>
                }
                @if (lowRotationProducts().length === 0) {
                  <li class="empty-list">Sin registros para esta categoría</li>
                }
              </ul>
            </article>
          </div>

          <!-- FILA INFERIOR: GRÁFICOS DE ADMINISTRADOR -->
          <div class="sales-grid">
            <article class="chart-card">
              <div class="chart-header">
                <h3>Demanda por talla</h3>
              </div>
              <div class="chart-wrapper">
                <canvas #adminLineChartCanvas></canvas>
              </div>
            </article>

            <article class="chart-card">
              <div class="chart-header">
                <h3>Ventas por categoría</h3>
              </div>
              <div class="chart-wrapper">
                <canvas #adminBarChartCanvas></canvas>
              </div>
            </article>
          </div>
        </section>
      }
    </section>
  `,
  styles: [
    `
      :host {
        display: block;
      }

      .dashboard-shell {
        background: #fafaf9;
        padding: 2rem 1.75rem 2.5rem;
        color: #14532d;
        font-family: var(--ls-font, 'Nunito', 'Segoe UI', sans-serif);
        min-height: 100vh;
        box-sizing: border-box;
      }

      .dashboard-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-end;
        gap: 1.25rem;
        margin-bottom: 1.5rem;
        padding: 1.2rem 1.5rem;
        background: #ffffff;
        border: 1px solid #e7e5e4;
        border-radius: 16px;
        box-shadow: 0 4px 12px rgba(15, 23, 42, 0.03);
        flex-wrap: wrap;
      }

      .header-copy h2 {
        margin: 0 0 0.25rem;
        font-size: clamp(1.8rem, 2.2vw, 2.4rem);
        line-height: 1.1;
        letter-spacing: -0.04em;
        color: #14532d;
        font-weight: 800;
      }

      .header-copy p {
        margin: 0;
        color: #4b5563;
        font-size: 0.92rem;
      }

      .header-filters {
        display: flex;
        flex-wrap: wrap;
        justify-content: flex-end;
        align-items: flex-end;
        gap: 0.85rem;
      }

      .filter-field {
        display: flex;
        flex-direction: column;
        gap: 0.35rem;
        min-width: 140px;
      }

      .date-field {
        min-width: 140px;
      }

      .category-field {
        min-width: 190px;
      }

      .filter-field span {
        color: #6b7280;
        font-size: 0.68rem;
        letter-spacing: 0.06em;
        text-transform: uppercase;
        font-weight: 800;
      }

      .filter-field select,
      .filter-field input {
        width: 100%;
        height: 42px;
        background: #ffffff;
        border: 1px solid #d6d3d1;
        color: #14532d;
        border-radius: 10px;
        padding: 0.5rem 0.75rem;
        font: inherit;
        font-size: 0.85rem;
        font-weight: 600;
        box-sizing: border-box;
      }

      .filter-field select:focus,
      .filter-field input:focus {
        outline: none;
        border-color: #15803d;
        box-shadow: 0 0 0 3px rgba(21, 128, 61, 0.12);
      }

      .reports-grid-layout {
        display: flex;
        flex-direction: column;
        gap: 1.25rem;
      }

      /* TARJETAS KPI DE VENDEDOR */
      .kpi-grid {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 1.25rem;
      }

      .kpi-card {
        background: #ffffff;
        border: 1px solid #e7e5e4;
        border-radius: 16px;
        padding: 1.25rem;
        box-shadow: 0 4px 12px rgba(15, 23, 42, 0.03);
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
      }

      .kpi-card span {
        color: #6b7280;
        font-size: 0.75rem;
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }

      .kpi-card strong {
        color: #14532d;
        font-size: clamp(1.4rem, 2vw, 1.8rem);
        font-weight: 800;
      }

      /* LISTAS RESUMEN DE ADMINISTRADOR */
      .summary-grid {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 1.25rem;
      }

      .sales-grid {
        display: grid;
        grid-template-columns: 1.2fr 0.8fr;
        gap: 1.25rem;
      }

      .metric-card,
      .chart-card {
        background: #ffffff;
        border: 1px solid #e7e5e4;
        border-radius: 16px;
        box-shadow: 0 4px 12px rgba(15, 23, 42, 0.03);
      }

      .metric-card {
        padding: 1.2rem;
      }

      .chart-card {
        padding: 1.2rem;
        min-height: 320px;
      }

      .card-header,
      .chart-header {
        margin-bottom: 0.85rem;
      }

      .metric-card h3,
      .chart-card h3 {
        margin: 0;
        font-size: 1.05rem;
        color: #14532d;
        font-weight: 700;
      }

      .product-list {
        list-style: none;
        margin: 0;
        padding: 0;
        display: flex;
        flex-direction: column;
      }

      .product-list li {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 0.8rem;
        padding: 0.65rem 0;
        border-bottom: 1px solid #f5f5f4;
        color: #1f2937;
        font-size: 0.9rem;
      }

      .product-list li:last-child {
        border-bottom: none;
        padding-bottom: 0;
      }

      .product-list span {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        max-width: 70%;
      }

      .blue-card strong { color: #2563eb; font-weight: 700; font-size: 0.88rem; }
      .green-card strong { color: #166534; font-weight: 700; font-size: 0.88rem; }
      .red-card strong { color: #dc2626; font-weight: 700; font-size: 0.88rem; }

      .empty-list {
        color: #9ca3af !important;
        font-style: italic;
        justify-content: center !important;
      }

      .chart-wrapper {
        position: relative;
        width: 100%;
        height: 270px;
      }

      .chart-wrapper canvas {
        width: 100% !important;
        height: 100% !important;
      }

      .loader,
      .empty-state {
        background: #ffffff;
        border: 1px solid #e7e5e4;
        border-radius: 16px;
        padding: 2rem;
        color: #14532d;
        text-align: center;
      }

      .loader {
        display: flex;
        justify-content: center;
        align-items: center;
        gap: 0.85rem;
      }

      .spinner {
        width: 20px;
        height: 20px;
        border-radius: 50%;
        border: 3px solid #d1fae5;
        border-top-color: #15803d;
        animation: spin 0.8s linear infinite;
      }

      @keyframes spin {
        to { transform: rotate(360deg); }
      }

      @media (max-width: 900px) {
        .dashboard-header {
          flex-direction: column;
          align-items: flex-start;
        }

        .header-filters {
          justify-content: flex-start;
          width: 100%;
        }

        .kpi-grid,
        .summary-grid,
        .sales-grid {
          grid-template-columns: 1fr;
        }

        .filter-field {
          flex: 1 1 130px;
        }
      }
    `,
  ],
})
export class DashboardAnalyticsPage implements OnInit, AfterViewInit, OnDestroy {
  private readonly _authService = inject(AuthService);
  private readonly _dashboardAnalyticsService = inject(DashboardAnalyticsService);

  @ViewChild('sellerLineChartCanvas') sellerLineChartCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('sellerBarChartCanvas') sellerBarChartCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('adminLineChartCanvas') adminLineChartCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('adminBarChartCanvas') adminBarChartCanvas!: ElementRef<HTMLCanvasElement>;

  // Variables de Filtro
  periodoSeleccionado = '30_DIAS';
  fechaDesde = '';
  fechaHasta = '';
  categoriaSeleccionada: string = 'TODAS';

  // Signals de estado
  startDate = signal('2026-09-27');
  endDate = signal('2026-10-27');
  categoryId = signal<string>('');

  mostViewedProducts = signal<DashboardSerie[]>([]);
  bestSellerProducts = signal<DashboardSerie[]>([]);
  lowRotationProducts = signal<DashboardSerie[]>([]);
  overviewData = signal<DashboardOverview | null>(null);

  cargando = signal(true);
  error = signal('');
  esVendedor = signal(true);

  // Títulos dinámicos según el Rol
  tituloVista = computed(() =>
    this.esVendedor() ? 'Reportes del vendedor' : 'Reportes administrativos'
  );

  subtituloVista = computed(() =>
    this.esVendedor()
      ? 'Evolución de tu catálogo, pedidos y ventas de tu tienda.'
      : 'Análisis general de la operación de la plataforma.'
  );

  private lineChart?: Chart<'line'>;
  private barChart?: Chart<'bar'>;

  private readonly defaultMostViewed: DashboardSerie[] = [
    { label: 'Vestido de lino', value: 1240, cantidad: 1240 },
    { label: 'Pantalón cargo', value: 980, cantidad: 980 },
    { label: 'Chaqueta urbana', value: 840, cantidad: 840 },
    { label: 'Conjunto premium', value: 760, cantidad: 760 },
  ];

  private readonly defaultBestSeller: DashboardSerie[] = [
    { label: 'Vestido de lino', value: 124, cantidad: 124 },
    { label: 'Pantalón cargo', value: 98, cantidad: 98 },
    { label: 'Chaqueta urbana', value: 82, cantidad: 82 },
    { label: 'Conjunto premium', value: 76, cantidad: 76 },
  ];

  private readonly defaultLowRotation: DashboardSerie[] = [
    { label: 'Saco de lana', value: 2, cantidad: 2 },
    { label: 'Blazer corto', value: 1, cantidad: 1 },
    { label: 'Safari oversize', value: 0, cantidad: 0 },
    { label: 'Pantalón clásico', value: 0, cantidad: 0 },
  ];

  constructor() {
    effect(() => {
      if (!this.cargando()) {
        const overview = this.overviewData();
        const chartData = this.buildChartDataFromOverview(overview);
        queueMicrotask(() =>
          this.renderCharts(
            chartData.lineLabels,
            chartData.lineValues,
            chartData.barLabels,
            chartData.barValues
          )
        );
      }
    });
  }

  ngOnInit(): void {
    this._authService.perfil().subscribe({
      next: (profile) => {
        this.esVendedor.set(profile.rol === 'VENDEDOR');
      },
      error: () => {
        this.esVendedor.set(true);
      },
    });

    this.onPeriodoChange();
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.cargarDatosDashboard(), 100);
  }

  ngOnDestroy(): void {
    this.destruirGraficos();
  }

  // --- SINCRONIZACIÓN DE FECHAS ---
  onPeriodoChange(): void {
    const hoy = new Date();

    switch (this.periodoSeleccionado) {
      case '30_DIAS':
        this.fechaHasta = this.formatISODate(hoy);
        this.fechaDesde = this.formatISODate(this.addDays(hoy, -30));
        break;
      case 'MES_ACTUAL':
        this.fechaHasta = this.formatISODate(hoy);
        this.fechaDesde = this.formatISODate(new Date(hoy.getFullYear(), hoy.getMonth(), 1));
        break;
      case '6_MESES':
        this.fechaHasta = this.formatISODate(hoy);
        this.fechaDesde = this.formatISODate(this.addMonths(hoy, -6));
        break;
    }

    this.startDate.set(this.fechaDesde);
    this.endDate.set(this.fechaHasta);
    this.cargarDatosDashboard();
  }

  onFechaManualChange(): void {
    if (this.fechaDesde && this.fechaHasta && this.fechaDesde <= this.fechaHasta) {
      this.startDate.set(this.fechaDesde);
      this.endDate.set(this.fechaHasta);
      this.cargarDatosDashboard();
    }
  }

  onCategoriaChange(): void {
    this.categoryId.set(
      this.categoriaSeleccionada === 'TODAS' ? '' : String(this.categoriaSeleccionada)
    );
    this.cargarDatosDashboard();
  }

  cargarDatosDashboard(): void {
    this.cargando.set(true);
    this.error.set('');

    const categoryParam =
      this.categoriaSeleccionada === 'TODAS' ? undefined : Number(this.categoriaSeleccionada);

    forkJoin({
      overview: this._dashboardAnalyticsService.consultar(
        this.startDate(),
        this.endDate(),
        categoryParam
      ),
      mostViewed: this._dashboardAnalyticsService.consultarTopMasConsultados(
        this.startDate(),
        this.endDate(),
        categoryParam
      ),
      bestSeller: this._dashboardAnalyticsService.consultarTopMasVendidos(
        this.startDate(),
        this.endDate(),
        categoryParam
      ),
    }).subscribe({
      next: ({ overview, mostViewed, bestSeller }) => {
        const filteredMostViewed = this.filterSeriesByCategory(mostViewed);
        const filteredBestSeller = this.filterSeriesByCategory(bestSeller);

        this.overviewData.set(overview);
        this.mostViewedProducts.set(
          filteredMostViewed.length ? filteredMostViewed.slice(0, 4) : this.defaultMostViewed
        );
        this.bestSellerProducts.set(
          filteredBestSeller.length ? filteredBestSeller.slice(0, 4) : this.defaultBestSeller
        );
        this.lowRotationProducts.set(this.defaultLowRotation);

        this.cargando.set(false);
      },
      error: () => {
        this.overviewData.set(null);
        this.mostViewedProducts.set(this.defaultMostViewed);
        this.bestSellerProducts.set(this.defaultBestSeller);
        this.lowRotationProducts.set(this.defaultLowRotation);
        this.cargando.set(false);
      },
    });
  }

  formatMetric(value: number, unit: string): string {
    return `${value.toLocaleString('es-CO')} ${unit}`;
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency: 'COP',
      maximumFractionDigits: 0,
    }).format(value);
  }

  formatUnits(value: number): string {
    return `${value.toLocaleString('es-CO')} ventas`;
  }

  private filterSeriesByCategory<T extends Record<string, any>>(items: T[] = []): T[] {
    if (this.categoriaSeleccionada === 'TODAS' || !this.categoriaSeleccionada) {
      return items;
    }
    const cat = String(this.categoriaSeleccionada).trim().toLowerCase();
    return items.filter((item) => {
      const catObj = item['categoria'];
      const catNombre = catObj && typeof catObj === 'object' ? catObj['nombre'] : undefined;
      const catId = item['categoriaId'];
      return (
        String(catNombre ?? '').trim().toLowerCase() === cat ||
        String(catId ?? '').trim().toLowerCase() === cat
      );
    });
  }

  private buildChartDataFromOverview(overview: DashboardOverview | null) {
    const lineData = overview?.demandaPorTalla?.length ? overview.demandaPorTalla : [];
    const barData = overview?.ventasPorCategoria?.length ? overview.ventasPorCategoria : [];

    const lineMocks: Record<string, { labels: string[]; values: number[] }> = {
      TODAS: { labels: ['6', '8', '10', '12'], values: [12, 18, 15, 9] },
      Vestidos: { labels: ['6', '8', '10', '12'], values: [15, 22, 17, 8] },
      Conjuntos: { labels: ['6', '8', '10', '12'], values: [10, 16, 13, 7] },
      Pantalones: { labels: ['6', '8', '10', '12'], values: [7, 12, 19, 14] },
      Camisetas: { labels: ['6', '8', '10', '12'], values: [9, 14, 11, 6] },
      Chaquetas: { labels: ['6', '8', '10', '12'], values: [4, 8, 12, 6] },
    };

    const fallbackLine = lineMocks[String(this.categoriaSeleccionada)] || lineMocks['TODAS'];

    return {
      lineLabels: lineData.length ? lineData.map((i) => i.label) : fallbackLine.labels,
      lineValues: lineData.length ? lineData.map((i) => i.value) : fallbackLine.values,
      barLabels: barData.length
        ? barData.map((i) => i.label)
        : ['Vestidos', 'Conjuntos', 'Pantalones', 'Camisetas', 'Chaquetas'],
      barValues: barData.length ? barData.map((i) => i.value) : [170000, 140000, 95000, 72000, 50000],
    };
  }

  private renderCharts(
    lineLabels: string[],
    lineValues: number[],
    barLabels: string[],
    barValues: number[]
  ): void {
    this.destruirGraficos();

    const lineCanvas = this.esVendedor() ? this.sellerLineChartCanvas : this.adminLineChartCanvas;
    const barCanvas = this.esVendedor() ? this.sellerBarChartCanvas : this.adminBarChartCanvas;

    if (!lineCanvas || !barCanvas) return;

    const lineCtx = lineCanvas.nativeElement.getContext('2d');
    const barCtx = barCanvas.nativeElement.getContext('2d');

    if (!lineCtx || !barCtx) return;

    this.lineChart = new Chart(lineCtx, {
      type: 'line',
      data: {
        labels: lineLabels,
        datasets: [
          {
            data: lineValues,
            borderColor: '#15803d',
            backgroundColor: 'rgba(34, 197, 94, 0.15)',
            borderWidth: 3,
            pointRadius: 4,
            pointBackgroundColor: '#15803d',
            pointBorderColor: '#ffffff',
            fill: true,
            tension: 0.38,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: { grid: { display: false }, ticks: { color: '#6b7280' } },
          y: { beginAtZero: true, grid: { color: '#f5f5f4' }, ticks: { color: '#6b7280' } },
        },
      },
    });

    const coloresBarras = barLabels.map((label) =>
      this.categoriaSeleccionada === 'TODAS' || this.categoriaSeleccionada === label
        ? '#15803d'
        : '#e7e5e4'
    );

    this.barChart = new Chart(barCtx, {
      type: 'bar',
      data: {
        labels: barLabels,
        datasets: [
          {
            data: barValues,
            backgroundColor: coloresBarras,
            borderRadius: 8,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: { grid: { display: false }, ticks: { color: '#6b7280' } },
          y: { beginAtZero: true, grid: { color: '#f5f5f4' }, ticks: { color: '#6b7280' } },
        },
      },
    });
  }

  private destruirGraficos(): void {
    if (this.lineChart) {
      this.lineChart.destroy();
      this.lineChart = undefined;
    }
    if (this.barChart) {
      this.barChart.destroy();
      this.barChart = undefined;
    }
  }

  private formatISODate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private addDays(date: Date, days: number): Date {
    const res = new Date(date);
    res.setDate(res.getDate() + days);
    return res;
  }

  private addMonths(date: Date, months: number): Date {
    const res = new Date(date);
    res.setMonth(res.getMonth() + months);
    return res;
  }
}
