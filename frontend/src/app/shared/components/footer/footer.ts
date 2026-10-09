import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Pie del e-commerce: contacto, accesos y aviso legal. */
@Component({
  selector: 'app-footer',
  imports: [RouterLink],
  template: `
    <footer class="footer">
      <div class="footer-grid">
        <div>
          <p class="footer-brand">Little<strong>Style</strong></p>
          <p>Ropa infantil con la talla ideal para cada etapa.</p>
        </div>
        <nav aria-label="Accesos del pie de página">
          <p class="footer-titulo">Explora</p>
          <a routerLink="/cliente/catalogo">Catálogo</a>
          <a [routerLink]="['/cliente/catalogo']" [queryParams]="{ genero: 'NINO' }">Niños</a>
          <a [routerLink]="['/cliente/catalogo']" [queryParams]="{ genero: 'NINA' }">Niñas</a>
          <a routerLink="/cliente/hijos">Mis hijos</a>
        </nav>
        <div>
          <p class="footer-titulo">Contacto</p>
          <p>contacto@littlestyle.com</p>
          <p>Lunes a viernes, 8:00 a 18:00</p>
        </div>
      </div>
      <p class="footer-legal">© LittleStyle — Proyecto académico de Ingeniería de Software.</p>
    </footer>
  `,
})
export class Footer {}
