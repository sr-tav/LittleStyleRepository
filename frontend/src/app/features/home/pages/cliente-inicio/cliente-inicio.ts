import { Component, inject } from '@angular/core';

import { AuthService } from '../../../../core/services/auth.service';
import { Navbar } from '../../../../shared/components/navbar/navbar';

/** UI-3: Pantalla principal de clientes (solo rol CLIENTE). Contenido real pendiente del Proceso 1/2. */
@Component({
  selector: 'app-cliente-inicio',
  imports: [Navbar],
  template: `
    <app-navbar />
    <main class="home">
      <h1>Hola, {{ auth.usuario()?.nombre }} 👋</h1>
      <p class="home-sub">¿Qué quieres hacer hoy?</p>
      <div class="home-grid">
        <article class="home-tile"><span>👶</span><h2>Mis hijos</h2><p>Perfiles infantiles y medidas</p></article>
        <article class="home-tile"><span>👗</span><h2>Catálogo</h2><p>Prendas recomendadas por talla</p></article>
        <article class="home-tile"><span>🛒</span><h2>Mi carrito</h2><p>Revisa y finaliza tu compra</p></article>
        <article class="home-tile"><span>📦</span><h2>Mis pedidos</h2><p>Seguimiento de tus envíos</p></article>
      </div>
    </main>
  `,
})
export class ClienteInicio {
  protected readonly auth = inject(AuthService);
}
