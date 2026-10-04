import { Component, inject } from '@angular/core';

import { AuthService } from '../../../../core/services/auth.service';
import { Navbar } from '../../../../shared/components/navbar/navbar';

/** UI-18: Panel del vendedor (solo rol VENDEDOR). Contenido real pendiente del Proceso 2. */
@Component({
  selector: 'app-vendedor-panel',
  imports: [Navbar],
  template: `
    <app-navbar />
    <main class="home">
      <h1>Panel del vendedor</h1>
      <p class="home-sub">{{ auth.usuario()?.nombreTienda }}</p>
      <div class="home-grid">
        <article class="home-tile">
          <span aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M16 3 20 8l-2 2v11H6V10L4 8l4-5 4 3 4-3Z" /><path d="M8 3c0 2 1.5 3 4 3s4-1 4-3" /></svg></span>
          <h2>Mis productos</h2><p>Gestiona tu catálogo</p>
        </article>
        <article class="home-tile">
          <span aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M3 7 12 3l9 4-9 4-9-4ZM3 7v10l9 4 9-4V7M12 11v10" /></svg></span>
          <h2>Inventario</h2><p>Stock y alertas</p>
        </article>
        <article class="home-tile">
          <span aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M5 3h14v18H5zM8 7h8M8 11h8M8 15h5" /></svg></span>
          <h2>Pedidos</h2><p>Pedidos recibidos</p>
        </article>
        <article class="home-tile">
          <span aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 20V4M4 20h17M7 16l4-5 3 2 6-8M16 5h4v4" /></svg></span>
          <h2>Reportes</h2><p>Ventas y demanda</p>
        </article>
      </div>
    </main>
  `,
})
export class VendedorPanel {
  protected readonly auth = inject(AuthService);
}
