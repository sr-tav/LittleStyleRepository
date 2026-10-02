import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';

/** Endpoints públicos: no llevan token y un 401 en ellos significa credenciales incorrectas. */
const RUTAS_PUBLICAS = ['/auth/login', '/auth/register'];

/**
 * AuthInterceptor: adjunta "Authorization: Bearer <jwt>" a las peticiones hacia la API
 * y reacciona a 401 (sesión expirada/inválida) y 403 (rol sin permiso).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const esApi = req.url.startsWith(environment.apiUrl);
  const esPublica = RUTAS_PUBLICAS.some((ruta) => req.url.endsWith(ruta));

  let request = req;
  if (esApi && !esPublica) {
    const token = auth.getToken();
    if (token) {
      request = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
    }
  }

  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && esApi && !esPublica) {
        if (error.status === 401) {
          auth.logout(false);
          router.navigate(['/auth/login'], {
            queryParams: { sesion: 'expirada', returnUrl: router.url },
          });
        } else if (error.status === 403) {
          router.navigate(['/acceso-denegado']);
        }
      }
      return throwError(() => error);
    }),
  );
};
