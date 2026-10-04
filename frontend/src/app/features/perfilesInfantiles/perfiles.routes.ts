import { Routes } from '@angular/router';

export const PERFILES_ROUTES: Routes = [
  {
    path: '',
    title: 'Mis hijos | LittleStyle',
    loadComponent: () => import('./pages/perfiles-lista').then((m) => m.PerfilesLista),
  },
  {
    path: ':id',
    title: 'Perfil infantil | LittleStyle',
    loadComponent: () => import('./pages/perfil-detalle').then((m) => m.PerfilDetalle),
  },
];
