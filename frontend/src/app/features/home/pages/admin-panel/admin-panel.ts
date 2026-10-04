import { Component } from '@angular/core';

import { Navbar } from '../../../../shared/components/navbar/navbar';

/** UI-25: Panel inicial del administrador (solo rol ADMINISTRADOR). Contenido real pendiente del Proceso 3. */
@Component({
  selector: 'app-admin-panel',
  imports: [Navbar],
  template: `
    <app-navbar />
    <main class="home">
      <h1>Panel administrativo</h1>
      <p class="home-sub">Supervisión general de la plataforma</p>
      <div class="home-grid">
        <article class="home-tile">
          <span aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="9" cy="8" r="3" /><circle cx="18" cy="9" r="2.5" /><path d="M3 20v-1a6 6 0 0 1 12 0v1M15 15a4 4 0 0 1 6 3v2" /></svg></span>
          <h2>Usuarios</h2><p>Clientes y vendedores</p>
        </article>
        <article class="home-tile">
          <span aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11Z" /><path d="m9 12 2 2 4-4" /></svg></span>
          <h2>Moderación</h2><p>Publicaciones del catálogo</p>
        </article>
        <article class="home-tile">
          <span aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="m20 13-7 7-11-11V2h7l11 11Z" /><circle cx="6.5" cy="6.5" r="1" /></svg></span>
          <h2>Categorías</h2><p>Categorías generales</p>
        </article>
        <article class="home-tile">
          <span aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M6 3h9l4 4v14H6zM14 3v5h5M9 13h7M9 17h7" /></svg></span>
          <h2>Reportes</h2><p>Dashboards y exportación PDF</p>
        </article>
      </div>
    </main>
  `,
})
export class AdminPanel {}
