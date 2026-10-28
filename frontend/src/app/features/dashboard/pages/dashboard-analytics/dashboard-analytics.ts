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
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Chart, registerables } from 'chart.js';
import { Subscription } from 'rxjs';

import {
  DashboardAnalyticsService,
  DashboardOverview,
  DashboardSerie,
} from '../../services/dashboard-analytics.service';
import { Navbar } from '../../../../shared/components/navbar/navbar';
import { AuthService } from '../../../../core/services/auth.service';
import {
  ETIQUETAS_CATEGORIAS,
  OPCIONES_CATEGORIAS,
} from '../../../catalogoCompras/models/prenda-opciones';

Chart.register(...registerables);

const RESUMEN_VACIO: DashboardOverview = {
  totalVentas: 0,
  totalPedidos: 0,
  ticketPromedio: 0,
  productosVendidos: 0,
  margenBruto: null,
  stockTotal: 0,
  stockBajo: 0,
  ventasPorCategoria: [],
  demandaPorTalla: [],
  productosPopulares: [],
  productosMasConsultados: [],
  busquedasMasFrecuentes: [],
};

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
              @for (opcion of opcionesCategorias; track opcion.valor) {
                <option [value]="opcion.valor">{{ opcion.etiqueta }}</option>
              }
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
          <button type="button" class="retry-btn" (click)="cargarDatosDashboard()">Reintentar</button>
        </div>
      } @else {
        <section class="reports-grid-layout">
          <!-- KPIS (AMBOS ROLES): los datos ya vienen limitados por el backend según el token -->
          <div class="kpi-grid">
            <article class="kpi-card">
              <span>Ventas totales</span>
              <strong>{{ formatCurrency(resumen().totalVentas) }}</strong>
            </article>

            <article class="kpi-card">
              <span>Unidades vendidas</span>
              <strong>{{ formatUnits(resumen().productosVendidos) }}</strong>
            </article>

            <article class="kpi-card">
              <span>Ticket promedio</span>
              <strong>{{ formatCurrency(resumen().ticketPromedio) }}</strong>
            </article>

            <article class="kpi-card">
              <span>Pedidos</span>
              <strong>{{ resumen().totalPedidos.toLocaleString('es-CO') }}</strong>
            </article>

            <article class="kpi-card">
              <span>Inventario disponible</span>
              <strong>{{ formatUnits(resumen().stockTotal) }}</strong>
            </article>

            <article class="kpi-card" [class.kpi-alerta]="resumen().stockBajo > 0">
              <span>Tallas con stock bajo</span>
              <strong>{{ resumen().stockBajo.toLocaleString('es-CO') }}</strong>
            </article>

            @if (resumen().margenBruto !== null) {
              <article class="kpi-card">
                <span>Margen bruto</span>
                <strong>{{ formatCurrency(resumen().margenBruto ?? 0) }}</strong>
              </article>
            }
          </div>

          @if (resumen().margenBruto === null) {
            <p class="kpi-note">
              El margen bruto no está disponible porque las prendas todavía no registran su costo.
            </p>
          }

          @if (esVendedor()) {
            <!-- VISTA DEL VENDEDOR: DEMANDA Y VENTAS DE SU TIENDA -->
            <div class="sales-grid">
              <article class="chart-card">
                <div class="chart-header">
                  <h3>Demanda por talla (unidades)</h3>
                </div>
                <div class="chart-wrapper">
                  @if (sinDatosTalla()) {
                    <p class="empty-chart">Sin ventas en el período seleccionado</p>
                  }
                  <canvas #sellerLineChartCanvas [class.oculto]="sinDatosTalla()"></canvas>
                </div>
              </article>

              <article class="chart-card">
                <div class="chart-header">
                  <h3>Ventas por categoría (COP)</h3>
                </div>
                <div class="chart-wrapper">
                  @if (sinDatosCategorias()) {
                    <p class="empty-chart">Sin ventas en el período seleccionado</p>
                  }
                  <canvas #sellerBarChartCanvas [class.oculto]="sinDatosCategorias()"></canvas>
                </div>
              </article>
            </div>
          } @else {
            <!-- VISTA DEL ADMINISTRADOR: LISTAS DE DEMANDA DE TODA LA PLATAFORMA -->
            <div class="summary-grid">
              <article class="metric-card blue-card">
                <div class="card-header">
                  <h3>Más consultados</h3>
                </div>
                <ul class="product-list">
                  @for (item of mostViewedProducts(); track item.label) {
                    <li>
                      <span>{{ item.label }}</span>
                      <strong>{{ formatMetric(item.cantidad, 'vistas') }}</strong>
                    </li>
                  }
                  @if (mostViewedProducts().length === 0) {
                    <li class="empty-list">Sin consultas en el período seleccionado</li>
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
                      <strong>{{ formatMetric(item.cantidad, 'uds') }}</strong>
                    </li>
                  }
                  @if (bestSellerProducts().length === 0) {
                    <li class="empty-list">Sin ventas en el período seleccionado</li>
                  }
                </ul>
              </article>

              <article class="metric-card red-card">
                <div class="card-header">
                  <h3>Palabras más buscadas</h3>
                </div>
                <ul class="product-list">
                  @for (item of topSearches(); track item.label) {
                    <li>
                      <span>{{ item.label }}</span>
                      <strong>{{ formatMetric(item.cantidad, 'búsquedas') }}</strong>
                    </li>
                  }
                  @if (topSearches().length === 0) {
                    <li class="empty-list">Sin búsquedas en el período seleccionado</li>
                  }
                </ul>
              </article>
            </div>

            <div class="sales-grid">
              <article class="chart-card">
                <div class="chart-header">
                  <h3>Demanda por talla (unidades)</h3>
                </div>
                <div class="chart-wrapper">
                  @if (sinDatosTalla()) {
                    <p class="empty-chart">Sin ventas en el período seleccionado</p>
                  }
                  <canvas #adminLineChartCanvas [class.oculto]="sinDatosTalla()"></canvas>
                </div>
              </article>

              <article class="chart-card">
                <div class="chart-header">
                  <h3>Ventas por categoría (COP)</h3>
                </div>
                <div class="chart-wrapper">
                  @if (sinDatosCategorias()) {
                    <p class="empty-chart">Sin ventas en el período seleccionado</p>
                  }
                  <canvas #adminBarChartCanvas [class.oculto]="sinDatosCategorias()"></canvas>
                </div>
              </article>
            </div>
          }
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

      .chart-wrapper canvas.oculto {
        display: none;
      }

      .empty-chart {
        position: absolute;
        inset: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        margin: 0;
        color: #9ca3af;
        font-style: italic;
        text-align: center;
      }

      .kpi-alerta strong {
        color: #b45309;
      }

      .kpi-note {
        margin: -0.5rem 0 0;
        color: #6b7280;
        font-size: 0.82rem;
      }

      .retry-btn {
        margin-top: 0.75rem;
        padding: 0.55rem 1.2rem;
        border: 1px solid #15803d;
        border-radius: 10px;
        background: #ffffff;
        color: #15803d;
        font-weight: 700;
        cursor: pointer;
      }

      .retry-btn:hover {
        background: #f0fdf4;
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
export class DashboardAnalyticsPage implements OnInit, OnDestroy {
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
  /** `TODAS` o el código del enum de categoría (`VESTIDOS`, ...): es lo que espera el backend. */
  categoriaSeleccionada = 'TODAS';
  readonly opcionesCategorias = OPCIONES_CATEGORIAS;

  // Signals de estado
  startDate = signal('');
  endDate = signal('');

  overviewData = signal<DashboardOverview | null>(null);

  /** Ceros (no cifras inventadas) mientras no hay respuesta; el template solo se pinta tras cargar. */
  resumen = computed<DashboardOverview>(() => this.overviewData() ?? RESUMEN_VACIO);
  mostViewedProducts = computed(() => (this.overviewData()?.productosMasConsultados ?? []).slice(0, 5));
  bestSellerProducts = computed(() => (this.overviewData()?.productosPopulares ?? []).slice(0, 5));
  topSearches = computed(() => (this.overviewData()?.busquedasMasFrecuentes ?? []).slice(0, 5));
  sinDatosTalla = computed(() => !this.overviewData()?.demandaPorTalla?.length);
  sinDatosCategorias = computed(() => !this.overviewData()?.ventasPorCategoria?.length);

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
  private cargaEnCurso?: Subscription;

  constructor() {
    effect(() => {
      // Lee el rol para repintar si el perfil llega después de los datos: los canvas dependen de él.
      this.esVendedor();
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
    // Se conoce el rol antes de pedir datos para cargar una sola vez; si el perfil falla se muestra la vista
    // de vendedor, que es la más restrictiva (el backend limita los datos con el token de todos modos).
    this._authService.perfil().subscribe({
      next: (profile) => {
        this.esVendedor.set(profile.rol === 'VENDEDOR');
        this.onPeriodoChange();
      },
      error: () => {
        this.esVendedor.set(true);
        this.onPeriodoChange();
      },
    });
  }

  ngOnDestroy(): void {
    this.cargaEnCurso?.unsubscribe();
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
    this.cargarDatosDashboard();
  }

  /** Una sola petición: el resumen ya trae los tops y las búsquedas. Cancela la anterior si sigue en vuelo. */
  cargarDatosDashboard(): void {
    this.cargaEnCurso?.unsubscribe();
    this.cargando.set(true);
    this.error.set('');

    const categoria = this.categoriaSeleccionada === 'TODAS' ? undefined : this.categoriaSeleccionada;

    this.cargaEnCurso = this._dashboardAnalyticsService
      .consultar(this.startDate(), this.endDate(), categoria)
      .subscribe({
        next: (overview) => {
          this.overviewData.set(overview);
          this.cargando.set(false);
        },
        error: (err: unknown) => {
          this.overviewData.set(null);
          this.error.set(this.mensajeError(err));
          this.cargando.set(false);
        },
      });
  }

  private mensajeError(err: unknown): string {
    const status = err instanceof HttpErrorResponse ? err.status : 0;
    if (status === 401 || status === 403) {
      return 'No tienes permiso para ver este reporte. Inicia sesión de nuevo.';
    }
    if (status === 400) {
      return 'Los filtros seleccionados no son válidos. Revisa las fechas y la categoría.';
    }
    return 'No pudimos cargar las métricas. Revisa tu conexión e inténtalo de nuevo.';
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
    return `${value.toLocaleString('es-CO')} uds`;
  }

  /** Código de categoría (`VESTIDOS`) a etiqueta legible (`Vestidos`); si no se conoce, el mismo código. */
  etiquetaCategoria(codigo: string): string {
    return (ETIQUETAS_CATEGORIAS as Record<string, string>)[codigo] ?? codigo;
  }

  /** Solo datos reales: sin datos las listas quedan vacías y el template muestra el estado vacío. */
  private buildChartDataFromOverview(overview: DashboardOverview | null) {
    const tallas = overview?.demandaPorTalla ?? [];
    const categorias = overview?.ventasPorCategoria ?? [];

    return {
      lineLabels: tallas.map((i) => i.label),
      lineValues: tallas.map((i) => i.cantidad), // unidades por talla
      barLabels: categorias.map((i) => this.etiquetaCategoria(i.label)),
      barValues: categorias.map((i) => i.value), // monto vendido en COP
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

    this.barChart = new Chart(barCtx, {
      type: 'bar',
      data: {
        labels: barLabels,
        datasets: [
          {
            data: barValues,
            backgroundColor: '#15803d',
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
