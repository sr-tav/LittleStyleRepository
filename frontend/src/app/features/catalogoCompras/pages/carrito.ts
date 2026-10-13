import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { Navbar } from '../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../auth/auth.validators';
import { formatearPrecio } from '../models/prenda-opciones';
import { ItemCarrito } from '../models/carrito.models';
import { CarritoService } from '../services/carrito.service';

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

  protected checkoutDisponible(): boolean {
    return this.carrito.items().length > 0
      && this.carrito.items().every((item) => item.disponible);
  }

  protected continuarCheckout(): void {
    if (!this.checkoutDisponible()) return;
    void this.router.navigate(['/cliente/checkout']);
  }

  protected recargar(): void {
    this.error.set(null);
    this.carrito.cargar().subscribe({
      error: (error: unknown) => this.error.set(procesarErrorApi(error)),
    });
  }

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

  protected disminuirCantidad(item: ItemCarrito): void {
    if (item.cantidad === 1) {
      this.quitar(item);
      return;
    }
    this.cambiarCantidad(item, item.cantidad - 1);
  }
}
