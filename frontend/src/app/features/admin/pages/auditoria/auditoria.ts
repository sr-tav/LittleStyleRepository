import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Navbar } from '../../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../../auth/auth.validators';
import {
  ETIQUETAS_ACCION, ETIQUETAS_RESULTADO, ETIQUETAS_TIPO_ENTIDAD,
  FiltrosAuditoria, Pagina, RegistroAuditoria, RespuestaIntegridad,
} from '../../models/admin.models';
import { AuditoriaService } from '../../services/auditoria.service';

/**
 * US-14: visor de solo lectura de la auditoría con filtros, paginación y
 * verificación de la integridad de la cadena de hashes.
 * @see US-14
 * @see RNF-17
 */

@Component({
  selector: 'app-auditoria',
  imports: [Navbar, DatePipe],
  templateUrl: './auditoria.html',
  styles: `
    .admin-filtros {
      display: flex;
      flex-wrap: wrap;
      align-items: flex-end;
      gap: 0.75rem;
      margin-bottom: 1.25rem;
    }
    .admin-filtros .field {
      flex: 1 1 170px;
      margin-bottom: 0;
    }
    .admin-paginado {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      margin-top: 1.25rem;
      color: var(--ls-muted);
      font-size: 0.9rem;
    }
    .auditoria-table {
      min-width: 900px;
      table-layout: fixed;
    }
    .auditoria-table th:nth-child(1),
    .auditoria-table td:nth-child(1) { width: 15%; }
    .auditoria-table th:nth-child(2),
    .auditoria-table td:nth-child(2) { width: 18%; overflow-wrap: anywhere; }
    .auditoria-table th:nth-child(3),
    .auditoria-table td:nth-child(3) { width: 17%; overflow-wrap: anywhere; }
    .auditoria-table th:nth-child(4),
    .auditoria-table td:nth-child(4) { width: 12%; }
    .auditoria-table th:nth-child(5),
    .auditoria-table td:nth-child(5) { width: 15%; }
    .auditoria-table th:nth-child(6),
    .auditoria-table td:nth-child(6) { width: 10%; }
    .auditoria-table th:nth-child(7),
    .auditoria-table td:nth-child(7) { width: 13%; overflow-wrap: anywhere; }
    .auditoria-table td:nth-child(5) {
      overflow-wrap: anywhere;
    }
    .chip-FALLO {
      background: var(--ls-error-soft);
      color: var(--ls-error);
    }
    .chip-INTEGRIDAD-MALA {
      background: var(--ls-error-soft);
      color: var(--ls-error);
    }
    .valor-cambio {
      display: block;
      font-size: 0.82rem;
      color: var(--ls-muted);
    }
    .valor-cambio s {
      opacity: 0.7;
    }
    .admin-filtros select,
    .admin-filtros input[type='datetime-local'] {
      width: 100%;
      padding: 0.8rem 1rem;
      border: 1.5px solid var(--ls-border);
      border-radius: var(--ls-radius);
      background: #fffef7;
      color: var(--ls-ink);
      font: inherit;
      font-size: 0.97rem;
      transition: border-color 0.15s, box-shadow 0.15s;
    }
    .admin-filtros select:focus,
    .admin-filtros input[type='datetime-local']:focus {
      outline: none;
      border-color: var(--ls-primary);
      box-shadow: 0 0 0 4px var(--ls-primary-soft);
    }
    .admin-filtros .field {
      flex: 1 1 200px;
    }
    .admin-filtros .filtros-acciones {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }
  `,
})
export class AuditoriaPage implements OnInit {
  private readonly auditoria = inject(AuditoriaService);

  protected readonly etiquetasResultado = ETIQUETAS_RESULTADO;

  protected readonly filtroEmail = signal('');
  protected readonly filtroAccion = signal('');
  protected readonly filtroTipoEntidad = signal('');
  protected readonly filtroEntidadId = signal('');

