import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { CarritoService } from '../../../features/catalogoCompras/services/carrito.service';
import { ItemCarrito } from '../../../features/catalogoCompras/models/carrito.models';
import { procesarErrorApi } from '../../../features/auth/auth.validators';
import {
  formatearPrecio,
  OPCIONES_CATEGORIAS,
} from '../../../features/catalogoCompras/models/prenda-opciones';

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
            type="button" class="icon-button navbar-carrito-trigger"
            [attr.aria-label]="carrito.panelAbierto() ? 'Cerrar carrito de compras' : 'Abrir carrito de compras'"
            [attr.aria-expanded]="carrito.panelAbierto()"
            (click)="carrito.alternarPanel()"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
              stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M3 3h2l2.4 12.2a2 2 0 0 0 2 1.6h8.9a2 2 0 0 0 2-1.6L22 8H6" />
              <circle cx="10" cy="21" r="1" />
              <circle cx="19" cy="21" r="1" />
            </svg>
            @if (carrito.cantidad() > 0) {
              <span class="navbar-carrito-badge" aria-hidden="true">{{ carrito.cantidad() }}</span>
            }
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

    @if (auth.rol() === 'CLIENTE' && carrito.panelAbierto()) {
      <button
        type="button" class="carrito-panel-backdrop"
        aria-label="Cerrar vista rápida del carrito"
        (click)="carrito.cerrarPanel()"
      ></button>
      <aside
        class="carrito-panel"
        role="dialog"
        aria-modal="true"
        aria-label="Carrito de compras"
        (keydown.escape)="carrito.cerrarPanel()"
      >
        <header class="carrito-panel-header">
          <h2>Carrito</h2>
          <button
            type="button" class="icon-button"
            aria-label="Cerrar carrito"
            (click)="carrito.cerrarPanel()"
          >×</button>
        </header>
        @if (carrito.items().length === 0) {
          <div class="carrito-panel-vacio">
            <p>Tu carrito está vacío.</p>
            <a class="btn-ghost btn-inline" routerLink="/cliente/catalogo" (click)="carrito.cerrarPanel()">
              Explorar catálogo
            </a>
          </div>
        } @else {
          <ul class="carrito-panel-items">
            @for (item of carrito.items(); track item.id) {
              <li>
                @if (item.imagenUrl) {
                  <img [src]="item.imagenUrl" [alt]="item.nombre" />
                } @else {
                  <span class="carrito-panel-imagen-vacia" aria-hidden="true">LS</span>
                }
                <div>
                  <h3>{{ item.nombre }}</h3>
                  <p>Talla {{ item.talla }}</p>
                  <div class="carrito-panel-item-controles">
                    <div class="carrito-cantidad">
                      <button
                        type="button"
                        [attr.aria-label]="'Disminuir cantidad de ' + item.nombre"
                        [disabled]="cambioPendiente() === item.id || item.cantidad <= 1"
                        (click)="cambiarCantidad(item, item.cantidad - 1)"
                      >−</button>
                      <span aria-live="polite">{{ item.cantidad }}</span>
                      <button
                        type="button"
                        [attr.aria-label]="'Aumentar cantidad de ' + item.nombre"
                        [disabled]="cambioPendiente() === item.id || !item.disponible"
                        (click)="cambiarCantidad(item, item.cantidad + 1)"
                      >+</button>
                    </div>
                    <strong>{{ precio(item.subtotal) }}</strong>
                    <button
                      type="button"
                      class="carrito-panel-eliminar"
                      [attr.aria-label]="'Eliminar ' + item.nombre + ' del carrito'"
                      [disabled]="cambioPendiente() === item.id"
                      (click)="quitar(item)"
                    >
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                        stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <path d="M4 7h16M10 11v6m4-6v6M5 7l1 14h12l1-14M9 7V4h6v3" />
                      </svg>
                    </button>
                  </div>
                  @if (!item.disponible) {
                    <p class="carrito-panel-no-disponible">Disponibilidad insuficiente</p>
                  }
                </div>
              </li>
            }
          </ul>
          @if (errorCambio(); as mensaje) {
            <p class="alert alert-error carrito-panel-error" role="alert">{{ mensaje }}</p>
          }
          <footer class="carrito-panel-footer">
            <div><span>Subtotal</span><strong>{{ precio(carrito.subtotal()) }}</strong></div>
            <p class="carrito-no-reservados">
              <span aria-hidden="true">!</span>
              Los artículos no están reservados. ¡No los dejes escapar!
            </p>
            <a
              class="btn-primary carrito-panel-ver"
              routerLink="/cliente/carrito"
              (click)="carrito.cerrarPanel()"
            >Ver carrito</a>
          </footer>
        }
      </aside>
    }
  `,
})
export class Navbar {
  protected readonly auth = inject(AuthService);
  protected readonly carrito = inject(CarritoService);
  protected readonly menuAbierto = signal(false);
  protected readonly cuentaAbierta = signal(false);
  protected readonly categorias = OPCIONES_CATEGORIAS;
  protected readonly grupoAbierto = signal<string | null>(null);
  protected readonly precio = formatearPrecio;
  protected readonly cambioPendiente = signal<number | null>(null);
  protected readonly errorCambio = signal<string | null>(null);

  protected alternarGrupo(grupo: string): void {
    this.grupoAbierto.update((actual) => (actual === grupo ? null : grupo));
  }

  protected cerrarMenu(): void {
    this.menuAbierto.set(false);
    this.grupoAbierto.set(null);
  }

  protected cambiarCantidad(item: ItemCarrito, cantidad: number): void {
    if (cantidad < 1 || this.cambioPendiente() !== null) return;
    this.cambioPendiente.set(item.id);
    this.errorCambio.set(null);
    this.carrito.cambiarCantidad(item.id, cantidad).subscribe({
      error: (error: unknown) => {
        this.errorCambio.set(procesarErrorApi(error));
        this.cambioPendiente.set(null);
      },
      complete: () => this.cambioPendiente.set(null),
    });
  }

  protected quitar(item: ItemCarrito): void {
    if (this.cambioPendiente() !== null || !window.confirm(
      '¿Deseas eliminar esta prenda del carrito?',
    )) return;
    this.cambioPendiente.set(item.id);
    this.errorCambio.set(null);
    this.carrito.quitar(item.id).subscribe({
      error: (error: unknown) => {
        this.errorCambio.set(procesarErrorApi(error));
        this.cambioPendiente.set(null);
      },
      complete: () => this.cambioPendiente.set(null),
    });
  }
}
