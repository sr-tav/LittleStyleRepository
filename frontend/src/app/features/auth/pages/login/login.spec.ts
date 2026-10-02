import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { crearAuthResponse } from '../../../../core/testing/auth-testing';
import { Login } from './login';

describe('Login (UI-1)', () => {
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => httpMock.verify());

  function crear() {
    const fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const escribir = (id: string, valor: string) => {
      const input = el.querySelector<HTMLInputElement>(`#${id}`)!;
      input.value = valor;
      input.dispatchEvent(new Event('input'));
    };
    const enviar = () => el.querySelector<HTMLButtonElement>('button[type=submit]')!.click();
    return { fixture, el, escribir, enviar };
  }

  it('muestra el título del mockup', () => {
    const { el } = crear();
    expect(el.querySelector('h1')?.textContent).toContain('Bienvenido de nuevo');
    expect(el.textContent).toContain('¿Olvidaste tu contraseña?');
  });

  it('valida campos obligatorios y formato de correo sin llamar al backend', async () => {
    const { fixture, el, escribir, enviar } = crear();
    enviar();
    await fixture.whenStable();
    expect(el.textContent).toContain('El correo es obligatorio');
    expect(el.textContent).toContain('La contraseña es obligatoria');

    escribir('email', 'no-es-correo');
    await fixture.whenStable();
    expect(el.textContent).toContain('Ingresa un correo válido');
    httpMock.expectNone('/api/auth/login');
  });

  it('con credenciales correctas navega al inicio según el rol', async () => {
    const { fixture, escribir, enviar } = crear();
    escribir('email', 'vendedor@correo.com');
    escribir('password', 'Clave1234');
    enviar();

    httpMock.expectOne('/api/auth/login').flush(crearAuthResponse('VENDEDOR'));
    await fixture.whenStable();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/vendedor');
  });

  it('muestra el mensaje del backend ante credenciales incorrectas', async () => {
    const { fixture, el, escribir, enviar } = crear();
    escribir('email', 'a@correo.com');
    escribir('password', 'mala');
    enviar();

    httpMock
      .expectOne('/api/auth/login')
      .flush({ status: 401, mensaje: 'Correo o contraseña incorrectos' }, { status: 401, statusText: 'Unauthorized' });
    await fixture.whenStable();

    expect(el.querySelector('.alert-error')?.textContent).toContain('Correo o contraseña incorrectos');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });
});
