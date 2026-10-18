import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import {
  NavigationError,
  provideRouter,
  withComponentInputBinding,
  withNavigationErrorHandler,
} from '@angular/router';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';

const RECARGA_KEY = 'ls_recarga_version';

/**
 * Tras un despliegue, una pestaña con la versión anterior pide chunks que ya no existen y
 * la navegación falla sin aviso (los botones "no hacen nada"). Se recarga hacia la URL de
 * destino para tomar la versión nueva. Si la misma URL ya se recargó hace menos de 30 s, el
 * problema no es la versión y no se insiste, para no entrar en un ciclo de recargas.
 */
export function recargarSiCambioLaVersion(
  evento: NavigationError,
  recargar = (url: string) => window.location.assign(url),
  ahora = Date.now(),
): void {
  const mensaje = String((evento.error as Error | undefined)?.message ?? evento.error);
  if (!/dynamically imported module|Importing a module script failed|Loading chunk/i.test(mensaje)) {
    return;
  }
  try {
    const [url, cuando] = (sessionStorage.getItem(RECARGA_KEY) ?? '').split('|');
    if (url === evento.url && ahora - Number(cuando) < 30_000) {
      return;
    }
    sessionStorage.setItem(RECARGA_KEY, `${evento.url}|${ahora}`);
  } catch {
    // Sin sessionStorage se recarga igual: el caso normal es que la versión nueva cargue
  }
  recargar(evento.url);
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding(), withNavigationErrorHandler((e) => recargarSiCambioLaVersion(e))),
    provideHttpClient(withFetch(), withInterceptors([authInterceptor])),
  ],
};
