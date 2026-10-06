import { Routes } from '@angular/router';

/** UI-19 y UI-20: catálogo del vendedor, montado en /vendedor/productos. */
export const PRODUCTOS_ROUTES: Routes = [
  {
    path: '',
    title: 'Mis productos | LittleStyle',
    loadComponent: () => import('./pages/productos-lista').then((m) => m.ProductosLista),
  },
  {
    path: 'nuevo',
    title: 'Crear producto | LittleStyle',
    loadComponent: () => import('./pages/prenda-form').then((m) => m.PrendaForm),
  },
  {
    path: ':id',
    title: 'Editar producto | LittleStyle',
    loadComponent: () => import('./pages/prenda-form').then((m) => m.PrendaForm),
  },
];

/** UI-21 y UI-22: inventario y alertas, montado en /vendedor/inventario. */
export const INVENTARIO_ROUTES: Routes = [
  {
    path: '',
    title: 'Inventario | LittleStyle',
    loadComponent: () => import('./pages/inventario').then((m) => m.Inventario),
  },
  {
    path: 'alertas',
    title: 'Alertas de inventario | LittleStyle',
    loadComponent: () => import('./pages/alertas').then((m) => m.AlertasStock),
  },
];
