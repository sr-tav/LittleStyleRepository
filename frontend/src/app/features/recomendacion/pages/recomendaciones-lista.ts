import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Navbar } from '../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../auth/auth.validators';
import { PerfilActivoService } from '../../perfilesInfantiles/services/perfil-activo.service';
import { PerfilesService } from '../../perfilesInfantiles/services/perfiles.service';
import { TallaSugerida} from '../components/talla-sugerida';
import { RecomendacionItem } from '../models/recomendacion.models';
import { RecomendacionService } from '../services/recomendacion.service';
import { PrendaMiniatura } from '../../catalogoCompras/components/prenda-miniatura';
import { formatearPrecio } from '../../catalogoCompras/models/prenda-opciones';

@Component({
  selector: 'app-recomendaciones-lista',
  imports: [Navbar, RouterLink, TallaSugerida, PrendaMiniatura],
  template: `
  <app-navbar />
  <main class="reco-page">
    <header class="reco-heading">
      <div>
        <p class="reco-eyebrow">Recomendación</p>
        <h1>Talla sugerida</h1>
        <p class="reco-subtitle">Calculada con las medidas de tu hijo, no con la edad</p>
      </div>
      <a class="btn-ghost btn-inline" routerLink="/cliente/hijos" [queryParams]="{ modo: 'seleccionar' }">Cambiar perfil</a>
    </header>

    @if (cargando()) {
      <div class="reco-state" role="status"><p>Cargando recomendaciones…</p></div>
    } @else if (error()) {
      <p class="alert alert-error" role="alert">{{ error() }}</p>
      @if (esMedicionDesactualizada()) {
        <a class="btn-primary btn-inline" [routerLink]="['/cliente/hijos', perfilId()]">Agregar medición</a>
      }
    } @else if (items().length === 0) {
      <div class="reco-state" role="status">
        @if (esSoloSinStock()) {
          <p>Hay prendas en tu talla pero sin stock. Prueba con otra talla.</p>
        } @else {
          <p>No hay prendas compatibles: o el catálogo está vacío o todas fueron excluidas por alergia.</p>
        }
      </div>
    } @else {
      <ul class="reco-grid" aria-label="Prendas recomendadas">
        @for (r of items(); track r.prendaId) {
          <li>
            <article class="reco-card">
              <div class="reco-card-media">
                <app-prenda-miniatura [url]="r.imagenUrl" [nombre]="r.nombre" />
              </div>
              <div class="reco-card-body">
                <h2 class="reco-card-title">{{ r.nombre }}</h2>
                @if (precioDe(r) !== null) {
                  <p class="reco-price">{{ precioDe(r) }}</p>
                }
                <app-talla-sugerida [item]="r" />
                <p class="reco-score">
                  <span>Compatibilidad: <strong>{{ r.puntaje }}%</strong></span>
                  <span class="reco-score-track" aria-hidden="true"><span [style.width.%]="r.puntaje"></span></span>
                </p>
                <div class="reco-card-cta">
                  <a class="btn-primary btn-inline btn-small" [routerLink]="['/cliente/prendas', r.prendaId]" [queryParams]="{ perfilId: perfilId() }" [attr.aria-label]="'Ver detalle de ' + r.nombre">Ver detalle</a>
                </div>
              </div>
            </article>
          </li>
        }
      </ul>
    }
  </main>`,
})
export class RecomendacionesLista implements OnInit {
  private route = inject(ActivatedRoute);
  private recomendaciones = inject(RecomendacionService);
  private perfiles = inject(PerfilesService);
  private activo = inject(PerfilActivoService);

  protected readonly items = signal<RecomendacionItem[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly perfilId = signal<number | null>(null);
  protected readonly esMedicionDesactualizada = signal(false);
  protected readonly esSoloSinStock = signal(false);

  protected precioDe(r: RecomendacionItem): string | null {
    return r.precio === null ? null : formatearPrecio(r.precio);
  }

  ngOnInit(): void {
    const qp = Number(this.route.snapshot.queryParamMap.get('perfilId'));
    const id = Number.isFinite(qp) && qp > 0 ? qp : this.activo.perfil()?.id ?? null;
    if (id === null) {
      this.perfiles.listar().subscribe({
        next: (ps) => {
          this.activo.sincronizar(ps);
          const primero = ps[0]?.id ?? null;
          this.perfilId.set(primero);
          if (primero === null) { this.cargando.set(false); this.error.set('Crea un perfil primero.'); }
          else this.cargar(primero);
        },
        error: (e) => { this.error.set(procesarErrorApi(e)); this.cargando.set(false); },
      });
      return;
    }
    this.perfilId.set(id);
    this.cargar(id);
  }

  private cargar(id: number): void {
    this.recomendaciones.listar(id).subscribe({
      next: (res) => { this.items.set(res); this.cargando.set(false); },
      error: (err: unknown) => {
        const msg = procesarErrorApi(err);
        this.error.set(msg);
        this.esMedicionDesactualizada.set(msg.toLowerCase().includes('desactualizada') || msg.toLowerCase().includes('medici'));
        this.cargando.set(false);
      },
    });
  }
}
