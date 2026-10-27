import { Routes } from '@angular/router';

/**
 * Rutas del panel administrativo (US-14): resumen, usuarios, moderación,
 * auditoría e incidencias. El roleGuard y los roles se declaran en app.routes.
 * @see US-14
 */
export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    title: 'Panel administrativo | LittleStyle',
    loadComponent: () =>
      import('./pages/dashboard-admin/dashboard-admin').then((m) => m.DashboardAdmin),
  },
  {
    path: 'usuarios',
    title: 'Gestión de usuarios | LittleStyle',
    loadComponent: () =>
      import('./pages/usuarios-admin/usuarios-admin').then((m) => m.UsuariosAdminPage),
  },
  {
    path: 'moderacion',
    title: 'Moderación de prendas | LittleStyle',
    loadComponent: () =>
      import('./pages/moderacion/moderacion').then((m) => m.ModeracionPage),
  },
  {
    path: 'auditoria',
    title: 'Auditoría | LittleStyle',
    loadComponent: () =>
      import('./pages/auditoria/auditoria').then((m) => m.AuditoriaPage),
  },
  {
    path: 'incidencias',
    title: 'Incidencias | LittleStyle',
    loadComponent: () =>
      import('./pages/incidencias-admin/incidencias-admin').then(
        (m) => m.IncidenciasAdminPage,
      ),
  },
];
