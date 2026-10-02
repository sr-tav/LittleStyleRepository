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
        <article class="home-tile"><span>👥</span><h2>Usuarios</h2><p>Clientes y vendedores</p></article>
        <article class="home-tile"><span>🛡️</span><h2>Moderación</h2><p>Publicaciones del catálogo</p></article>
        <article class="home-tile"><span>🏷️</span><h2>Categorías</h2><p>Categorías generales</p></article>
        <article class="home-tile"><span>📑</span><h2>Reportes</h2><p>Dashboards y exportación PDF</p></article>
      </div>
    </main>
  `,
})
export class AdminPanel {}