  protected readonly items = signal<RegistroAuditoria[]>([]);
  protected readonly pagina = signal(0);
  protected readonly totalPaginas = signal(0);
  protected readonly totalElementos = signal(0);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly integridad = signal<RespuestaIntegridad | null>(null);
  protected readonly integridadCargando = signal(false);
  protected readonly errorIntegridad = signal<string | null>(null);
  protected readonly etiquetasAccion = ETIQUETAS_ACCION;
  protected readonly etiquetasTipo = ETIQUETAS_TIPO_ENTIDAD;
  protected readonly acciones = Object.keys(ETIQUETAS_ACCION);
  protected readonly tipos = Object.keys(ETIQUETAS_TIPO_ENTIDAD);

  protected readonly filtroResultado = signal('');
  protected readonly filtroDesde = signal('');
  protected readonly filtroHasta = signal('');
  /**
   * Carga la primera página del visor al iniciar.
   * @returns No devuelve un valor.
   */
  ngOnInit(): void {
    this.cargar();
  }

  /**
   * Consulta los registros de auditoría con los filtros vigentes.
   * @returns No devuelve un valor.
   * @see US-14
   */
  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    const entidadId = this.filtroEntidadId().trim();
    const filtros: FiltrosAuditoria = {
      adminEmail: this.filtroEmail().trim() || undefined,
      accion: this.filtroAccion() || undefined,
      tipoEntidad: this.filtroTipoEntidad() || undefined,
      resultado: this.filtroResultado() || undefined,
      entidadId: /^\d+$/.test(entidadId) ? Number(entidadId) : undefined,
      desde: this.filtroDesde() ? new Date(this.filtroDesde()).toISOString() : undefined,
      hasta: this.filtroHasta() ? new Date(this.filtroHasta()).toISOString() : undefined,
      pagina: this.pagina(),
      tamano: 10,
    };
    this.auditoria.listar(filtros).subscribe({
      next: (res: Pagina<RegistroAuditoria>) => {
        this.items.set(res.contenido);
        this.pagina.set(res.pagina);
        this.totalPaginas.set(res.totalPaginas);
        this.totalElementos.set(res.totalElementos);
        this.cargando.set(false);
      },
      error: (err: unknown) => {
        this.error.set(procesarErrorApi(err));
        this.cargando.set(false);
      },
    });
  }

  protected hayFiltros(): boolean {
    return !!(this.filtroEmail() || this.filtroAccion() || this.filtroTipoEntidad()
      || this.filtroEntidadId() || this.filtroResultado() || this.filtroDesde() || this.filtroHasta());
  }

  /**
   * Aplica los filtros y vuelve a la primera página.
   * @returns No devuelve un valor.
   */
  protected aplicarFiltros(): void {
    this.pagina.set(0);
    this.cargar();
  }

  /**
   * Limpia los filtros y recarga el visor.
   * @returns No devuelve un valor.
   */
  protected limpiarFiltros(): void {
    for (const f of [this.filtroEmail, this.filtroAccion, this.filtroTipoEntidad,
      this.filtroEntidadId, this.filtroResultado, this.filtroDesde, this.filtroHasta]) {
      f.set('');
    }
    this.pagina.set(0);
    this.cargar();
  }

  /**
   * Retrocede una página cuando existe una anterior.
   * @returns No devuelve un valor.
   */
  protected anterior(): void {
    if (this.pagina() > 0) {
      this.pagina.update((p) => p - 1);
      this.cargar();
    }
  }

  /**
   * Avanza una página cuando quedan resultados.
   * @returns No devuelve un valor.
   */
  protected siguiente(): void {
    if (this.pagina() < this.totalPaginas() - 1) {
      this.pagina.update((p) => p + 1);
      this.cargar();
    }
  }

  /**
   * Verifica la integridad de la cadena de hashes y muestra el resultado.
   * @returns No devuelve un valor.
   * @see US-14
   */
  protected verificarIntegridad(): void {
    this.integridadCargando.set(true);
    this.errorIntegridad.set(null);
    this.integridad.set(null);
    this.auditoria.verificarIntegridad().subscribe({
      next: (res) => {
        this.integridad.set(res);
        this.integridadCargando.set(false);
      },
      error: (err: unknown) => {
        this.errorIntegridad.set(procesarErrorApi(err));
        this.integridadCargando.set(false);
      },
    });
  }

  protected etiquetaAccion(accion: string): string {
    return this.etiquetasAccion[accion] ?? accion;
  }

  protected etiquetaTipo(tipo: string): string {
    return this.etiquetasTipo[tipo] ?? tipo;
  }
}
