import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-navbar',
  imports: [RouterLink],
  template: `
    <header class="navbar">
      <a class="navbar-brand" [routerLink]="auth.rutaInicio()">Little<strong>Style</strong></a>
      @if (auth.usuario(); as usuario) {
        <div class="navbar-user">
          <div class="account-menu">
            <button
              type="button"
              class="avatar account-menu-trigger"
              aria-label="Abrir menú de cuenta"
              aria-haspopup="menu"
              [attr.aria-expanded]="menuAbierto()"
              (click)="menuAbierto.update((abierto) => !abierto)"
            >
              {{ usuario.nombre.charAt(0).toUpperCase() }}
            </button>
            @if (menuAbierto()) {
              <div class="account-menu-panel" role="menu">
                <a routerLink="/cuenta" role="menuitem" (click)="menuAbierto.set(false)">Mi cuenta</a>
                <button type="button" role="menuitem" (click)="auth.logout()">Cerrar sesión</button>
              </div>
            }
          </div>
        </div>
      }
    </header>
  `,
})
export class Navbar {
  protected readonly auth = inject(AuthService);
  protected readonly menuAbierto = signal(false);
}
