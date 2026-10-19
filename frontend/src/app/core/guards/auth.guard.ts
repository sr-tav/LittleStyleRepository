import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';

/**
 * Permite el paso solo a usuarios con sesión vigente; si no, redirige al login conservando la URL.
 * @param _route Ruta que se intenta activar; no interviene en la decisión.
 * @param state Estado de navegación, usado para conservar la URL solicitada.
 * @returns `true` con sesión vigente o un árbol de URL al login.
 * @see US-05
 * @see RNF-10
 */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.estaAutenticado()) {
    return true;
  }
  return router.createUrlTree(['/auth/login'], { queryParams: { returnUrl: state.url } });
};

/**
 * Evita que un usuario ya autenticado vuelva a login/registro: lo envía a su inicio.
 * @returns `true` sin sesión o un árbol de URL al inicio correspondiente.
 * @see US-05
 * @see RNF-10
 */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.estaAutenticado() ? router.parseUrl(auth.rutaInicio()) : true;
};
