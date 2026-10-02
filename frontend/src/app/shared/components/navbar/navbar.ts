import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Rol } from '../../../core/models/auth.models';
import { AuthService } from '../../../core/services/auth.service';

const ETIQUETA_ROL: Record<Rol, string> = {
  CLIENTE: 'Cliente',
  VENDEDOR: 'Vendedor',
  ADMINISTRADOR: 'Administrador',
};

@Component({
  selector: 'app-navbar',
  imports: [RouterLink],
  template: `
    <header class="navbar">
      <a class="navbar-brand" [routerLink]="auth.rutaInicio()">Little<strong>Style</strong></a>
      @if (auth.usuario(); as usuario) {
        <div class="navbar-user">
          <span class="avatar" aria-hidden="true">{{ usuario.nombre.charAt(0) }}</span>
          <span class="navbar-name">{{ usuario.nombre }} {{ usuario.apellido }}</span>
          <span class="role-badge" [attr.data-rol]="usuario.rol">{{ etiqueta() }}</span>
          <button type="button" class="btn-ghost" (click)="auth.logout()">Cerrar sesión</button>
        </div>
      }
    </header>
  `,
})
export class Navbar {
  protected readonly auth = inject(AuthService);
  protected readonly etiqueta = computed(() => {
    const rol = this.auth.rol();
    return rol ? ETIQUETA_ROL[rol] : '';
  });
}
