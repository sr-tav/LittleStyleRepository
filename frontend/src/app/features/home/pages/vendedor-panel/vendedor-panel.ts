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
        <article class="home-tile"><span>👕</span><h2>Mis productos</h2><p>Gestiona tu catálogo</p></article>
        <article class="home-tile"><span>📊</span><h2>Inventario</h2><p>Stock y alertas</p></article>
        <article class="home-tile"><span>🧾</span><h2>Pedidos</h2><p>Pedidos recibidos</p></article>
        <article class="home-tile"><span>📈</span><h2>Reportes</h2><p>Ventas y demanda</p></article>
      </div>
    </main>
  `,
})
export class VendedorPanel {
  protected readonly auth = inject(AuthService);
}
