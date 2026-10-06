import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Navbar } from '../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../auth/auth.validators';
import { PerfilActivoService } from '../../perfilesInfantiles/services/perfil-activo.service';
import { PerfilesService } from '../../perfilesInfantiles/services/perfiles.service';
import { TallaSugerida} from '../components/talla-sugerida';
import { RecomendacionItem } from '../models/recomendacion.models';
import { RecomendacionService } from '../services/recomendacion.service';

@Component({
  selector: 'app-recomendaciones-lista',
  imports: [Navbar, RouterLink, TallaSugerida],
  template: `
  <app-navbar />
  <main class="profiles-page">
    <header class="profiles-heading">
      <div>
        <p class="profiles-eyebrow">Recomendación</p>
        <h1>Talla sugerida</h1>
        <p class="profiles-subtitle">Calculada con medidas reales, no con edad</p>
      </div>
      <a class="btn-ghost btn-inline" routerLink="/cliente/hijos" [queryParams]="{ modo: 'seleccionar' }">Cambiar perfil</a>
    </header>

    @if (cargando()) {
      <p role="status">Cargando recomendaciones…</p>
    } @else if (error()) {
      <p class="alert alert-error" role="alert">{{ error() }}</p>
      @if (esMedicionDesactualizada()) {
        <a class="btn-primary btn-inline" [routerLink]="['/cliente/hijos', perfilId()]">Agregar medición</a>
      }
    } @else if (items().length === 0) {
      <p role="status">No hay prendas compatibles con este perfil.</p>
    } @else {
      <section class="profiles-grid" aria-label="Prendas recomendadas">
        @for (r of items(); track r.prendaId) {
          <article class="profile-card">
            <h2>{{ r.nombre }}</h2>
            <app-talla-sugerida [item]="r" />
            <p>Puntaje: {{ r.puntaje }}</p>
          </article>
        }
      </section>
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

  ngOnInit(): void {
    const qp = Number(this.route.snapshot.queryParamMap.get('perfilId'));
    const id = Number.isFinite(qp) && qp > 0 ? qp : this.activo.perfil()?.id ?? null;
    if (id === null) {
      this.perfiles.listar().subscribe({
        next: (ps) => {
          this.activo.sincronizar(ps);
          const primero = ps[0]?.id ?? null;
          this.perfilId.set(primero);
          if (primero === null) { this.cargando.set(false); this.error.set('Creá un perfil primero.'); }
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
