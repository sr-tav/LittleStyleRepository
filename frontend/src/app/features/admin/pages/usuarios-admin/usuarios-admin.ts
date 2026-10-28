import { Component, ElementRef, OnInit, computed, inject, signal, viewChild } from '@angular/core';

import { Navbar } from '../../../../shared/components/navbar/navbar';
import { Rol } from '../../../../core/models/auth.models';
import { procesarErrorApi } from '../../../auth/auth.validators';
import {
  ETIQUETAS_ESTADO_USUARIO,
  ETIQUETAS_ROL,
  EstadoUsuario,
  FiltrosUsuarios,
  Pagina,
  UsuarioAdmin,
} from '../../models/admin.models';
import { UsuariosAdminService } from '../../services/usuarios-admin.service';

/** Estado destino del cambio: suspender, bloquear o habilitar la cuenta. */
type AccionEstado = 'SUSPENDIDO' | 'BLOQUEADO' | 'ACTIVO';

/** Acciones válidas según el estado actual de la cuenta, con su etiqueta visible. */
const ACCIONES_POR_ESTADO: Record<Exclude<EstadoUsuario, 'ELIMINADO'>, AccionEstado[]> = {
  ACTIVO: ['SUSPENDIDO', 'BLOQUEADO'],
  SUSPENDIDO: ['ACTIVO', 'BLOQUEADO'],
  BLOQUEADO: ['ACTIVO'],
};

/** Etiqueta visible de cada acción de cambio de estado. */
const ETIQUETAS_ACCION: Record<AccionEstado, string> = {
  SUSPENDIDO: 'Suspender',
  BLOQUEADO: 'Bloquear',
  ACTIVO: 'Habilitar',
};

/**
 * US-14: gestión de cuentas de usuario con filtros, paginación y cambio de
 * estado (suspender / bloquear / habilitar) mediante diálogo con motivo.
 * @see US-14
 */
