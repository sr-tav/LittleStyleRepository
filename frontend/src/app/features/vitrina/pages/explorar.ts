import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { Navbar } from '../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../auth/auth.validators';
import { PrendaMiniatura } from '../../catalogoCompras/components/prenda-miniatura';
import {
  OPCIONES_CATEGORIAS,
  OPCIONES_GENERO,
  OPCIONES_MATERIALES,
  formatearPrecio,
} from '../../catalogoCompras/models/prenda-opciones';
import { PerfilActivoService } from '../../perfilesInfantiles/services/perfil-activo.service';
import { RecomendacionService } from '../../recomendacion/services/recomendacion.service';
import { Pagina, VitrinaItem } from '../models/vitrina.models';
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
  imports: [Navbar, RouterLink, PrendaMiniatura],
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
  protected readonly idsMiTalla = signal<Set<number> | null>(null);

  protected readonly visibles = computed(() => {
    const ids = this.idsMiTalla();
    if (!this.soloMiTalla() || ids === null) return this.items();
    return this.items().filter((item) => ids.has(item.id));
  });
  protected readonly precio = (item: VitrinaItem) => formatearPrecio(item.precio);

  constructor() {
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
    this.disponible.set(false);
    this.orden.set('novedades');
    this.soloMiTalla.set(false);
    this.idsMiTalla.set(null);
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
      return;
    }
    const perfilId = this.activo.perfil()?.id ?? null;
    if (perfilId === null) {
      this.error.set('Para filtrar por tu talla, selecciona primero un perfil.');
      return;
    }
    this.recomendaciones.listar(perfilId).subscribe({
      next: (res) => {
        this.idsMiTalla.set(new Set(res.map((r) => r.prendaId)));
        this.soloMiTalla.set(true);
      },
      error: (err: unknown) => this.error.set(procesarErrorApi(err)),
    });
  }

  private buscarDesdePrimera(): void {
    this.pagina.set(0);
    this.buscar();
  }

  private buscar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.vitrina
      .explorar({
        q: this.q() || undefined,
        categoria: this.categoria() || undefined,
        genero: this.genero() || undefined,
        precioMin: this.precioMin() === '' ? undefined : Number(this.precioMin()),
        precioMax: this.precioMax() === '' ? undefined : Number(this.precioMax()),
        disponible: this.disponible() || undefined,
        material: this.material() || undefined,
        talla: this.talla() || undefined,
        pagina: this.pagina(),
        tamano: 12,
        orden: this.orden(),
      })
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
  }
}
