import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { crearAuthResponse } from '../testing/auth-testing';
import { AuthService, decodificarExpiracion } from './auth.service';

describe('AuthService', () => {
  let http: HttpTestingController;

  function crearServicio(): AuthService {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    return TestBed.inject(AuthService);
  }

  beforeEach(() => localStorage.clear());
  afterEach(() => http?.verify());

  it('inicia sin sesión', () => {
    const auth = crearServicio();
    expect(auth.autenticado()).toBe(false);
    expect(auth.getToken()).toBeNull();
    expect(auth.rutaInicio()).toBe('/auth/login');
  });

  it('login guarda la sesión y expone usuario y rol', () => {
    const auth = crearServicio();
    const respuesta = crearAuthResponse('VENDEDOR');

    auth.login({ email: 'test@correo.com', password: 'Clave1234' }).subscribe();
    const req = http.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush(respuesta);

    expect(auth.autenticado()).toBe(true);
    expect(auth.rol()).toBe('VENDEDOR');
    expect(auth.getToken()).toBe(respuesta.token);
    expect(auth.rutaInicio()).toBe('/vendedor');
    expect(localStorage.getItem('ls_token')).toBe(respuesta.token);
  });

  it('registrar guarda la sesión del nuevo usuario', () => {
    const auth = crearServicio();
    auth
      .registrar({
        nombre: 'Ana', apellido: 'Ruiz', email: 'ana@correo.com', telefono: '3001234567',
        password: 'Clave1234', confirmarPassword: 'Clave1234', rol: 'CLIENTE',
        nombreTienda: null, aceptaTerminos: true,
      })
      .subscribe();
    http.expectOne('/api/auth/register').flush(crearAuthResponse('CLIENTE'));

    expect(auth.tieneRol('CLIENTE')).toBe(true);
    expect(auth.tieneRol('VENDEDOR', 'ADMINISTRADOR')).toBe(false);
  });

  it('restaura una sesión vigente desde localStorage', () => {
    const respuesta = crearAuthResponse('ADMINISTRADOR');
    localStorage.setItem('ls_token', respuesta.token);
    localStorage.setItem('ls_session', JSON.stringify(respuesta));

    const auth = crearServicio();

    expect(auth.autenticado()).toBe(true);
    expect(auth.rutaInicio()).toBe('/admin');
  });

  it('descarta una sesión expirada al iniciar', () => {
    const respuesta = crearAuthResponse('CLIENTE', Date.now() - 1000);
    localStorage.setItem('ls_token', respuesta.token);
    localStorage.setItem('ls_session', JSON.stringify(respuesta));

    const auth = crearServicio();

    expect(auth.autenticado()).toBe(false);
    expect(localStorage.getItem('ls_token')).toBeNull();
  });

  it('logout limpia la sesión y redirige al login', () => {
    const auth = crearServicio();
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    auth.login({ email: 'a@b.co', password: 'x' }).subscribe();
    http.expectOne('/api/auth/login').flush(crearAuthResponse('CLIENTE'));

    auth.logout();

    expect(auth.autenticado()).toBe(false);
    expect(localStorage.getItem('ls_session')).toBeNull();
    expect(navigate).toHaveBeenCalledWith(['/auth/login']);
  });

  it('decodificarExpiracion lee el claim exp en milisegundos', () => {
    const exp = Date.now() + 5000;
    const token = crearAuthResponse('CLIENTE', exp).token;
    expect(decodificarExpiracion(token)).toBe(Math.floor(exp / 1000) * 1000);
    expect(decodificarExpiracion('no-es-jwt')).toBeNull();
  });
});
