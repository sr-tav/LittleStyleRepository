import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, switchMap } from 'rxjs';

import { Navbar } from '../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../auth/auth.validators';
import { AccesoRapidoPrenda } from '../../catalogoCompras/components/acceso-rapido-prenda';
import { PrendaMiniatura } from '../../catalogoCompras/components/prenda-miniatura';
import {
  OPCIONES_CATEGORIAS,
  OPCIONES_GENERO,
  OPCIONES_MATERIALES,
  formatearPrecio,
} from '../../catalogoCompras/models/prenda-opciones';
import { PerfilActivoService } from '../../perfilesInfantiles/services/perfil-activo.service';
import { RecomendacionItem } from '../../recomendacion/models/recomendacion.models';
import { RecomendacionService } from '../../recomendacion/services/recomendacion.service';
import { Pagina, VitrinaItem, FiltrosVitrina } from '../models/vitrina.models';
import { VitrinaService } from '../services/vitrina.service';

/** Filtro por talla con existencias. */
const TALLAS = ['2', '4', '6', '8', '10', '12', '14', '16'];
const ORDENES = [
  { valor: 'novedades', etiqueta: 'Novedades' },
  { valor: 'precioAsc', etiqueta: 'Menor precio' },
  { valor: 'precioDesc', etiqueta: 'Mayor precio' },
  { valor: 'nombre', etiqueta: 'Nombre' },
];

/** US-09: exploración del catálogo con filtros laterales y paginado. */
@Component({
  selector: 'app-explorar',
  imports: [Navbar, RouterLink, PrendaMiniatura, AccesoRapidoPrenda],
  templateUrl: './explorar.html',
})
export class Explorar {
  private readonly vitrina = inject(VitrinaService);
  private readonly recomendaciones = inject(RecomendacionService);
  private readonly activo = inject(PerfilActivoService);
  private readonly ruta = inject(ActivatedRoute);

  protected readonly categorias = OPCIONES_CATEGORIAS;
  protected readonly generos = OPCIONES_GENERO;
  protected readonly materiales = OPCIONES_MATERIALES;
  protected readonly tallas = TALLAS;
  protected readonly ordenes = ORDENES;

  protected readonly q = signal('');
  protected readonly categoria = signal('');
  protected readonly material = signal('');
  protected readonly precioMin = signal('');
  protected readonly precioMax = signal('');
  protected readonly talla = signal('');
  protected readonly edadAnos = signal('');
  protected readonly disponible = signal(false);
  protected readonly orden = signal('novedades');
  protected readonly pagina = signal(0);
  protected readonly soloMiTalla = signal(false);

  protected readonly items = signal<VitrinaItem[]>([]);
  protected readonly genero = signal('');
  protected readonly totalElementos = signal(0);
  protected readonly totalPaginas = signal(0);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly idsMiTalla = signal<number[] | null>(null);
  protected readonly cargandoMiTalla = signal(false);
  /** Talla sugerida y advertencias por prenda (mejor esfuerzo, tope 24 del motor). */
  protected readonly sugerencias = signal<Map<number, RecomendacionItem>>(new Map());

  protected readonly visibles = computed(() => this.items());
  protected readonly precio = (item: VitrinaItem) => formatearPrecio(item.precio);

  constructor() {
    // switchMap: si el usuario cambia filtros antes de que responda la
    // anterior, la petición vieja se cancela y nunca pinta resultados viejos.
    // (Se suscribe primero: el Subject no repite emisiones anteriores.)
    this.recargar
      .pipe(
        switchMap(() => this.vitrina.explorar(this.filtrosActuales())),
        takeUntilDestroyed(),
      )
      .subscribe({
        next: (res: Pagina<VitrinaItem>) => {
          this.items.set(res.contenido);
          this.totalElementos.set(res.totalElementos);
          this.totalPaginas.set(res.totalPaginas);
          this.cargando.set(false);
        },
        error: (err: unknown) => {
          this.error.set(procesarErrorApi(err));
          this.cargando.set(false);
        },
      });
    // Los enlaces del menú cambian los query params sin recrear la página:
    // cada cambio precarga los filtros y vuelve a buscar.
    this.ruta.queryParamMap.subscribe((params) => {
      const categoria = params.get('categoria') ?? '';
      const genero = params.get('genero') ?? '';
      const cambio = categoria !== this.categoria() || genero !== this.genero();
      this.categoria.set(categoria);
      this.genero.set(genero);
      if (cambio || this.items().length === 0) this.buscarDesdePrimera();
    });
    // La búsqueda por texto espera 300 ms; el resto de filtros buscan de inmediato
    toObservable(this.q)
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(() => this.buscarDesdePrimera());
  }

