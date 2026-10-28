import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Navbar } from '../../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../../auth/auth.validators';
import { ResumenAdmin } from '../../models/admin.models';
import { ResumenAdminService } from '../../services/resumen-admin.service';

/**
 * UI de inicio del panel administrativo (US-14): indicadores consolidados de
 * usuarios, moderación, incidencias y auditoría con enlaces a cada módulo.
 * @see US-14
 */
@Component({
  selector: 'app-dashboard-admin',
  imports: [Navbar, RouterLink],
  templateUrl: './dashboard-admin.html',
  styles: `
    .dashboard-intro {
      margin-bottom: 1.5rem;
    }
    .dashboard-intro p {
      color: var(--ls-muted);
      margin-top: 0.25rem;
    }
  `,
})
export class DashboardAdmin implements OnInit {
  private readonly resumen = inject(ResumenAdminService);

  protected readonly datos = signal<ResumenAdmin | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);

  /**
   * Carga el resumen de KPIs al iniciar la página.
   * @returns No devuelve un valor.
   */
  ngOnInit(): void {
    this.cargar();
  }

  /**
   * Consulta el resumen y actualiza los estados de carga y error.
   * @returns No devuelve un valor.
   * @see US-14
   */
  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.resumen.consultar().subscribe({
      next: (res) => {
        this.datos.set(res);
        this.cargando.set(false);
      },
      error: (err: unknown) => {
        this.error.set(procesarErrorApi(err));
        this.cargando.set(false);
      },
    });
  }
}
