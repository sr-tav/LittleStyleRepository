import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../../../core/services/auth.service';
import { Navbar } from '../../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../../auth/auth.validators';
import { PerfilInfantil } from '../../../perfilesInfantiles/models/perfil-infantil.models';
import { tallaProvisional } from '../../../perfilesInfantiles/models/talla-provisional';
import { PerfilActivoService } from '../../../perfilesInfantiles/services/perfil-activo.service';
import { PerfilesService } from '../../../perfilesInfantiles/services/perfiles.service';

@Component({
  selector: 'app-cliente-inicio',
  imports: [Navbar, RouterLink],
  template: `
    <app-navbar />
    <main class="home">
      <header class="home-welcome">
        <p class="home-eyebrow">LittleStyle</p>
        <h1>Hola, {{ auth.usuario()?.nombre }}</h1>
        <p class="home-sub">Bienvenido de vuelta a LittleStyle</p>
      </header>

      @if (cargando()) {
        <section class="active-profile active-profile-skeleton" role="status">
          <span class="home-skeleton-line"></span>
          <span class="home-skeleton-line"></span>
          <span class="home-skeleton-line"></span>
        </section>
      } @else if (error()) {
        <p class="alert alert-error" role="alert">{{ error() }}</p>
      } @else if (perfilActivo(); as perfil) {
        <section class="active-profile">
          <div class="active-profile-copy">
            <p class="home-eyebrow">Perfil activo</p>
            <h2>{{ perfil.nombre }}</h2>
            <p>
              {{ edad(perfil) }} ·
              {{
                perfil.ultimaMedicion
                  ? perfil.ultimaMedicion.estaturaCm + ' cm'
                  : 'Estatura sin registrar'
              }}
            </p>
          </div>
          <a
            class="btn-primary btn-inline"
            routerLink="/cliente/hijos"
            [queryParams]="{ modo: 'seleccionar' }"
            >Cambiar perfil</a
          >
        </section>
      } @else {
        <section class="active-profile active-profile-empty">
          <div>
            <p class="home-eyebrow">Perfil activo</p>
            <h2>Empieza creando su perfil</h2>
            <p>Registra el perfil de tu hijo para organizar sus medidas y preferencias.</p>
          </div>
          <a class="btn-primary btn-inline" routerLink="/cliente/hijos/nuevo"
            >Crear primer perfil</a
          >
        </section>
      }

      <nav class="home-grid" aria-label="Accesos rápidos">
        <a class="home-tile" routerLink="/cliente/hijos">
          <span class="home-tile-icon home-tile-icon-children">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
              stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
            </svg>
          </span>
          <span class="home-tile-label">Mis hijos</span>
        </a>
        <article class="home-tile home-tile-disabled" aria-disabled="true" title="Próximamente">
          <span class="home-tile-icon home-tile-icon-catalog">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
              stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M6 7h12l1 14H5L6 7Z" />
              <path d="M9 7a3 3 0 0 1 6 0" />
            </svg>
          </span>
          <span class="home-tile-label">Catálogo</span>
        </article>
        <article class="home-tile home-tile-disabled" aria-disabled="true" title="Próximamente">
          <span class="home-tile-icon home-tile-icon-orders">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
              stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="m3 7 9-4 9 4-9 4-9-4Z" />
              <path d="M3 7v10l9 4 9-4V7M12 11v10" />
            </svg>
          </span>
          <span class="home-tile-label">Mis pedidos</span>
        </article>
        <article class="home-tile home-tile-disabled" aria-disabled="true" title="Próximamente">
          <span class="home-tile-icon home-tile-icon-cart">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
              stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M3 3h2l2.4 12.2a2 2 0 0 0 2 1.6h8.9a2 2 0 0 0 2-1.6L22 8H6" />
              <circle cx="10" cy="21" r="1" />
              <circle cx="19" cy="21" r="1" />
            </svg>
          </span>
          <span class="home-tile-label">Carrito</span>
        </article>
      </nav>
    </main>
  `,
})
export class ClienteInicio implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly perfilesService = inject(PerfilesService);
  private readonly perfilActivoService = inject(PerfilActivoService);

  protected readonly perfilActivo = this.perfilActivoService.perfil;
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);

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
  }

  /**
   * @deprecated Provisional de US-06
   * @param perfil
   * @protected
   */
  protected talla(perfil: PerfilInfantil): number {
    return tallaProvisional(perfil.edadAnios);
  }

  protected edad(perfil: PerfilInfantil): string {
    if (perfil.edadAnios === 0) {
      return `${perfil.mesesRestantes} ${perfil.mesesRestantes === 1 ? 'mes' : 'meses'}`;
    }
    return `${perfil.edadAnios} ${perfil.edadAnios === 1 ? 'año' : 'años'}`;
  }
}
