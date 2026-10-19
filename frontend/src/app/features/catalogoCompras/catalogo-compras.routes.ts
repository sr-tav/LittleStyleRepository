import { Routes } from '@angular/router';

/**
 * Ruta de la página de carrito de compras.
 * @see US-10
 * @see UI-10
 */
export const CARRITO_ROUTES: Routes = [
  {
    path: '',
    title: 'Carrito de compras | LittleStyle',
    loadComponent: () => import('./pages/carrito').then((m) => m.CarritoPage),
  },
];

/**
 * Ruta de la página que presenta el resultado del pago.
 * @see US-11
 * @see UI-15
 */
export const PAGO_RESULTADO_ROUTES: Routes = [
  {
    path: '',
    title: 'Resultado del pago | LittleStyle',
    loadComponent: () => import('./pages/pago-resultado').then((m) => m.PagoResultadoPage),
  },
];

/**
 * Ruta de la página de información de envío previa a la compra.
 * @see US-10
 * @see UI-11
 * @see UI-12
 */
export const CHECKOUT_ROUTES: Routes = [
  {
    path: '',
    title: 'Información de envío | LittleStyle',
    loadComponent: () => import('./pages/checkout').then((m) => m.CheckoutPage),
  },
];
