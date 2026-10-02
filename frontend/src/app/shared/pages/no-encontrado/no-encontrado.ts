import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-no-encontrado',
  imports: [RouterLink],
  template: `
    <main class="status-page">
      <span class="status-code">404</span>
      <h1>Página no encontrada</h1>
      <p>La dirección que buscas no existe.</p>
      <a class="btn-primary btn-inline" [routerLink]="auth.rutaInicio()">Ir al inicio</a>
    </main>
  `,
})
export class NoEncontrado {
  protected readonly auth = inject(AuthService);
}
