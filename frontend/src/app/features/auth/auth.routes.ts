import { Routes } from '@angular/router';

/**
 * Rutas de autenticación con carga diferida de las páginas de acceso y registro.
 * @see US-05
 * @see UI-1
 * @see UI-2
 */
export const AUTH_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  {
    path: 'login',
    title: 'Iniciar sesión | LittleStyle',
    loadComponent: () => import('./pages/login/login').then((m) => m.Login),
  },
  {
    path: 'registro',
    title: 'Crear cuenta | LittleStyle',
    loadComponent: () => import('./pages/registro/registro').then((m) => m.Registro),
  },
  {
    path: 'recuperar',
    title: 'Recuperar contraseña | LittleStyle',
    loadComponent: () => import('./pages/recuperar/recuperar').then((m) => m.RecuperarPassword),
  },
  {
    path: 'restablecer',
    title: 'Restablecer contraseña | LittleStyle',
    loadComponent: () => import('./pages/restablecer/restablecer').then((m) => m.RestablecerPassword),
  },
];
