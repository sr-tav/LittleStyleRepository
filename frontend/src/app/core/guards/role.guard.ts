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
 *
 * @param route Ruta cuya configuración proporciona `data.roles`.
 * @param state Estado de navegación, usado para conservar la URL al redirigir al login.
 * @returns `true` si el rol está permitido o un árbol de URL a login/acceso denegado.
 * @see US-05
 * @see RNF-10
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
