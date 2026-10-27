import { Component, ElementRef, OnInit, inject, signal, viewChild } from '@angular/core';

import { Navbar } from '../../../../shared/components/navbar/navbar';
import { Rol } from '../../../../core/models/auth.models';
import { procesarErrorApi } from '../../../auth/auth.validators';
import {
  ETIQUETAS_ESTADO_INCIDENCIA,
  ETIQUETAS_ROL,
  ETIQUETAS_TIPO_INCIDENCIA,
  EstadoIncidencia,
  FiltrosIncidencias,
  Incidencia,
  Pagina,
  TipoIncidencia,
} from '../../models/admin.models';
import { IncidenciasService } from '../../services/incidencias.service';

/** Estados disponibles en el filtro de la bandeja. */
const ESTADOS_FILTRO: EstadoIncidencia[] = ['ABIERTA', 'EN_REVISION', 'RESUELTA', 'RECHAZADA'];
/** Tipos disponibles en el filtro de la bandeja. */
const TIPOS_FILTRO: TipoIncidencia[] = [
  'APELACION_SUSPENSION',
  'APELACION_PRODUCTO',
  'REPORTE_PRODUCTO',
  'SOPORTE_GENERAL',
];
/** Roles disponibles en el filtro de la bandeja. */
const ROLES_FILTRO: Rol[] = ['CLIENTE', 'VENDEDOR'];

/**
 * US-14: bandeja de incidencias del administrador con filtros, paginación y
 * acciones de tomar, resolver y rechazar con respuesta obligatoria.
 * @see US-14
 */