@Component({
  selector: 'app-usuarios-admin',
  imports: [Navbar],
  templateUrl: './usuarios-admin.html',
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
    .admin-filtros select,
    .confirm-dialog textarea,
    .confirm-dialog select {
      width: 100%;
      padding: 0.8rem 1rem;
      border: 1.5px solid var(--ls-border);
      border-radius: var(--ls-radius);
      background: #fffef7;
      color: var(--ls-ink);
      font: inherit;
      font-size: 0.97rem;
    }
    .btn-accion {
      background: var(--ls-primary-soft);
      border-color: var(--ls-primary-dark);
    }
    .confirm-dialog [role='group'] {
      display: flex;
      flex-wrap: wrap;
      gap: 0.6rem;
      margin-bottom: 1.1rem;
    }
    .admin-paginado {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      margin-top: 1.25rem;
      color: var(--ls-muted);
      font-size: 0.9rem;
    }
    .chip-SUSPENDIDO {
      background: var(--ls-accent);
      color: var(--ls-ink);
    }
    .chip-BLOQUEADO {
      background: var(--ls-error-soft);
      color: var(--ls-error);
    }
    .chip-ELIMINADO {
      background: #eef0ea;
      color: var(--ls-muted);
    }
  `,
})
export class UsuariosAdminPage implements OnInit {
  private readonly usuarios = inject(UsuariosAdminService);

  protected readonly roles: Rol[] = ['CLIENTE', 'VENDEDOR', 'ADMINISTRADOR'];
  protected readonly estados: EstadoUsuario[] = [
    'ACTIVO',
    'SUSPENDIDO',
    'BLOQUEADO',
    'ELIMINADO',
  ];
  protected readonly etiquetasRol = ETIQUETAS_ROL;
  protected readonly etiquetasEstado = ETIQUETAS_ESTADO_USUARIO;
  protected readonly etiquetasAccion = ETIQUETAS_ACCION;

  protected readonly filtroRol = signal('');
  protected readonly filtroEstado = signal('');
  protected readonly filtroTexto = signal('');

  protected readonly items = signal<UsuarioAdmin[]>([]);
  protected readonly pagina = signal(0);
  protected readonly totalPaginas = signal(0);
  protected readonly totalElementos = signal(0);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly mensaje = signal<string | null>(null);

  /** Usuario seleccionado para el diálogo de cambio de estado; null lo cierra. */
  protected readonly dialogo = signal<UsuarioAdmin | null>(null);
  protected readonly accionDialogo = signal<AccionEstado | null>(null);
  protected readonly motivoDialogo = signal('');
  protected readonly errorDialogo = signal<string | null>(null);
  protected readonly enviando = signal(false);
  protected readonly motivoInput = viewChild<ElementRef<HTMLTextAreaElement>>('motivoCampo');

  /** Acciones visibles para el estado actual del usuario del diálogo. */
  protected readonly accionesDisponibles = computed<AccionEstado[]>(() => {
    const usuario = this.dialogo();
    if (!usuario || usuario.rol === 'ADMINISTRADOR' || usuario.estado === 'ELIMINADO') {
      return [];
    }
    const acciones = ACCIONES_POR_ESTADO[usuario.estado as Exclude<EstadoUsuario, 'ELIMINADO'>];
    return acciones ?? [];
  });

  /**
   * Carga la primera página al iniciar la página.
   * @returns No devuelve un valor.
   */
  ngOnInit(): void {
    this.cargar();
  }

  /**
   * Indica si la cuenta admite acciones de cambio de estado.
   * @param usuario Cuenta evaluada.
   * @returns `false` para administradores y cuentas eliminadas.
   */
  protected admiteAcciones(usuario: UsuarioAdmin): boolean {
    return usuario.rol !== 'ADMINISTRADOR' && usuario.estado !== 'ELIMINADO';
  }

  /**
   * Consulta el listado con los filtros vigentes y la página actual.
   * @returns No devuelve un valor.
   * @see US-14
   */
  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    const filtros: FiltrosUsuarios = {
      rol: (this.filtroRol() || undefined) as Rol | undefined,
      estado: (this.filtroEstado() || undefined) as EstadoUsuario | undefined,
      texto: this.filtroTexto().trim() || undefined,
      pagina: this.pagina(),
      tamano: 10,
    };
    this.usuarios.listar(filtros).subscribe({
      next: (res: Pagina<UsuarioAdmin>) => {
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
   * Limpia los filtros y recarga el listado completo.
   * @returns No devuelve un valor.
   */
  protected limpiarFiltros(): void {
    this.filtroRol.set('');
    this.filtroEstado.set('');
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
   * Abre el diálogo de cambio de estado y enfoca el campo del motivo.
   * @param usuario Cuenta que se va a modificar.
   * @returns No devuelve un valor.
   */
  protected abrirDialogo(usuario: UsuarioAdmin): void {
    this.dialogo.set(usuario);
    this.accionDialogo.set(this.accionesDisponibles()[0] ?? null);
    this.motivoDialogo.set('');
    this.errorDialogo.set(null);
    setTimeout(() => this.motivoInput()?.nativeElement.focus());
  }

  /**
   * Cierra el diálogo sin aplicar cambios.
   * @returns No devuelve un valor.
   */
  protected cerrarDialogo(): void {
    this.dialogo.set(null);
  }

  /**
   * Confirma el cambio de estado con el motivo obligatorio; recarga al éxito.
   * @returns No devuelve un valor.
   * @see US-14
   */
  protected confirmarCambio(): void {
    const usuario = this.dialogo();
    const accion = this.accionDialogo();
    const motivo = this.motivoDialogo().trim();
    if (!usuario || !accion) return;
    if (motivo.length < 10) {
      this.errorDialogo.set('Escribe un motivo de al menos 10 caracteres.');
      return;
    }
    this.enviando.set(true);
    this.errorDialogo.set(null);
    this.usuarios
      .cambiarEstado(usuario.id, { estado: accion, motivo })
      .subscribe({
        next: () => {
          this.enviando.set(false);
          this.dialogo.set(null);
          this.mensaje.set(`El estado de ${usuario.nombre} se actualizó correctamente.`);
          this.cargar();
        },
        error: (err: unknown) => {
          this.enviando.set(false);
          this.errorDialogo.set(procesarErrorApi(err));
        },
      });
  }
}