  protected alCambiarFiltro(): void {
    this.buscarDesdePrimera();
  }

  protected limpiar(): void {
    this.q.set('');
    this.categoria.set('');
    this.genero.set('');
    this.material.set('');
    this.precioMin.set('');
    this.precioMax.set('');
    this.talla.set('');
    this.edadAnos.set('');
    this.disponible.set(false);
    this.orden.set('novedades');
    this.soloMiTalla.set(false);
    this.idsMiTalla.set(null);
    this.sugerencias.set(new Map());
    this.buscarDesdePrimera();
  }

  protected anterior(): void {
    if (this.pagina() > 0) {
      this.pagina.update((p) => p - 1);
      this.buscar();
    }
  }

  protected siguiente(): void {
    if (this.pagina() < this.totalPaginas() - 1) {
      this.pagina.update((p) => p + 1);
      this.buscar();
    }
  }

  protected alternarMiTalla(): void {
    if (this.soloMiTalla()) {
      this.soloMiTalla.set(false);
      this.idsMiTalla.set(null);
      this.sugerencias.set(new Map());
      this.buscarDesdePrimera();
      return;
    }
    const perfilId = this.activo.perfil()?.id ?? null;
    if (perfilId === null) {
      this.error.set('Para filtrar por tu talla, selecciona primero un perfil.');
      return;
    }
    this.cargandoMiTalla.set(true);
    // Filtro de servidor: los ids recomendados viajan como parámetro y la
    // paginación y el contador los calcula la base de datos (antes se filtraba
    // solo la página actual de 12 sobre un máximo de 24, con falsos negativos).
    this.recomendaciones.ids(perfilId).subscribe({
      next: (res) => {
        this.error.set(null);
        this.idsMiTalla.set(res);
        this.soloMiTalla.set(true);
        this.cargandoMiTalla.set(false);
        this.buscarDesdePrimera();
        this.cargarBadges(perfilId);
      },
      error: (err: unknown) => {
        this.cargandoMiTalla.set(false);
        this.error.set(
          err instanceof HttpErrorResponse && err.status === 422
            ? 'Tu perfil no tiene medidas vigentes. Agrega una medición para usar este filtro.'
            : procesarErrorApi(err),
        );
      },
    });
  }

  private buscarDesdePrimera(): void {
    this.pagina.set(0);
    this.buscar();
  }

  private buscar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.recargar.next();
  }

  /** Badges por tarjeta (mejor esfuerzo): el filtro exacto ya lo da /ids. */
  private cargarBadges(perfilId: number): void {
    this.recomendaciones.listar(perfilId).subscribe({
      next: (res) => this.sugerencias.set(new Map(res.map((r) => [r.prendaId, r]))),
      error: () => this.sugerencias.set(new Map()),
    });
  }

  private readonly recargar = new Subject<void>();

  private filtrosActuales(): FiltrosVitrina {
    const edad = this.edadAnos().trim() === '' ? NaN : Number(this.edadAnos());
    return {
      q: this.q() || undefined,
      categoria: this.categoria() || undefined,
      genero: this.genero() || undefined,
      precioMin: this.precioMin() === '' ? undefined : Number(this.precioMin()),
      precioMax: this.precioMax() === '' ? undefined : Number(this.precioMax()),
      disponible: this.disponible() || undefined,
      material: this.material() || undefined,
      talla: this.talla() || undefined,
      edadMeses: Number.isNaN(edad) ? undefined : Math.max(0, Math.round(edad * 12)),
      ids: this.soloMiTalla() && this.idsMiTalla() ? this.idsMiTalla()!.join(',') : undefined,
      pagina: this.pagina(),
      tamano: 12,
      orden: this.orden(),
    };
  }
}
