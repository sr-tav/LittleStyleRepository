import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CarritoService } from './features/catalogoCompras/services/carrito.service';

/**
 * Componente raíz que aloja la salida del enrutador y activa el servicio del carrito.
 * @see ADR-01
 * @see US-10
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: '<router-outlet />',
})
export class App {
  /** Inicializa la dependencia del carrito al crear la aplicación. */
  constructor() {
    inject(CarritoService);
  }
}
