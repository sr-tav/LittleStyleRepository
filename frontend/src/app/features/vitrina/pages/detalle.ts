import { Component, OnInit, computed, inject, signal } from '@angular/core';
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
import { VitrinaDetalle } from '../models/vitrina.models';
import { VitrinaService } from '../services/vitrina.service';

/** US-09: ficha del producto con ficha técnica textil y tabla de medidas. */
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

  protected readonly prenda = signal<VitrinaDetalle | null>(null);
  protected readonly sugerida = signal<RecomendacionItem | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly fotoActual = signal(0);
  protected readonly tallaSeleccionada = signal('');
  protected readonly agregandoAlCarrito = signal(false);
  protected readonly mensajeCarrito = signal<string | null>(null);
  protected readonly errorCarrito = signal<string | null>(null);

  /** Marca "Nuevo" si la prenda se actualizó en los últimos 30 días. */
  protected readonly esNuevo = computed(() => {
    const fecha = this.prenda()?.fechaActualizacion;
    if (!fecha) return false;
    const millis = new Date(fecha).getTime();
    return !Number.isNaN(millis) && Date.now() - millis < 30 * 24 * 60 * 60 * 1000;
  });

  protected anteriorFoto(): void {
    const total = this.prenda()?.imagenes.length ?? 0;
    if (total > 1) this.fotoActual.update((i) => (i - 1 + total) % total);
  }

  protected siguienteFoto(): void {
    const total = this.prenda()?.imagenes.length ?? 0;
    if (total > 1) this.fotoActual.update((i) => (i + 1) % total);
  }

  protected ampliar(visor: HTMLElement): void {
    if (document.fullscreenElement) {
      void document.exitFullscreen().catch(() => undefined);
    } else if (visor.requestFullscreen) {
      void visor.requestFullscreen().catch(() => undefined);
    }
  }

  protected agregarAlCarrito(): void {
    const prenda = this.prenda();
    const talla = this.tallaSeleccionada();
    if (!prenda || !talla || this.agregandoAlCarrito()) return;

    this.agregandoAlCarrito.set(true);
    this.mensajeCarrito.set(null);
    this.errorCarrito.set(null);
    this.carrito.agregar({ prendaId: prenda.id, talla, cantidad: 1 }).subscribe({
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
  protected readonly estampadoDe = (e: string | null) =>
    e === null ? '—' : (ETIQUETAS_ESTAMPADOS[e as keyof typeof ETIQUETAS_ESTAMPADOS] ?? e);

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

  protected seleccionarTalla(event: Event): void {
    if (event.target instanceof HTMLSelectElement) {
      this.tallaSeleccionada.set(event.target.value);
    }
  }

  private cargarSugerida(prendaId: number, perfilId: number): void {
    this.recomendaciones.porPrenda(prendaId, perfilId).subscribe({
      next: (r) => this.sugerida.set(r),
      error: () => this.sugerida.set(null),
    });
  }
}
