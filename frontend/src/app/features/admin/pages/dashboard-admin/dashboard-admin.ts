import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Navbar } from '../../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../../auth/auth.validators';
import { ResumenAdmin } from '../../models/admin.models';
import { ResumenAdminService } from '../../services/resumen-admin.service';

/**
 * UI de inicio del panel administrativo (US-14): indicadores consolidados de
 * usuarios, moderación, incidencias y auditoría con enlaces a cada módulo.
 * Los indicadores se refrescan al volver a la pestaña para no mostrar cifras viejas.
 * @see US-14
 */
@Component({
  selector: 'app-dashboard-admin',
  imports: [Navbar, RouterLink],
  templateUrl: './dashboard-admin.html',
  styles: `
    .admin-home {
      max-width: 1240px;
    }

    .dashboard-intro {
      display: flex;
      flex-wrap: wrap;
      align-items: flex-end;
      justify-content: space-between;
      gap: 1rem;
      margin-bottom: 1.5rem;
    }

    .dashboard-intro h1 {
      margin: 0;
    }

    .dashboard-intro p {
      margin: 0.25rem 0 0;
      color: var(--ls-muted);
    }

    .intro-actions {
      display: flex;
      align-items: center;
      gap: 0.85rem;
    }

    .intro-meta {
      color: var(--ls-muted);
      font-size: 0.8rem;
    }

    .btn-refresh {
      min-height: 42px;
      border: 1px solid var(--ls-border);
      border-radius: 10px;
      background: #fff;
      color: var(--ls-primary-dark);
      padding: 0 1.1rem;
      font: inherit;
      font-size: 0.85rem;
      font-weight: 700;
      cursor: pointer;
    }

    .btn-refresh:disabled {
      cursor: not-allowed;
      opacity: 0.55;
    }

    .admin-kpis {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 1.25rem;
      margin-bottom: 1.5rem;
    }

    .admin-kpis .kpi-card {
      transition: transform 160ms ease, border-color 160ms ease;
    }

    .admin-kpis .kpi-card:hover {
      transform: translateY(-2px);
      border-color: var(--ls-primary);
      text-decoration: none;
    }

    .admin-tiles {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(min(100%, 190px), 1fr));
      align-items: stretch;
      gap: clamp(0.75rem, 1.5vw, 1.25rem);
    }

    .admin-tiles .home-tile {
      box-sizing: border-box;
      height: 100%;
      min-height: 150px;
    }

    .admin-tiles .home-tile-moderacion h2 {
      margin-top: 0.75rem;
    }

    .admin-tiles .home-tile-moderacion > span {
      flex: 0 0 auto;
    }

    @media (max-width: 1024px) {
      .admin-kpis {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }
    }

    @media (max-width: 520px) {
      .admin-kpis {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class DashboardAdmin implements OnInit, OnDestroy {
  private readonly resumen = inject(ResumenAdminService);

  protected readonly datos = signal<ResumenAdmin | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly ultimaActualizacion = signal<string | null>(null);

  private ultimaCarga = 0;
  private temporizadorActualizacion?: number;

  private readonly alCambiarVisibilidad = (): void => {
    if (document.visibilityState === 'visible') {
      this.refrescarSiHaceFalta();
    }
  };

  private readonly alEnfocar = (): void => {
    this.refrescarSiHaceFalta();
  };

  /**
   * Carga el resumen de KPIs al iniciar la página y registra el refresco al volver a la pestaña.
   * @returns No devuelve un valor.
   */
  ngOnInit(): void {
    this.cargar();
    this.temporizadorActualizacion = window.setInterval(() => this.refrescarSiHaceFalta(), 30_000);
    document.addEventListener('visibilitychange', this.alCambiarVisibilidad);
    window.addEventListener('focus', this.alEnfocar);
  }

  ngOnDestroy(): void {
    if (this.temporizadorActualizacion !== undefined) {
      window.clearInterval(this.temporizadorActualizacion);
    }
    document.removeEventListener('visibilitychange', this.alCambiarVisibilidad);
    window.removeEventListener('focus', this.alEnfocar);
  }

  /**
   * Consulta el resumen y actualiza los estados de carga y error.
   * @param silencioso si es {@code true} no muestra el loader para no parpadear en un refresco.
   * @returns No devuelve un valor.
   * @see US-14
   */
  protected cargar(silencioso = false): void {
    if (!silencioso) {
      this.cargando.set(true);
    }
    this.error.set(null);
    this.resumen.consultar().subscribe({
      next: (res) => {
        this.datos.set(res);
        this.ultimaCarga = Date.now();
        this.ultimaActualizacion.set(
          new Intl.DateTimeFormat('es-CO', { hour: '2-digit', minute: '2-digit' }).format(new Date()),
        );
        this.cargando.set(false);
      },
      error: (err: unknown) => {
        if (!this.datos()) {
          this.error.set(procesarErrorApi(err));
        }
        this.cargando.set(false);
      },
    });
  }

  /** Evita ráfagas de peticiones cuando la ventana gana foco varias veces seguidas. */
  private refrescarSiHaceFalta(): void {
    if (this.cargando() || Date.now() - this.ultimaCarga < 10_000) {
      return;
    }
    this.cargar(true);
  }
}
