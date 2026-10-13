import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../../../core/services/auth.service';
import { Footer } from '../../../../shared/components/footer/footer';
import { Navbar } from '../../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../../auth/auth.validators';
import { AccesoRapidoPrenda } from '../../../catalogoCompras/components/acceso-rapido-prenda';
import { PrendaMiniatura } from '../../../catalogoCompras/components/prenda-miniatura';
import { formatearPrecio } from '../../../catalogoCompras/models/prenda-opciones';
import { CarritoService } from '../../../catalogoCompras/services/carrito.service';
import { PerfilInfantil } from '../../../perfilesInfantiles/models/perfil-infantil.models';
import { PerfilActivoService } from '../../../perfilesInfantiles/services/perfil-activo.service';
import { PerfilesService } from '../../../perfilesInfantiles/services/perfiles.service';
import { Pagina, VitrinaItem } from '../../../vitrina/models/vitrina.models';
import { VitrinaService } from '../../../vitrina/services/vitrina.service';

const TAMANO_NOVEDADES = 8;

/** Inicio del cliente con estilo e-commerce: carrusel, novedades y accesos. */
@Component({
  selector: 'app-cliente-inicio',
  imports: [Navbar, RouterLink, PrendaMiniatura, AccesoRapidoPrenda, Footer],
  templateUrl: './cliente-inicio.html',
})
export class ClienteInicio implements OnInit, OnDestroy {
  protected readonly auth = inject(AuthService);
  protected readonly carrito = inject(CarritoService);
  private readonly perfilesService = inject(PerfilesService);
  private readonly perfilActivoService = inject(PerfilActivoService);
  private readonly vitrina = inject(VitrinaService);

  protected readonly perfilActivo = this.perfilActivoService.perfil;
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly destacadas = signal<VitrinaItem[]>([]);
  protected readonly slide = signal(0);
  protected readonly novedades = signal<VitrinaItem[]>([]);
  protected readonly paginaNovedades = signal(0);
  protected readonly totalPaginasNovedades = signal(0);
  protected readonly cargandoMas = signal(false);

  protected readonly precio = (item: VitrinaItem) => formatearPrecio(item.precio);

  private intervalo: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.perfilesService.listar().subscribe({
      next: (perfiles) => {
        this.perfilActivoService.sincronizar(perfiles);
        this.cargando.set(false);
      },
      error: (err: unknown) => {
        this.error.set(procesarErrorApi(err));
        this.cargando.set(false);
      },
    });
    this.cargarHero();
    this.cargarNovedades();
    this.intervalo = setInterval(() => this.siguienteSlide(), 6000);
  }

  ngOnDestroy(): void {
    if (this.intervalo !== null) clearInterval(this.intervalo);
  }

  protected anteriorSlide(): void {
    const total = this.destacadas().length;
    if (total > 1) this.slide.update((i) => (i - 1 + total) % total);
  }

  protected siguienteSlide(): void {
    const total = this.destacadas().length;
    if (total > 1) this.slide.update((i) => (i + 1) % total);
  }

  protected cargarMas(): void {
    if (this.cargandoMas() || this.paginaNovedades() >= this.totalPaginasNovedades() - 1) return;
    this.cargandoMas.set(true);
    const siguiente = this.paginaNovedades() + 1;
    this.vitrina.explorar({ pagina: siguiente, tamano: TAMANO_NOVEDADES, orden: 'novedades' }).subscribe({
      next: (res: Pagina<VitrinaItem>) => {
        this.novedades.update((actuales) => [...actuales, ...res.contenido]);
        this.paginaNovedades.set(res.pagina);
        this.totalPaginasNovedades.set(res.totalPaginas);
        this.cargandoMas.set(false);
      },
      error: () => this.cargandoMas.set(false),
    });
  }

  private cargarHero(): void {
    this.vitrina.explorar({ pagina: 0, tamano: 10, orden: 'novedades' }).subscribe({
      next: (res: Pagina<VitrinaItem>) => {
        const conFoto = res.contenido.filter((item) => item.imagenUrl);
        this.destacadas.set(conFoto.slice(0, 5));
      },
      error: () => this.destacadas.set([]),
    });
  }

  private cargarNovedades(): void {
    this.vitrina.explorar({ pagina: 0, tamano: TAMANO_NOVEDADES, orden: 'novedades' }).subscribe({
      next: (res: Pagina<VitrinaItem>) => {
        this.novedades.set(res.contenido);
        this.paginaNovedades.set(res.pagina);
        this.totalPaginasNovedades.set(res.totalPaginas);
      },
      error: () => this.novedades.set([]),
    });
  }

  protected edad(perfil: PerfilInfantil): string {
    if (perfil.edadAnios === 0) {
      return `${perfil.mesesRestantes} ${perfil.mesesRestantes === 1 ? 'mes' : 'meses'}`;
    }
    return `${perfil.edadAnios} ${perfil.edadAnios === 1 ? 'año' : 'años'}`;
  }
}
