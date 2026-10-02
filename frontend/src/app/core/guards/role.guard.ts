import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Rol } from '../models/auth.models';
import { AuthService } from '../services/auth.service';

/**
 * RoleGuard: restringe una ruta a los roles definidos en `data.roles`.
 *
 * ```ts
 * { path: 'vendedor', canActivate: [roleGuard], data: { roles: ['VENDEDOR'] } }
 * ```
 * - Sin sesión → login (con returnUrl).
 * - Con sesión pero rol no permitido → /acceso-denegado.
 */
export const roleGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const roles = (route.data['roles'] ?? []) as Rol[];

  if (!auth.estaAutenticado()) {
    return router.createUrlTree(['/auth/login'], { queryParams: { returnUrl: state.url } });
  }
  if (roles.length === 0 || auth.tieneRol(...roles)) {
    return true;
  }
  return router.createUrlTree(['/acceso-denegado']);
};
