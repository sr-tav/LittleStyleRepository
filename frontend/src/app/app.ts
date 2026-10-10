import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CarritoService } from './features/catalogoCompras/services/carrito.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: '<router-outlet />',
})
export class App {
  constructor() {
    inject(CarritoService);
  }
}
