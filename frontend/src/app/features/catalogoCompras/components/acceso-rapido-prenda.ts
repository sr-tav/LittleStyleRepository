import { Component, computed, input, inject, signal } from '@angular/core';

import { procesarErrorApi } from '../../auth/auth.validators';
import { CarritoService } from '../services/carrito.service';
import { VitrinaService } from '../../vitrina/services/vitrina.service';
import { VitrinaTalla } from '../../vitrina/models/vitrina.models';
import { ETIQUETAS_COLORES } from '../../perfilesInfantiles/models/perfil-opciones';

/**
 * Permite seleccionar una variante exacta y agregarla al carrito desde una vista compacta.
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
        @if (colores().length > 0) {
          <label class="producto-acceso-color">
            Color
            <select [value]="colorSeleccionado()" (change)="seleccionarColor($event)" [disabled]="agregando()">
              <option value="">Elige un color</option>
              @for (color of colores(); track color) {
                <option [value]="color">{{ colorDe(color) }}</option>
              }
            </select>
          </label>
        }
        @if (colores().length === 0 || colorSeleccionado()) {
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
  protected readonly variantes = signal<VitrinaTalla[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly tallaSeleccionada = signal<string | null>(null);
  protected readonly colorSeleccionado = signal<string>('');
  protected readonly agregando = signal(false);
  protected readonly colores = computed(() => {
    const tallasDisponibles = this.tallasDisponibles();
    const disponibles = this.variantes()
      .filter((t) => t.stockDisponible > 0
        && (tallasDisponibles === null || tallasDisponibles.includes(t.talla)))
      .map((t) => t.color)
      .filter((color): color is string => !!color);
    return [...new Set(disponibles)];
  });
  protected readonly tallas = computed(() => {
    const disponibles = this.variantes().filter((t) => t.stockDisponible > 0);
    const tallasDisponibles = this.tallasDisponibles();
    const porColor = this.colores().length > 0
      ? disponibles.filter((t) => t.color === this.colorSeleccionado())
      : disponibles.filter((t) => !t.color);
    return [...new Set(porColor.map((t) => t.talla)
      .filter((talla) => tallasDisponibles === null || tallasDisponibles.includes(talla)))];
  });
  private cargado = false;

  /**
   * Abre el selector y carga las tallas una sola vez, salvo que ya se hayan proporcionado.
   * @returns No devuelve un valor.
   */
  protected mostrarTallas(): void {
    this.abierto.set(true);
    if (!this.disponible() || this.cargado || this.cargando()) return;
    this.cargando.set(true);
    this.error.set(null);
    this.vitrina.detalle(this.prendaId()).subscribe({
      next: (detalle) => {
        this.variantes.set(detalle.tallas);
        this.colorSeleccionado.set('');
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

  protected seleccionarColor(event: Event): void {
    if (!(event.target instanceof HTMLSelectElement)) return;
    this.colorSeleccionado.set(event.target.value);
    this.tallaSeleccionada.set(null);
    this.error.set(null);
  }

  protected colorDe(color: string): string {
    return ETIQUETAS_COLORES[color as keyof typeof ETIQUETAS_COLORES] ?? color;
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
      this.error.set(this.colores().length > 0 && !this.colorSeleccionado()
        ? 'Selecciona un color.'
        : 'Selecciona una talla.');
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
    this.carrito.agregar({
      prendaId: this.prendaId(),
      talla,
      cantidad: 1,
      color: this.colorSeleccionado() || null,
    }).subscribe({
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
