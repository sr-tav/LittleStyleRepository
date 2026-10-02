import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';

import { Rol } from '../models/auth.models';
import { AuthService } from '../services/auth.service';
import { authGuard, guestGuard } from './auth.guard';
import { roleGuard } from './role.guard';

describe('Guards de autenticación y rol', () => {
  let router: Router;
  let authMock: { estaAutenticado: ReturnType<typeof vi.fn>; tieneRol: ReturnType<typeof vi.fn>; rutaInicio: ReturnType<typeof vi.fn> };

  const estado = { url: '/vendedor' } as RouterStateSnapshot;
  const ruta = (roles?: Rol[]) => ({ data: roles ? { roles } : {} }) as unknown as ActivatedRouteSnapshot;

  function sesion(rol: Rol | null): void {
    authMock.estaAutenticado.mockReturnValue(rol !== null);
    authMock.tieneRol.mockImplementation((...roles: Rol[]) => rol !== null && roles.includes(rol));
    authMock.rutaInicio.mockReturnValue(rol ? `/${rol.toLowerCase()}` : '/auth/login');
  }

  function ejecutar(guard: typeof roleGuard, r = ruta(), s = estado) {
    return TestBed.runInInjectionContext(() => guard(r, s));
  }

  beforeEach(() => {
    authMock = { estaAutenticado: vi.fn(), tieneRol: vi.fn(), rutaInicio: vi.fn() };
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: authMock }],
    });
    router = TestBed.inject(Router);
  });

  describe('roleGuard', () => {
    it('permite el acceso si el rol está en data.roles', () => {
      sesion('VENDEDOR');
      expect(ejecutar(roleGuard, ruta(['VENDEDOR']))).toBe(true);
    });

    it('redirige a /acceso-denegado si el rol no está permitido', () => {
      sesion('CLIENTE');
      const resultado = ejecutar(roleGuard, ruta(['VENDEDOR'])) as UrlTree;
      expect(router.serializeUrl(resultado)).toBe('/acceso-denegado');
    });

    it('redirige al login con returnUrl si no hay sesión', () => {
      sesion(null);
      const resultado = ejecutar(roleGuard, ruta(['ADMINISTRADOR'])) as UrlTree;
      expect(router.serializeUrl(resultado)).toBe('/auth/login?returnUrl=%2Fvendedor');
    });

    it('admite varios roles por ruta', () => {
      sesion('ADMINISTRADOR');
      expect(ejecutar(roleGuard, ruta(['VENDEDOR', 'ADMINISTRADOR']))).toBe(true);
    });

    it('sin data.roles solo exige estar autenticado', () => {
      sesion('CLIENTE');
      expect(ejecutar(roleGuard, ruta())).toBe(true);
    });
  });

  describe('authGuard', () => {
    it('deja pasar con sesión vigente', () => {
      sesion('CLIENTE');
      expect(ejecutar(authGuard)).toBe(true);
    });

    it('redirige al login sin sesión', () => {
      sesion(null);
      expect(router.serializeUrl(ejecutar(authGuard) as UrlTree)).toBe('/auth/login?returnUrl=%2Fvendedor');
    });
  });

  describe('guestGuard', () => {
    it('deja ver login/registro a visitantes', () => {
      sesion(null);
      expect(ejecutar(guestGuard)).toBe(true);
    });

    it('envía al inicio de su rol a un usuario autenticado', () => {
      sesion('VENDEDOR');
      expect(router.serializeUrl(ejecutar(guestGuard) as UrlTree)).toBe('/vendedor');
    });
  });
});
