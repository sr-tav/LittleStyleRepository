import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { crearAuthResponse } from '../testing/auth-testing';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let auth: AuthService;
  let router: Router;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => httpMock.verify());

  function iniciarSesion(): string {
    auth.login({ email: 'a@b.co', password: 'x' }).subscribe();
    const respuesta = crearAuthResponse('CLIENTE');
    httpMock.expectOne('/api/auth/login').flush(respuesta);
    return respuesta.token;
  }

  it('adjunta el header Authorization Bearer a las peticiones de la API', () => {
    const token = iniciarSesion();

    http.get('/api/auth/me').subscribe();
    const req = httpMock.expectOne('/api/auth/me');

    expect(req.request.headers.get('Authorization')).toBe(`Bearer ${token}`);
    req.flush({});
  });

  it('no adjunta token sin sesión', () => {
    http.get('/api/auth/me').subscribe({ error: () => {} });
    const req = httpMock.expectOne('/api/auth/me');

    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('no adjunta token a login/registro ni a URLs externas', () => {
    iniciarSesion();

    http.post('/api/auth/register', {}).subscribe();
    http.get('https://cdn.externo.com/img.png').subscribe();

    expect(httpMock.expectOne('/api/auth/register').request.headers.has('Authorization')).toBe(false);
    expect(httpMock.expectOne('https://cdn.externo.com/img.png').request.headers.has('Authorization')).toBe(false);
  });

  it('ante un 401 cierra la sesión y envía al login', () => {
    iniciarSesion();

    http.get('/api/auth/me').subscribe({ error: () => {} });
    httpMock.expectOne('/api/auth/me').flush({ mensaje: 'expirado' }, { status: 401, statusText: 'Unauthorized' });

    expect(auth.autenticado()).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/auth/login'], expect.objectContaining({
      queryParams: expect.objectContaining({ sesion: 'expirada' }),
    }));
  });

  it('ante un 403 envía a acceso denegado sin cerrar la sesión', () => {
    iniciarSesion();

    http.get('/api/admin/usuarios').subscribe({ error: () => {} });
    httpMock.expectOne('/api/admin/usuarios').flush({}, { status: 403, statusText: 'Forbidden' });

    expect(auth.autenticado()).toBe(true);
    expect(router.navigate).toHaveBeenCalledWith(['/acceso-denegado']);
  });

  it('un 401 en el login no redirige (credenciales incorrectas)', () => {
    auth.login({ email: 'a@b.co', password: 'mala' }).subscribe({ error: () => {} });
    httpMock.expectOne('/api/auth/login').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(router.navigate).not.toHaveBeenCalled();
  });
});
