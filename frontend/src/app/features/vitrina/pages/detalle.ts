import { Component, ElementRef, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Navbar } from '../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../auth/auth.validators';
import { PrendaMiniatura } from '../../catalogoCompras/components/prenda-miniatura';
import { CarritoService } from '../../catalogoCompras/services/carrito.service';
import {
  ETIQUETAS_CATEGORIAS,
  ETIQUETAS_GENERO,
  ETIQUETAS_MATERIALES,
  describirComposicion,
  formatearPrecio,
} from '../../catalogoCompras/models/prenda-opciones';
import { ETIQUETAS_COLORES, ETIQUETAS_ESTAMPADOS } from '../../perfilesInfantiles/models/perfil-opciones';
import { PerfilActivoService } from '../../perfilesInfantiles/services/perfil-activo.service';
import { TallaSugerida } from '../../recomendacion/components/talla-sugerida';
import { RecomendacionItem } from '../../recomendacion/models/recomendacion.models';
import { RecomendacionService } from '../../recomendacion/services/recomendacion.service';
import { VitrinaDetalle, VitrinaTalla } from '../models/vitrina.models';
import { VitrinaService } from '../services/vitrina.service';
import { CrearIncidencia } from '../../admin/models/admin.models';
import { IncidenciasService } from '../../admin/services/incidencias.service';

/**
 * US-09: ficha del producto con ficha técnica textil y tabla de medidas.
 * @see US-09
 * @see RNF-11
 */
