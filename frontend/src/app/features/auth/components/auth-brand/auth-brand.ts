import { Component } from '@angular/core';

/** Panel lateral con la marca LittleStyle, compartido por las vistas de inicio de sesión y registro. */
@Component({
  selector: 'app-auth-brand',
  template: `
    <aside class="auth-brand" aria-label="LittleStyle">
      <div class="brand-logo">
        <span class="brand-mark" aria-hidden="true">
          <svg viewBox="0 0 48 48">
            <path d="M17 8l-9 6 4 7 4-2v21h16V19l4 2 4-7-9-6c-1 3-4 5-7 5s-6-2-7-5z" />
          </svg>
        </span>
        <span class="brand-name">Little<strong>Style</strong></span>
      </div>
      <p class="brand-tagline">
        La plataforma de moda infantil que crece con tus hijos: tallas recomendadas,
        prendas seguras y compras sin complicaciones.
      </p>
      <ul class="brand-features">
        <li><span aria-hidden="true">📏</span> Recomendación de tallas según sus medidas</li>
        <li><span aria-hidden="true">🌿</span> Filtro de telas por alergias textiles</li>
        <li><span aria-hidden="true">🔒</span> Pagos seguros con pasarela externa</li>
      </ul>
      <div class="brand-bubbles" aria-hidden="true">
        <span></span><span></span><span></span>
      </div>
    </aside>
  `,
})
export class AuthBrand {}
