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
        <li>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
            stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M5 3v18M5 5h11l-2 3 2 3H5M5 15h8l-2 3 2 3H5" />
          </svg>
          Recomendación de tallas según sus medidas
        </li>
        <li>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
            stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M20 4c-8 0-14 3-14 10a6 6 0 0 0 6 6c7 0 8-8 8-16Z" />
            <path d="M4 21c3-5 7-8 13-12" />
          </svg>
          Filtro de telas por alergias textiles
        </li>
        <li>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
            stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <rect x="5" y="10" width="14" height="11" rx="2" />
            <path d="M8 10V7a4 4 0 0 1 8 0v3M12 14v3" />
          </svg>
          Pagos seguros con pasarela externa
        </li>
      </ul>
      <div class="brand-bubbles" aria-hidden="true">
        <span></span><span></span><span></span>
      </div>
    </aside>
  `,
})
export class AuthBrand {}
