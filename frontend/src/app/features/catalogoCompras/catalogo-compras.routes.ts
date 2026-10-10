import { Routes } from '@angular/router';

export const CARRITO_ROUTES: Routes = [
  {
    path: '',
    title: 'Carrito de compras | LittleStyle',
    loadComponent: () => import('./pages/carrito').then((m) => m.CarritoPage),
  },
];

export const CHECKOUT_ROUTES: Routes = [
  {
    path: '',
    title: 'Información de envío | LittleStyle',
    loadComponent: () => import('./pages/checkout').then((m) => m.CheckoutPage),
  },
];
