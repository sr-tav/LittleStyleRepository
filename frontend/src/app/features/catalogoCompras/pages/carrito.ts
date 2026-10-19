import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { Navbar } from '../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../auth/auth.validators';
import { formatearPrecio } from '../models/prenda-opciones';
import { ItemCarrito } from '../models/carrito.models';
import { CarritoService } from '../services/carrito.service';

/**
 * Presenta el carrito y permite cambiar cantidades, quitar líneas o continuar al checkout.
 * @see US-10
 * @see UI-10
 */
@Component({
  selector: 'app-carrito',
  imports: [Navbar, RouterLink],
  templateUrl: './carrito.html',
})
export class CarritoPage {
  private readonly router = inject(Router);
  protected readonly carrito = inject(CarritoService);
  protected readonly error = signal<string | null>(null);
  protected readonly precio = formatearPrecio;
  protected readonly cambioPendiente = signal<number | null>(null);

  /**
   * Comprueba que el carrito tenga productos y que todos estén disponibles.
   * @returns `true` cuando es posible continuar al checkout.
   */
  protected checkoutDisponible(): boolean {
    return this.carrito.items().length > 0
      && this.carrito.items().every((item) => item.disponible);
  }

  /**
   * Navega al checkout únicamente si todas las líneas están disponibles.
   * @returns No devuelve un valor.
   */
  protected continuarCheckout(): void {
    if (!this.checkoutDisponible()) return;
    void this.router.navigate(['/cliente/checkout']);
  }

  /**
   * Vuelve a consultar el carrito y presenta cualquier error recibido.
   * @returns No devuelve un valor.
   */
  protected recargar(): void {
    this.error.set(null);
    this.carrito.cargar().subscribe({
      error: (error: unknown) => this.error.set(procesarErrorApi(error)),
    });
  }

  /**
   * Solicita el cambio de cantidad si es positivo y no hay otra modificación pendiente.
   * @param item Línea cuyo número de unidades se cambia.
   * @param cantidad Nueva cantidad solicitada.
   * @returns No devuelve un valor.
   */
  protected cambiarCantidad(item: ItemCarrito, cantidad: number): void {
    if (cantidad < 1 || this.cambioPendiente() !== null) return;
    this.cambioPendiente.set(item.id);
    this.error.set(null);
    this.carrito.cambiarCantidad(item.id, cantidad).subscribe({
      error: (error: unknown) => {
        this.error.set(procesarErrorApi(error));
        this.cambioPendiente.set(null);
      },
      complete: () => this.cambioPendiente.set(null),
    });
  }

  /**
   * Pide confirmación antes de quitar una línea y evita cambios simultáneos.
   * @param item Línea que se eliminará.
   * @returns No devuelve un valor.
   */
  protected quitar(item: ItemCarrito): void {
    if (this.cambioPendiente() !== null || !window.confirm(
      '¿Deseas eliminar esta prenda del carrito?',
    )) return;
    this.cambioPendiente.set(item.id);
    this.error.set(null);
    this.carrito.quitar(item.id).subscribe({
      error: (error: unknown) => {
        this.error.set(procesarErrorApi(error));
        this.cambioPendiente.set(null);
      },
      complete: () => this.cambioPendiente.set(null),
    });
  }

  /**
   * Vacía el carrito si contiene líneas y no hay otra modificación en curso.
   * @returns No devuelve un valor.
   */
  protected vaciar(): void {
    if (this.cambioPendiente() !== null || this.carrito.items().length === 0) return;
    this.cambioPendiente.set(0);
    this.error.set(null);
    this.carrito.vaciar().subscribe({
      error: (error: unknown) => {
        this.error.set(procesarErrorApi(error));
        this.cambioPendiente.set(null);
      },
      complete: () => this.cambioPendiente.set(null),
    });
  }

  /**
   * Reduce una unidad; si la línea tiene una, solicita su eliminación.
   * @param item Línea cuya cantidad se reduce.
   * @returns No devuelve un valor.
   */
  protected disminuirCantidad(item: ItemCarrito): void {
    if (item.cantidad === 1) {
      this.quitar(item);
      return;
    }
    this.cambiarCantidad(item, item.cantidad - 1);
  }
}
