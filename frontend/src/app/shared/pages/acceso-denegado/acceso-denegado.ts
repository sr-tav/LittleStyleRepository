import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';

/**
 * Página 403 para usuarios sin permiso, con retorno al inicio del rol autenticado.
 * @see US-05
 * @see RNF-10
 */
@Component({
  selector: 'app-acceso-denegado',
  imports: [RouterLink],
  template: `
    <main class="status-page">
      <span class="status-code">403</span>
      <h1>Acceso denegado</h1>
      <p>Tu cuenta no tiene permisos para ver esta sección.</p>
      <a class="btn-primary btn-inline" [routerLink]="auth.rutaInicio()">Volver a mi inicio</a>
    </main>
  `,
})
export class AccesoDenegado {
  protected readonly auth = inject(AuthService);
}
