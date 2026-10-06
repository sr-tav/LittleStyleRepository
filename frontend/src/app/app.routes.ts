import { inject } from '@angular/core';
import { Routes } from '@angular/router';

import { guestGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { AuthService } from './core/services/auth.service';

export const routes: Routes = [
  // La raíz envía a cada usuario al inicio de su rol (o al login si no hay sesión)
  { path: '', pathMatch: 'full', redirectTo: () => inject(AuthService).rutaInicio() },
  {
    path: 'auth',
    canActivate: [guestGuard],
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: 'cuenta',
    title: 'Mi cuenta | LittleStyle',
    canActivate: [roleGuard],
    loadComponent: () =>
      import('./features/cuenta/pages/cuenta').then((m) => m.Cuenta),
  },
  {
    path: 'cliente/hijos',
    title: 'Mis hijos | LittleStyle',
    canActivate: [roleGuard],
    data: { roles: ['CLIENTE'] },
    loadChildren: () =>
      import('./features/perfilesInfantiles/perfiles.routes').then((m) => m.PERFILES_ROUTES),
  },
  {
    path: 'cliente/recomendaciones',
    title: 'Talla sugerida | LittleStyle',
    canActivate: [roleGuard],
    data: { roles: ['CLIENTE'] },
    loadComponent: () =>
      import('./features/recomendacion/pages/recomendaciones-lista').then((m) => m.RecomendacionesLista),
  },
  {
    path: 'cliente',
    title: 'Inicio | LittleStyle',
    canActivate: [roleGuard],
    data: { roles: ['CLIENTE'] },
    loadComponent: () =>
      import('./features/home/pages/cliente-inicio/cliente-inicio').then((m) => m.ClienteInicio),
  },
  {
    path: 'vendedor/productos',
    canActivate: [roleGuard],
    data: { roles: ['VENDEDOR'] },
    loadChildren: () =>
      import('./features/catalogoCompras/catalogo.routes').then((m) => m.PRODUCTOS_ROUTES),
  },
  {
    path: 'vendedor/inventario',
    canActivate: [roleGuard],
    data: { roles: ['VENDEDOR'] },
    loadChildren: () =>
      import('./features/catalogoCompras/catalogo.routes').then((m) => m.INVENTARIO_ROUTES),
  },
  {
    path: 'vendedor',
    title: 'Panel del vendedor | LittleStyle',
    canActivate: [roleGuard],
    data: { roles: ['VENDEDOR'] },
    loadComponent: () =>
      import('./features/home/pages/vendedor-panel/vendedor-panel').then((m) => m.VendedorPanel),
  },
  {
    path: 'admin',
    title: 'Panel administrativo | LittleStyle',
    canActivate: [roleGuard],
    data: { roles: ['ADMINISTRADOR'] },
    loadComponent: () =>
      import('./features/home/pages/admin-panel/admin-panel').then((m) => m.AdminPanel),
  },
  {
    path: 'acceso-denegado',
    title: 'Acceso denegado | LittleStyle',
    loadComponent: () =>
      import('./shared/pages/acceso-denegado/acceso-denegado').then((m) => m.AccesoDenegado),
  },
  {
    path: '**',
    title: 'Página no encontrada | LittleStyle',
    loadComponent: () =>
      import('./shared/pages/no-encontrado/no-encontrado').then((m) => m.NoEncontrado),
  },
];
