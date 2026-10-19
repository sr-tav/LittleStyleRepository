import { Component, input, inject, signal } from '@angular/core';

import { procesarErrorApi } from '../../auth/auth.validators';
import { CarritoService } from '../services/carrito.service';
import { VitrinaService } from '../../vitrina/services/vitrina.service';

/**
 * Permite consultar tallas y agregar una prenda disponible al carrito desde una vista compacta.
 * @see US-10
 * @see UI-6
 * @see UI-8
 */
@Component({
  selector: 'app-acceso-rapido-prenda',
  host: {
    '(mouseenter)': 'mostrarTallas()',
    '(mouseleave)': 'ocultar()',
  },
  template: `
    <div
      class="producto-acceso-rapido"
      (focusin)="mostrarTallas()"
      (focusout)="alPerderFoco($event)"
    >
      <button
        type="button"
        class="producto-acceso-trigger"
        [disabled]="!disponible() || agregando()"
        [attr.aria-expanded]="abierto()"
        [attr.aria-label]="'Agregar ' + nombre() + ' al carrito'"
        (click)="agregarSeleccionada()"
      >{{ disponible() ? 'Agregar al carrito' : 'Agotado' }}</button>
      @if (abierto() && disponible()) {
        @if (cargando()) {
          <p class="producto-acceso-mensaje" role="status">Cargando tallas…</p>
        } @else if (error(); as mensaje) {
          <p class="producto-acceso-error" role="alert">{{ mensaje }}</p>
        }
        @if (tallas().length > 0) {
          <div class="producto-acceso-tallas" role="group" [attr.aria-label]="'Tallas disponibles para ' + nombre()">
            @for (talla of tallas(); track talla) {
              <button
                type="button"
                [disabled]="agregando() || cargando()"
                [class.is-seleccionada]="tallaSeleccionada() === talla"
                [attr.aria-pressed]="tallaSeleccionada() === talla"
                [attr.aria-label]="'Agregar ' + nombre() + ', talla ' + talla"
                (click)="seleccionarTalla(talla)"
              >{{ talla }}</button>
            }
          </div>
        } @else if (!cargando() && !error()) {
          <p class="producto-acceso-mensaje">No hay tallas disponibles.</p>
        }
      }
    </div>
  `,
})
export class AccesoRapidoPrenda {
  private readonly vitrina = inject(VitrinaService);
  private readonly carrito = inject(CarritoService);

  readonly prendaId = input.required<number>();
  readonly nombre = input.required<string>();
  readonly disponible = input(true);
  readonly tallasDisponibles = input<readonly string[] | null>(null);

  protected readonly abierto = signal(false);
  protected readonly cargando = signal(false);
  protected readonly tallas = signal<string[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly tallaSeleccionada = signal<string | null>(null);
  protected readonly agregando = signal(false);
  private cargado = false;

  /**
   * Abre el selector y carga las tallas una sola vez, salvo que ya se hayan proporcionado.
   * @returns No devuelve un valor.
   */
  protected mostrarTallas(): void {
    this.abierto.set(true);
    if (!this.disponible() || this.cargado || this.cargando()) return;
    const tallasDisponibles = this.tallasDisponibles();
    if (tallasDisponibles !== null) {
      this.tallas.set([...tallasDisponibles]);
      this.cargado = true;
      return;
    }

    this.cargando.set(true);
    this.error.set(null);
    this.vitrina.detalle(this.prendaId()).subscribe({
      next: (detalle) => {
        this.tallas.set(detalle.tallas.filter((talla) => talla.disponible).map((talla) => talla.talla));
        this.cargado = true;
        this.cargando.set(false);
      },
      error: (error: unknown) => {
        this.error.set(procesarErrorApi(error));
        this.cargando.set(false);
      },
    });
  }

  protected ocultar(): void {
    this.abierto.set(false);
  }

  /**
   * Selecciona una talla y solicita agregarla al carrito.
   * @param talla Talla elegida.
   * @returns No devuelve un valor.
   */
  protected seleccionarTalla(talla: string): void {
    this.tallaSeleccionada.set(talla);
    this.error.set(null);
    this.agregar(talla);
  }

  /**
   * Cierra el selector solo si el foco salió del contenedor.
   * @param event Evento de cambio de foco.
   * @returns No devuelve un valor.
   */
  protected alPerderFoco(event: FocusEvent): void {
    const contenedor = event.currentTarget;
    if (contenedor instanceof HTMLElement && event.relatedTarget instanceof Node
      && contenedor.contains(event.relatedTarget)) return;
    this.ocultar();
  }

  /**
   * Valida que exista una talla elegida antes de agregar la prenda.
   * @returns No devuelve un valor.
   */
  protected agregarSeleccionada(): void {
    const talla = this.tallaSeleccionada();
    if (!talla) {
      this.mostrarTallas();
      this.error.set('Selecciona una talla.');
      return;
    }
    this.agregar(talla);
  }

  /**
   * Agrega una unidad de la talla indicada si la prenda está disponible.
   * @param talla Talla que se agregará.
   * @returns No devuelve un valor.
   */
  private agregar(talla: string): void {
    if (this.agregando() || !this.disponible()) return;
    this.agregando.set(true);
    this.error.set(null);
    this.carrito.agregar({ prendaId: this.prendaId(), talla, cantidad: 1 }).subscribe({
      next: () => {
        this.agregando.set(false);
        this.tallaSeleccionada.set(null);
        this.abierto.set(false);
        this.carrito.abrirPanel();
      },
      error: (error: unknown) => {
        this.error.set(procesarErrorApi(error));
        this.agregando.set(false);
      },
    });
  }
}