@Component({
  selector: 'app-incidencias-admin',
  imports: [Navbar],
  templateUrl: './incidencias-admin.html',
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
    .admin-filtros select {
      width: 100%;
      padding: 0.8rem 1rem;
      border: 1.5px solid var(--ls-border);
      border-radius: var(--ls-radius);
      background: #fffef7;
      color: var(--ls-ink);
      font: inherit;
      font-size: 0.97rem;
    }
    .admin-paginado {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      margin-top: 1.25rem;
      color: var(--ls-muted);
      font-size: 0.9rem;
    }
    .chip-ABIERTA {
      background: var(--ls-accent);
      color: var(--ls-ink);
    }
    .chip-EN_REVISION {
      background: var(--ls-info-soft);
      color: #2563a8;
    }
    .chip-RESUELTA {
      background: var(--ls-success-soft);
      color: var(--ls-primary-dark);
    }
    .chip-RECHAZADA {
      background: var(--ls-error-soft);
      color: var(--ls-error);
    }
    .celda-accion {
      min-width: 150px;
    }
    .acciones-select {
      width: 100%;
      min-width: 145px;
      padding: 0.65rem 2rem 0.65rem 0.8rem;
      border: 1.5px solid var(--ls-border);
      border-radius: 999px;
      background: var(--ls-surface);
      color: var(--ls-primary-dark);
      font: inherit;
      font-size: 0.9rem;
      font-weight: 700;
      cursor: pointer;
      transition: border-color 0.15s, box-shadow 0.15s, background 0.15s;
    }
    .acciones-select:hover:not(:disabled) {
      border-color: var(--ls-primary);
      background: var(--ls-primary-soft);
    }
    .acciones-select:focus-visible {
      outline: none;
      border-color: var(--ls-primary);
      box-shadow: 0 0 0 4px var(--ls-primary-soft);
    }
    .acciones-select:disabled {
      cursor: wait;
      opacity: 0.65;
    }
    .confirm-dialog textarea {
      width: 100%;
      padding: 0.8rem 1rem;
      border: 1.5px solid var(--ls-border);
      border-radius: var(--ls-radius);
      background: #fffef7;
      color: var(--ls-ink);
      font: inherit;
      font-size: 0.97rem;
      resize: vertical;
    }
    .confirm-dialog .field-error {
      margin: 0.4rem 0 0.8rem;
    }
  `,
})
export class IncidenciasAdminPage implements OnInit {
  private readonly incidencias = inject(IncidenciasService);

  protected readonly estados = ESTADOS_FILTRO;
  protected readonly tipos = TIPOS_FILTRO;
  protected readonly roles = ROLES_FILTRO;
  protected readonly etiquetasEstado = ETIQUETAS_ESTADO_INCIDENCIA;
  protected readonly etiquetasTipo = ETIQUETAS_TIPO_INCIDENCIA;
  protected readonly etiquetasRol = ETIQUETAS_ROL;

  protected readonly filtroEstado = signal('');
  protected readonly filtroTipo = signal('');
  protected readonly filtroRol = signal('');
  protected readonly filtroTexto = signal('');

  protected readonly items = signal<Incidencia[]>([]);
  protected readonly pagina = signal(0);
  protected readonly totalPaginas = signal(0);
  protected readonly totalElementos = signal(0);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly mensaje = signal<string | null>(null);

  /** Incidencia en espera de decisión y tipo de decisión pendiente (resolver | rechazar). */
  protected readonly dialogo = signal<Incidencia | null>(null);
  protected readonly decision = signal<'RESOLVER' | 'RECHAZAR'>('RESOLVER');
  protected readonly respuestaTexto = signal('');
  protected readonly errorDialogo = signal<string | null>(null);
  protected readonly enviando = signal(false);
  protected readonly respuestaCampo = viewChild<ElementRef<HTMLTextAreaElement>>('respuestaCampo');

  /**
   * Ejecuta la opción seleccionada en el menú de acciones de una incidencia.
   * @param incidencia Incidencia sobre la que se eligió una acción.
   * @param event Cambio del selector de acciones.
   * @returns No devuelve un valor.
   */
  protected ejecutarAccion(incidencia: Incidencia, event: Event): void {
    const selector = event.currentTarget;
    if (!(selector instanceof HTMLSelectElement)) return;
    const accion = selector.value;
    selector.value = '';

    switch (accion) {
      case 'TOMAR':
        this.tomar(incidencia);
        break;
      case 'RESOLVER':
        this.abrirDialogo(incidencia, 'RESOLVER');
        break;
      case 'RECHAZAR':
        this.abrirDialogo(incidencia, 'RECHAZAR');
        break;
    }
  }

  /**
   * Carga la primera página de la bandeja al iniciar.
   * @returns No devuelve un valor.
   */
  ngOnInit(): void {
    this.cargar();
  }

  /**
   * Consulta la bandeja de incidencias con los filtros vigentes.
   * @returns No devuelve un valor.
   * @see US-14
   */
  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    const filtros: FiltrosIncidencias = {
      estado: (this.filtroEstado() || undefined) as EstadoIncidencia | undefined,
      tipo: (this.filtroTipo() || undefined) as TipoIncidencia | undefined,
      rol: (this.filtroRol() || undefined) as Rol | undefined,
      texto: this.filtroTexto().trim() || undefined,
      pagina: this.pagina(),
      tamano: 10,
    };
    this.incidencias.listar(filtros).subscribe({
      next: (res: Pagina<Incidencia>) => {
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

  /**
   * Aplica los filtros y vuelve a la primera página.
   * @returns No devuelve un valor.
   */
  protected aplicarFiltros(): void {
    this.pagina.set(0);
    this.cargar();
  }

  /**
   * Limpia los filtros y recarga la bandeja.
   * @returns No devuelve un valor.
   */
  protected limpiarFiltros(): void {
    this.filtroEstado.set('');
    this.filtroTipo.set('');
    this.filtroRol.set('');
    this.filtroTexto.set('');
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
   * Toma una incidencia abierta; pasa a revisión y recarga la bandeja.
   * @param incidencia Incidencia abierta que se toma.
   * @returns No devuelve un valor.
   * @see US-14
   */
  protected tomar(incidencia: Incidencia): void {
    this.enviando.set(true);
    this.incidencias.tomar(incidencia.id).subscribe({
      next: () => {
        this.enviando.set(false);
        this.mensaje.set(`La incidencia #${incidencia.id} pasó a revisión.`);
        this.cargar();
      },
      error: (err: unknown) => {
        this.enviando.set(false);
        this.error.set(procesarErrorApi(err));
      },
    });
  }

  /**
   * Abre el diálogo de decisión (resolver o rechazar) y enfoca la respuesta.
   * @param incidencia Incidencia tomada a la que se responde.
   * @param tipoDecision Resolución positiva o rechazo.
   * @returns No devuelve un valor.
   */
  protected abrirDialogo(incidencia: Incidencia, tipoDecision: 'RESOLVER' | 'RECHAZAR'): void {
    this.dialogo.set(incidencia);
    this.decision.set(tipoDecision);
    this.respuestaTexto.set('');
    this.errorDialogo.set(null);
    setTimeout(() => this.respuestaCampo()?.nativeElement.focus());
  }

  /**
   * Cierra el diálogo de decisión sin aplicar cambios.
   * @returns No devuelve un valor.
   */
  protected cerrarDialogo(): void {
    this.dialogo.set(null);
  }

  /**
   * Confirma la decisión con la respuesta obligatoria (mínimo 10 caracteres).
   * @returns No devuelve un valor.
   * @see US-14
   */
  protected confirmarDecision(): void {
    const incidencia = this.dialogo();
    const respuesta = this.respuestaTexto().trim();
    if (!incidencia) return;
    if (respuesta.length < 10) {
      this.errorDialogo.set('Escribe una respuesta de al menos 10 caracteres.');
      return;
    }
    this.enviando.set(true);
    this.errorDialogo.set(null);
    const destino: 'RESOLVER' | 'RECHAZAR' = this.decision();
    const llamada =
      destino === 'RESOLVER'
        ? this.incidencias.resolver(incidencia.id, { respuesta })
        : this.incidencias.rechazar(incidencia.id, { respuesta });
    llamada.subscribe({
      next: () => {
        this.enviando.set(false);
        this.dialogo.set(null);
        this.mensaje.set(
          destino === 'RESOLVER'
            ? `La incidencia #${incidencia.id} se resolvió.`
            : `La incidencia #${incidencia.id} se rechazó.`,
        );
        this.cargar();
      },
      error: (err: unknown) => {
        this.enviando.set(false);
        this.errorDialogo.set(procesarErrorApi(err));
      },
    });
  }
}
