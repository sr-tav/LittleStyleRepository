import { Component, ElementRef, OnInit, inject, signal, viewChild } from '@angular/core';

import { Navbar } from '../../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../../auth/auth.validators';
import {
  ETIQUETAS_ESTADO_PRENDA,
  ETIQUETAS_MODERACION,
  EstadoModeracion,
  EstadoPrenda,
  FiltrosModeracion,
  Pagina,
  PrendaAdmin,
  PrendaAdminDetalle,
} from '../../models/admin.models';
import { ModeracionService } from '../../services/moderacion.service';
import { formatearPrecio } from '../../../catalogoCompras/models/prenda-opciones';

/** Estados de moderación que pueden mostrarse en el filtro. */
const ESTADOS_MODERACION: EstadoModeracion[] = ['SIN_REVISAR', 'APROBADA', 'DESACTIVADA'];

/**
 * US-14: cola de moderación de prendas con filtros, detalle completo y acciones
 * de aprobación, desactivación con motivo y restauración.
 * @see US-14
 */
@Component({
  selector: 'app-moderacion',
  imports: [Navbar],
  templateUrl: './moderacion.html',
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
    .detalle-prenda {
      margin-top: 1.5rem;
      padding: 1.5rem;
    }
    .detalle-cabecera {
      display: flex;
      flex-wrap: wrap;
      align-items: flex-start;
      justify-content: space-between;
      gap: 1rem;
      margin-bottom: 1.25rem;
      padding-bottom: 1rem;
      border-bottom: 1px solid var(--ls-border);
    }
    .detalle-cabecera h2 {
      font-size: 1.5rem;
      font-weight: 800;
      line-height: 1.2;
    }
    .detalle-vendedor {
      margin-top: 0.35rem;
      color: var(--ls-muted);
      font-size: 0.95rem;
    }
    .detalle-vendedor strong {
      color: var(--ls-ink);
      font-weight: 700;
    }
    .detalle-motivo {
      margin-top: 0.5rem;
      color: var(--ls-error);
      font-size: 0.9rem;
    }
    .detalle-cabecera-lado {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .detalle-grid {
      display: grid;
      grid-template-columns: minmax(240px, 340px) 1fr;
      gap: 1.5rem 2rem;
      align-items: start;
    }
    @media (max-width: 800px) { .detalle-grid { grid-template-columns: 1fr; } }

    .detalle-foto {
      display: block;
      width: 100%;
      aspect-ratio: 4 / 5;
      object-fit: cover;
      border: 1px solid var(--ls-border);
      border-radius: 14px;
      background: #f4f5ee;
    }
    .detalle-foto-vacia {
      display: grid;
      place-items: center;
      color: var(--ls-muted);
      font-size: 0.9rem;
    }
    .detalle-miniaturas {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
      margin-top: 0.6rem;
    }
    .miniatura {
      width: 4rem;
      height: 4rem;
      padding: 0;
      overflow: hidden;
      cursor: pointer;
      border: 2px solid transparent;
      border-radius: 10px;
      background: none;
    }
    .miniatura img {
      display: block;
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    .miniatura.activa {
      border-color: var(--ls-primary-dark);
    }

    .detalle-info {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
    }
    .detalle-bloque h3 {
      margin-bottom: 0.6rem;
      font-size: 1rem;
      font-weight: 800;
    }
    .detalle-bloque p {
      font-size: 0.95rem;
      line-height: 1.55;
    }

    .detalle-datos {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
      gap: 0.9rem 1.25rem;
      margin: 0;
    }
    .detalle-datos dt {
      margin-bottom: 0.15rem;
      color: var(--ls-muted);
      font-size: 0.75rem;
      font-weight: 800;
      letter-spacing: 0.06em;
      text-transform: uppercase;
    }
    .detalle-datos dd {
      margin: 0;
      font-size: 1rem;
      font-weight: 600;
      overflow-wrap: anywhere;
    }
    .detalle-datos dd.dato-precio {
      font-size: 1.2rem;
      font-weight: 800;
    }

    .detalle-flags {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
      gap: 0.5rem;
      margin: 0;
      padding: 0;
      list-style: none;
    }
    .detalle-flags li {
      display: flex;
      align-items: center; justify-content: space-between;
      padding: 0.6rem 0.85rem;
      border: 1px solid var(--ls-border);
      border-radius: 10px;
      font-size: 0.9rem;
    }
    .detalle-flags li.flag-alerta {
      border-color: var(--ls-warning);
      background: var(--ls-warning-soft);
      color: var(--ls-warning);
    }

    .detalle-composicion {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
      margin: 0;
      padding: 0;
      list-style: none; }
    .detalle-composicion li {
      padding: 0.3rem 0.8rem;
      border-radius: 999px;
      background: var(--ls-primary-soft);
      color: var(--ls-primary-dark);
      font-size: 0.9rem;
      font-weight: 700;
    }

    .detalle-tabla { width: 100%;
      border-collapse: collapse;
      font-size: 0.92rem; }
    .detalle-tabla th {
      padding: 0.5rem 0.75rem;
      border-bottom: 1px solid var(--ls-border);
      color: var(--ls-muted);
      font-size: 0.75rem;
      font-weight: 800;
      letter-spacing: 0.06em;
      text-align: left;
      text-transform: uppercase;
    }
    .detalle-tabla td {
      padding: 0.6rem 0.75rem;
      border-bottom: 1px solid var(--ls-border);
    }

    .detalle-acciones {
      display: flex;
      flex-wrap: wrap;
      justify-content: flex-end;
      gap: 0.75rem;
      margin-top: 1.5rem;
      padding-top: 1.25rem;
      border-top: 1px solid var(--ls-border);
    }
    .detalle-acciones .accion-desactivar {
      border-color: var(--ls-error);
      color: var(--ls-error);
    }
    .chip-SIN-REVISAR {
      background: var(--ls-accent);
      color: var(--ls-ink);
    }
    .chip-DESACTIVADA {
      background: var(--ls-error-soft);
      color: var(--ls-error);
    }
  `,
})
export class ModeracionPage implements OnInit {
  private readonly moderacion = inject(ModeracionService);

  protected readonly estadosModeracion = ESTADOS_MODERACION;
  protected readonly etiquetasModeracion = ETIQUETAS_MODERACION;
  protected readonly etiquetasPrenda = ETIQUETAS_ESTADO_PRENDA;
  protected readonly precio = formatearPrecio;

  protected readonly filtroModeracion = signal('');
  protected readonly filtroPrenda = signal('');
  protected readonly filtroTexto = signal('');
  protected readonly filtroVendedor = signal('');

  protected readonly items = signal<PrendaAdmin[]>([]);
  protected readonly pagina = signal(0);
  protected readonly totalPaginas = signal(0);
  protected readonly totalElementos = signal(0);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly mensaje = signal<string | null>(null);

  /** Prenda seleccionada para el panel de detalle. */
  protected readonly seleccionada = signal<PrendaAdminDetalle | null>(null);
  protected readonly detalleCargando = signal(false);
  protected readonly desactivando = signal(false);
  protected readonly motivoDesactivacion = signal('');
  protected readonly errorDesactivacion = signal<string | null>(null);
  protected readonly envioPendiente = signal(false);
  protected readonly imagenActiva = signal(0);
  protected readonly imagenFallo = signal(false);
  protected readonly motivoCampo = viewChild<ElementRef<HTMLTextAreaElement>>('motivoDesactivacionCampo');

  /**
   * Carga la primera página de la cola al iniciar.
   * @returns No devuelve un valor.
   */
  ngOnInit(): void {
    this.cargar();
  }

  /**
   * Consulta la cola de moderación con los filtros vigentes.
   * @returns No devuelve un valor.
   * @see US-14
   */
  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    const filtros: FiltrosModeracion = {
      estadoModeracion: (this.filtroModeracion() || undefined) as EstadoModeracion | undefined,
      estadoPrenda: (this.filtroPrenda() || undefined) as EstadoPrenda | undefined,
      texto: this.filtroTexto().trim() || undefined,
      vendedorEmail: this.filtroVendedor().trim() || undefined,
      pagina: this.pagina(),
      tamano: 10,
    };
    this.moderacion.listar(filtros).subscribe({
      next: (res: Pagina<PrendaAdmin>) => {
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
   * Aplica los filtros y vuelve a la primera página de la cola.
   * @returns No devuelve un valor.
   */
  protected aplicarFiltros(): void {
    this.pagina.set(0);
    this.cargar();
  }

  /**
   * Limpia los filtros y recarga la cola.
   * @returns No devuelve un valor.
   */
  protected limpiarFiltros(): void {
    this.filtroModeracion.set('');
    this.filtroPrenda.set('');
    this.filtroTexto.set('');
    this.filtroVendedor.set('');
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
   * Carga el detalle completo de una prenda en el panel lateral.
   * @param prenda Fila seleccionada de la cola.
   * @returns No devuelve un valor.
   */
  protected verDetalle(prenda: PrendaAdmin): void {
    this.detalleCargando.set(true);
    this.seleccionada.set(null);
    this.imagenActiva.set(0);
    this.imagenFallo.set(false);
    this.moderacion.detalle(prenda.id).subscribe({
      next: (detalle) => {
        this.seleccionada.set(detalle);
        this.detalleCargando.set(false);
      },
      error: (err: unknown) => {
        this.error.set(procesarErrorApi(err));
        this.detalleCargando.set(false);
      },
    });
  }

  /**
   * Cierra el panel de detalle y sus errores pendientes.
   * @returns No devuelve un valor.
   */
  protected cerrarDetalle(): void {
    this.seleccionada.set(null);
    this.motivoDesactivacion.set('');
    this.errorDesactivacion.set(null);
  }

  /**
   * Aprueba la prenda seleccionada.
   * @returns No devuelve un valor.
   * @see US-14
   */
  protected aprobar(): void {
    const prenda = this.seleccionada();
    if (!prenda) return;
    this.envioPendiente.set(true);
    this.moderacion.aprobar(prenda.id).subscribe({
      next: () => this.trasDecision(`La prenda «${prenda.nombre}» fue aprobada.`),
      error: (err: unknown) => {
        this.envioPendiente.set(false);
        this.error.set(procesarErrorApi(err));
      },
    });
  }

  /**
   * Habilita el campo del motivo de desactivación y lo enfoca.
   * @returns No devuelve un valor.
   */
  protected abrirDesactivacion(): void {
    this.desactivando.set(true);
    this.motivoDesactivacion.set('');
    this.errorDesactivacion.set(null);
    setTimeout(() => this.motivoCampo()?.nativeElement.focus());
  }

  /**
   * Desactiva la prenda con el motivo obligatorio (mínimo 10 caracteres).
   * @returns No devuelve un valor.
   * @see US-14
   */
  protected confirmarDesactivacion(): void {
    const prenda = this.seleccionada();
    const motivo = this.motivoDesactivacion().trim();
    if (!prenda) return;
    if (motivo.length < 10) {
      this.errorDesactivacion.set('Escribe un motivo de al menos 10 caracteres.');
      return;
    }
    this.envioPendiente.set(true);
    this.moderacion.desactivar(prenda.id, motivo).subscribe({
      next: () => this.trasDecision(`La prenda «${prenda.nombre}» fue desactivada.`),
      error: (err: unknown) => {
        this.envioPendiente.set(false);
        this.errorDesactivacion.set(procesarErrorApi(err));
      },
    });
  }

  /**
   * Cancela la desactivación pendiente.
   * @returns No devuelve un valor.
   */
  protected cancelarDesactivacion(): void {
    this.desactivando.set(false);
    this.motivoDesactivacion.set('');
    this.errorDesactivacion.set(null);
  }

  /**
   * Restaura una prenda desactivada para que vuelva a ser visible.
   * @returns No devuelve un valor.
   * @see US-14
   */
  protected restaurar(): void {
    const prenda = this.seleccionada();
    if (!prenda) return;
    this.envioPendiente.set(true);
    this.moderacion.restaurar(prenda.id).subscribe({
      next: () => this.trasDecision(`La prenda «${prenda.nombre}» volvió a edición.`),
      error: (err: unknown) => {
        this.envioPendiente.set(false);
        this.error.set(procesarErrorApi(err));
      },
    });
  }

  /**
   * Recarga la cola, cierra el detalle y muestra el aviso tras una decisión.
   * @param aviso Mensaje de retroalimentación visible.
   * @returns No devuelve un valor.
   */
  private trasDecision(aviso: string): void {
    this.envioPendiente.set(false);
    this.desactivando.set(false);
    this.motivoDesactivacion.set('');
    this.errorDesactivacion.set(null);
    this.seleccionada.set(null);
    this.mensaje.set(aviso);
    this.cargar();
  }
}
