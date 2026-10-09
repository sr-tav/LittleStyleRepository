import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { OPCIONES_CATEGORIAS } from '../../../features/catalogoCompras/models/prenda-opciones';

/**
 * Barra superior del e-commerce: menú hamburguesa a la izquierda, logo
 * centrado, carrito y cuenta a la derecha. El carrito queda deshabilitado
 * hasta US-10.
 */
@Component({
  selector: 'app-navbar',
  imports: [RouterLink],
  template: `
    <header class="navbar">
      <div class="navbar-left">
        <button
          type="button"
          class="icon-button"
          aria-label="Abrir menú"
          [attr.aria-expanded]="menuAbierto()"
          (click)="menuAbierto.update((abierto) => !abierto)"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
            stroke-linecap="round" aria-hidden="true">
            <path d="M4 7h16M4 12h16M4 17h16" />
          </svg>
        </button>
      </div>

      <a class="navbar-brand navbar-brand-centro" [routerLink]="auth.rutaInicio()">Little<strong>Style</strong></a>

      <div class="navbar-right">
        @if (auth.rol() === 'CLIENTE') {
          <button
            type="button" class="icon-button" disabled
            title="Próximamente" aria-label="Carrito de compras (próximamente)"
          >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
            stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M3 3h2l2.4 12.2a2 2 0 0 0 2 1.6h8.9a2 2 0 0 0 2-1.6L22 8H6" />
            <circle cx="10" cy="21" r="1" />
            <circle cx="19" cy="21" r="1" />
          </svg>
          </button>
        }
        @if (auth.usuario(); as usuario) {
          <div class="navbar-user">
            <div class="account-menu">
              <button
                type="button"
                class="avatar account-menu-trigger"
                aria-label="Abrir menú de cuenta"
                aria-haspopup="menu"
                [attr.aria-expanded]="cuentaAbierta()"
                (click)="cuentaAbierta.update((abierto) => !abierto)"
              >
                {{ usuario.nombre.charAt(0).toUpperCase() }}
              </button>
              @if (cuentaAbierta()) {
                <div class="account-menu-panel" role="menu">
                  <a routerLink="/cuenta" role="menuitem" (click)="cuentaAbierta.set(false)">Mi cuenta</a>
                  @if (auth.rol() === 'CLIENTE') {
                    <a routerLink="/cliente/hijos" role="menuitem" (click)="cuentaAbierta.set(false)">Mis hijos</a>
                  }
                  <button type="button" role="menuitem" (click)="auth.logout()">Cerrar sesión</button>
                </div>
              }
            </div>
          </div>
        }
      </div>
    </header>

    @if (menuAbierto()) {
      <div class="menu-backdrop" (click)="cerrarMenu()" aria-hidden="true"></div>
      <nav class="menu-lateral" aria-label="Menú principal" (keydown.escape)="cerrarMenu()">
        <div class="menu-lateral-head">
          <strong>LittleStyle</strong>
          <button type="button" class="icon-button" aria-label="Cerrar menú" (click)="cerrarMenu()">✕</button>
        </div>
        @if (auth.rol() === 'VENDEDOR') {
          <a routerLink="/vendedor/productos" (click)="cerrarMenu()">Mis productos</a>
          <a routerLink="/vendedor/inventario" (click)="cerrarMenu()">Inventario</a>
          <a routerLink="/vendedor/inventario/alertas" (click)="cerrarMenu()">Alertas</a>
        } @else if (auth.rol() === 'ADMINISTRADOR') {
          <a routerLink="/admin" (click)="cerrarMenu()">Panel administrativo</a>
        } @else {
          <div class="menu-grupo" [class.is-abierto]="grupoAbierto() === 'catalogo'">
            <div class="menu-grupo-head">
              <a routerLink="/cliente/catalogo" (click)="cerrarMenu()">Catálogo</a>
              <button
                type="button" class="menu-desplegar"
                aria-label="Desplegar opciones del catálogo"
                [attr.aria-expanded]="grupoAbierto() === 'catalogo'"
                (click)="alternarGrupo('catalogo')"
              >▾</button>
            </div>
            <div class="menu-submenu">
              <a [routerLink]="['/cliente/catalogo']" [queryParams]="{ genero: 'NINO' }" (click)="cerrarMenu()">Niños</a>
              <a [routerLink]="['/cliente/catalogo']" [queryParams]="{ genero: 'NINA' }" (click)="cerrarMenu()">Niñas</a>
            </div>
          </div>
          <div class="menu-grupo" [class.is-abierto]="grupoAbierto() === 'categorias'">
            <div class="menu-grupo-head">
              <span class="menu-lateral-titulo">Categorías</span>
              <button
                type="button" class="menu-desplegar"
                aria-label="Desplegar categorías"
                [attr.aria-expanded]="grupoAbierto() === 'categorias'"
                (click)="alternarGrupo('categorias')"
              >▾</button>
            </div>
            <div class="menu-submenu">
              @for (c of categorias; track c.valor) {
                <a
                  [routerLink]="['/cliente/catalogo']" [queryParams]="{ categoria: c.valor }"
                  (click)="cerrarMenu()"
                >{{ c.etiqueta }}</a>
              }
            </div>
          </div>
        }
      </nav>
    }
  `,
})
export class Navbar {
  protected readonly auth = inject(AuthService);
  protected readonly menuAbierto = signal(false);
  protected readonly cuentaAbierta = signal(false);
  protected readonly categorias = OPCIONES_CATEGORIAS;
  protected readonly grupoAbierto = signal<string | null>(null);

  protected alternarGrupo(grupo: string): void {
    this.grupoAbierto.update((actual) => (actual === grupo ? null : grupo));
  }

  protected cerrarMenu(): void {
    this.menuAbierto.set(false);
    this.grupoAbierto.set(null);
  }
}