@Component({
  selector: 'app-detalle-producto',
  imports: [Navbar, RouterLink, PrendaMiniatura, TallaSugerida],
  templateUrl: './detalle.html',
})
export class DetalleProducto implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly vitrina = inject(VitrinaService);
  private readonly recomendaciones = inject(RecomendacionService);
  private readonly activo = inject(PerfilActivoService);
  private readonly carrito = inject(CarritoService);
  private readonly incidencias = inject(IncidenciasService);

  protected readonly prenda = signal<VitrinaDetalle | null>(null);
  protected readonly sugerida = signal<RecomendacionItem | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly fotoActual = signal(0);
  protected readonly tallaSeleccionada = signal('');
  protected readonly cantidadSeleccionada = signal(1);
  protected readonly agregandoAlCarrito = signal(false);
  protected readonly mensajeCarrito = signal<string | null>(null);
  protected readonly errorCarrito = signal<string | null>(null);
  protected readonly reporteAbierto = signal(false);
  protected readonly asuntoReporte = signal('');
  protected readonly descripcionReporte = signal('');
  protected readonly errorReporte = signal<string | null>(null);
  protected readonly reporteEnviando = signal(false);
  protected readonly reporteMensaje = signal<string | null>(null);
  protected readonly descripcionReporteInput = viewChild<ElementRef<HTMLTextAreaElement>>('descripcionReporteCampo');
  protected readonly stockTallaSeleccionada = computed(() => {
    const talla = this.tallaSeleccionada();
    return this.prenda()?.tallas.find((item) => item.talla === talla)?.stockDisponible ?? 0;
  });

  /** Marca "Nuevo" si la prenda se actualizó en los últimos 30 días. */
  protected readonly esNuevo = computed(() => {
    const fecha = this.prenda()?.fechaActualizacion;
    if (!fecha) return false;
    const millis = new Date(fecha).getTime();
    return !Number.isNaN(millis) && Date.now() - millis < 30 * 24 * 60 * 60 * 1000;
  });

  /**
   * Muestra la fotografía anterior en orden circular.
   * @returns No devuelve un valor.
   */
  protected anteriorFoto(): void {
    const total = this.prenda()?.imagenes.length ?? 0;
    if (total > 1) this.fotoActual.update((i) => (i - 1 + total) % total);
  }

  /**
   * Muestra la fotografía siguiente en orden circular.
   * @returns No devuelve un valor.
   */
  protected siguienteFoto(): void {
    const total = this.prenda()?.imagenes.length ?? 0;
    if (total > 1) this.fotoActual.update((i) => (i + 1) % total);
  }

  /**
   * Alterna el modo de pantalla completa del visor cuando el navegador lo permite.
   * @param visor Elemento que se amplía.
   * @returns No devuelve un valor.
   */
  protected ampliar(visor: HTMLElement): void {
    if (document.fullscreenElement) {
      void document.exitFullscreen().catch(() => undefined);
    } else if (visor.requestFullscreen) {
      void visor.requestFullscreen().catch(() => undefined);
    }
  }

  /**
   * Agrega la talla y cantidad seleccionadas si no exceden el stock disponible.
   * @returns No devuelve un valor.
   */
  protected agregarAlCarrito(): void {
    const prenda = this.prenda();
    const talla = this.tallaSeleccionada();
    const cantidad = this.cantidadSeleccionada();
    if (!prenda || !talla || cantidad < 1 || cantidad > this.stockTallaSeleccionada()
      || this.agregandoAlCarrito()) return;

    this.agregandoAlCarrito.set(true);
    this.mensajeCarrito.set(null);
    this.errorCarrito.set(null);
    this.carrito.agregar({
      prendaId: prenda.id,
      talla,
      cantidad,
      tallaSugerida: this.sugerida()?.tallaSugerida || null,
    }).subscribe({
      next: () => {
        this.mensajeCarrito.set('Prenda agregada al carrito.');
        this.carrito.abrirPanel();
      },
      error: (error: unknown) => {
        this.errorCarrito.set(procesarErrorApi(error));
        this.agregandoAlCarrito.set(false);
      },
      complete: () => this.agregandoAlCarrito.set(false),
    });
  }

  protected readonly precioDe = (p: VitrinaDetalle) => formatearPrecio(p.precio);
  protected readonly categoriaDe = (p: VitrinaDetalle) =>
    ETIQUETAS_CATEGORIAS[p.categoria as keyof typeof ETIQUETAS_CATEGORIAS] ?? p.categoria;
  protected readonly generoDe = (p: VitrinaDetalle) =>
    p.genero === null ? 'Unisex' : (ETIQUETAS_GENERO[p.genero as keyof typeof ETIQUETAS_GENERO] ?? p.genero);
  protected readonly composicionDe = (p: VitrinaDetalle) =>
    describirComposicion(
      p.composicion.map((c) => ({
        material: c.material as keyof typeof ETIQUETAS_MATERIALES,
        porcentaje: c.porcentaje,
      })),
    );
  protected readonly colorDe = (c: string) =>
    ETIQUETAS_COLORES[c as keyof typeof ETIQUETAS_COLORES] ?? c;

  /**
   * Abre el formulario de reporte de producto y enfoca la descripción.
   * @returns No devuelve un valor.
   * @see US-14
   */
  protected abrirReporte(): void {
    const prenda = this.prenda();
    if (!prenda) return;
    this.reporteMensaje.set(null);
    this.errorReporte.set(null);
    this.asuntoReporte.set(`Reporte de producto: ${prenda.nombre}`);
    this.descripcionReporte.set('');
    this.reporteAbierto.set(true);
    setTimeout(() => this.descripcionReporteInput()?.nativeElement.focus());
  }

  /**
   * Cierra el formulario de reporte sin registrar una incidencia.
   * @returns No devuelve un valor.
   */
  protected cerrarReporte(): void {
    this.reporteAbierto.set(false);
    this.errorReporte.set(null);
  }

  /**
   * Registra el reporte asociado a la prenda del detalle.
   * @returns No devuelve un valor.
   * @see US-14
   */
  protected enviarReporte(): void {
    const prenda = this.prenda();
    const asunto = this.asuntoReporte().trim();
    const descripcion = this.descripcionReporte().trim();
    if (!prenda || this.reporteEnviando()) return;
    if (asunto.length < 5 || asunto.length > 200 || descripcion.length < 20 || descripcion.length > 4000) {
      this.errorReporte.set('El asunto debe tener entre 5 y 200 caracteres y la descripción entre 20 y 4000.');
      return;
    }
    const reporte: CrearIncidencia = {
      tipo: 'REPORTE_PRODUCTO',
      asunto,
      descripcion,
      referenciaId: prenda.id,
      referenciaDetalle: prenda.nombre,
    };
    this.reporteEnviando.set(true);
    this.errorReporte.set(null);
    this.incidencias.crear(reporte).subscribe({
      next: () => {
        this.reporteEnviando.set(false);
        this.reporteAbierto.set(false);
        this.reporteMensaje.set('Gracias. El reporte quedó registrado para revisión administrativa.');
      },
      error: (err: unknown) => {
        this.reporteEnviando.set(false);
        this.errorReporte.set(procesarErrorApi(err));
      },
    });
  }

  protected readonly edadDe = (t: VitrinaTalla) =>
    t.edadMinMeses == null && t.edadMaxMeses == null
      ? '—'
      : `${t.edadMinMeses ?? '…'} – ${t.edadMaxMeses ?? '…'} m`;  protected readonly estampadoDe = (e: string | null) =>
    e === null ? '—' : (ETIQUETAS_ESTAMPADOS[e as keyof typeof ETIQUETAS_ESTAMPADOS] ?? e);

  /**
   * Carga la ficha del producto y, si se dispone de perfil, su sugerencia de talla.
   * @returns No devuelve un valor.
   * @see US-09
   */
  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    const perfilId =
      Number(this.route.snapshot.queryParamMap.get('perfilId')) ||
      this.activo.perfil()?.id ||
      null;
    this.vitrina.detalle(id).subscribe({
      next: (p) => {
        this.prenda.set(p);
        this.cargando.set(false);
        if (perfilId !== null) this.cargarSugerida(id, perfilId);
      },
      error: (err: unknown) => {
        this.error.set(procesarErrorApi(err));
        this.cargando.set(false);
      },
    });
  }

  /**
   * Actualiza la talla seleccionada y reinicia cantidad y mensajes del carrito.
   * @param event Evento del selector de tallas.
   * @returns No devuelve un valor.
   */
  protected seleccionarTalla(event: Event): void {
    if (event.target instanceof HTMLSelectElement) {
      this.tallaSeleccionada.set(event.target.value);
      this.cantidadSeleccionada.set(1);
      this.mensajeCarrito.set(null);
      this.errorCarrito.set(null);
    }
  }

  /**
   * Ajusta la cantidad al intervalo entre una unidad y el stock de la talla seleccionada.
   * @param delta Cambio de cantidad solicitado.
   * @returns No devuelve un valor.
   */
  protected cambiarCantidad(delta: number): void {
    this.cantidadSeleccionada.update((cantidad) =>
      Math.min(this.stockTallaSeleccionada(), Math.max(1, cantidad + delta)),
    );
  }

  /**
   * Consulta la sugerencia y representa como excluida la respuesta con código de alergia conocido.
   * @param prendaId Identificador del producto.
   * @param perfilId Identificador del perfil infantil.
   * @returns No devuelve un valor.
   * @see RNF-11
   */
  private cargarSugerida(prendaId: number, perfilId: number): void {
    this.recomendaciones.porPrenda(prendaId, perfilId).subscribe({
      next: (r) => this.sugerida.set(r),
      error: (err: unknown) => {
        // El backend responde 422 con código estable: PRENDA_EXCLUIDA_ALERGIA
        // se muestra con riesgo EXCLUIDA; otros 422 (SIN_TALLA_COMPATIBLE,
        // MEDICION_DESACTUALIZADA, PERFIL_SIN_MEDICIONES) no muestran nada.
        // No se decide por texto del mensaje: si el mensaje cambia, se rompería.
        if (err instanceof HttpErrorResponse && err.status === 422) {
          const body = err.error as { codigo?: string; mensaje?: string } | null;
          if (body?.codigo === 'PRENDA_EXCLUIDA_ALERGIA') {
            const mensaje = procesarErrorApi(err);
            const prenda = this.prenda();
            if (prenda) {
              this.sugerida.set({
                prendaId,
                nombre: prenda.nombre,
                precio: prenda.precio,
                tallaSugerida: '',
                puntaje: 0,
                afinidad: 0,
                riesgo: 'EXCLUIDA',
                advertencias: [mensaje],
                tieneStock: false,
                imagenUrl: prenda.imagenes[0] ?? null,
              });
              return;
            }
          }
        }
        this.sugerida.set(null);
      },
    });
  }
}
