import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { AuthService } from '../../../../core/services/auth.service';
import { Navbar } from '../../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../../auth/auth.validators';
import {
  CrearIncidencia,
  ETIQUETAS_ESTADO_INCIDENCIA,
  ETIQUETAS_TIPO_INCIDENCIA,
  EstadoIncidencia,
  Incidencia,
  Pagina,
  TipoIncidencia,
} from '../../models/admin.models';
import { IncidenciasService } from '../../services/incidencias.service';

/** Tipos de incidencia que puede reportar el usuario. */
const TIPOS_INCIDENCIA: TipoIncidencia[] = [
  'APELACION_PRODUCTO',
  'REPORTE_PRODUCTO',
  'SOPORTE_GENERAL',
];

/**
 * US-14: bandeja propia de incidencias del cliente o vendedor, con la lista de
 * sus reportes (estado y respuesta del administrador) y el formulario de creación.
 * @see US-14
 */
@Component({
  selector: 'app-mis-incidencias',
  imports: [Navbar],
  templateUrl: './mis-incidencias.html',
  styles: `
    .mis-incidencias-form {
      margin-top: 1.5rem;
      max-width: 560px;
    }
    .mis-incidencias-form .field textarea,
    .mis-incidencias-form .field select {
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
    .incidencia-respuesta {
      display: block;
      margin-top: 0.35rem;
      padding: 0.6rem 0.75rem;
      border-radius: 10px;
      background: var(--ls-primary-soft);
      color: var(--ls-primary-dark);
      font-size: 0.85rem;
    }
    .chip-ABIERTA {
      background: var(--ls-accent);
      color: var(--ls-ink);
    }
    .chip-EN_REVISION {
      background: var(--ls-info-soft);
      color: #2563a8;
    }
    .chip-RECHAZADA {
      background: var(--ls-error-soft);
      color: var(--ls-error);
    }
    .admin-paginado {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      margin-top: 1.25rem;
      color: var(--ls-muted);
      font-size: 0.9rem;
    }
  `,
})
export class MisIncidencias implements OnInit {
  private readonly incidencias = inject(IncidenciasService);
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);

  protected readonly tipos = this.auth.rol() === 'VENDEDOR'
    ? TIPOS_INCIDENCIA
    : TIPOS_INCIDENCIA.filter((tipo) => tipo !== 'APELACION_PRODUCTO');
  protected readonly etiquetasTipo = ETIQUETAS_TIPO_INCIDENCIA;
  protected readonly etiquetasEstado = ETIQUETAS_ESTADO_INCIDENCIA;

  protected readonly items = signal<Incidencia[]>([]);
  protected readonly pagina = signal(0);
  protected readonly totalPaginas = signal(0);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly mensaje = signal<string | null>(null);

  protected readonly tipoFormulario = signal<TipoIncidencia>('SOPORTE_GENERAL');
  protected readonly asunto = signal('');
  protected readonly descripcion = signal('');
  protected readonly referenciaId = signal('');
  protected readonly erroresFormulario = signal<string[]>([]);

  /**
   * Carga la primera página del listado propio al iniciar.
   * @returns No devuelve un valor.
   */
  ngOnInit(): void {
    const tipo = this.route.snapshot.queryParamMap.get('tipo');
    const referenciaId = Number(this.route.snapshot.queryParamMap.get('referenciaId'));
    if (tipo === 'APELACION_PRODUCTO' && Number.isInteger(referenciaId) && referenciaId > 0) {
      this.tipoFormulario.set('APELACION_PRODUCTO');
      this.referenciaId.set(String(referenciaId));
      this.asunto.set(`Apelación de la prenda #${referenciaId}`);
    }
    this.cargar();
  }

  /**
   * Consulta las incidencias del usuario autenticado.
   * @returns No devuelve un valor.
   * @see US-14
   */
  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.incidencias.listarPropias(this.pagina(), 10).subscribe({
      next: (res: Pagina<Incidencia>) => {
        this.items.set(res.contenido);
        this.pagina.set(res.pagina);
        this.totalPaginas.set(res.totalPaginas);
        this.cargando.set(false);
      },
      error: (err: unknown) => {
        this.error.set(procesarErrorApi(err));
        this.cargando.set(false);
      },
    });
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
   * Indica el estado de la incidencia para la etiqueta visible.
   * @param incidencia Incidencia evaluada.
   * @returns Etiqueta en español del estado.
   */
  protected estadoDe(incidencia: Incidencia): string {
    return this.etiquetasEstado[incidencia.estado as EstadoIncidencia];
  }

  /**
   * Valida el formulario client-side y crea la incidencia si los datos son válidos;
   * al éxito recarga la lista y limpia el formulario.
   * @returns No devuelve un valor.
   * @see US-14
   */
  protected enviar(): void {
    const asunto = this.asunto().trim();
    const descripcion = this.descripcion().trim();
    const errores: string[] = [];
    if (asunto.length < 5 || asunto.length > 200) {
      errores.push('El asunto debe tener entre 5 y 200 caracteres.');
    }
    if (descripcion.length < 20 || descripcion.length > 4000) {
      errores.push('La descripción debe tener entre 20 y 4000 caracteres.');
    }
    const requiereReferencia = ['APELACION_PRODUCTO', 'REPORTE_PRODUCTO'].includes(this.tipoFormulario());
    const referencia = Number(this.referenciaId());
    if (requiereReferencia && (!Number.isInteger(referencia) || referencia < 1)) {
      errores.push('Debes indicar un id de prenda válido.');
    }
    this.erroresFormulario.set(errores);
    if (errores.length > 0) {
      return;
    }
    const cuerpo: CrearIncidencia = {
      tipo: this.tipoFormulario(),
      asunto,
      descripcion,
    };
    if (requiereReferencia) {
      cuerpo.referenciaId = referencia;
    }
    this.incidencias.crear(cuerpo).subscribe({
      next: () => {
        this.mensaje.set('Tu incidencia fue registrada. El equipo la revisará pronto.');
        this.limpiarFormulario();
        this.pagina.set(0);
        this.cargar();
      },
      error: (err: unknown) => {
        this.erroresFormulario.set([procesarErrorApi(err)]);
      },
    });
  }

  /**
   * Restablece los controles del formulario de creación.
   * @returns No devuelve un valor.
   */
  protected limpiarFormulario(): void {
    this.tipoFormulario.set('SOPORTE_GENERAL');
    this.asunto.set('');
    this.descripcion.set('');
    this.referenciaId.set('');
    this.erroresFormulario.set([]);
  }
}
