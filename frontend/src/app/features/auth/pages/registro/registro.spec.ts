import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { crearAuthResponse } from '../../../../core/testing/auth-testing';
import { Registro } from './registro';

describe('Registro (UI-2)', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Registro],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => httpMock.verify());

  function crear() {
    const fixture = TestBed.createComponent(Registro);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const escribir = (id: string, valor: string) => {
      const input = el.querySelector<HTMLInputElement>(`#${id}`)!;
      input.value = valor;
      input.dispatchEvent(new Event('input'));
    };
    return { fixture, el, escribir };
  }

  function llenarValido(escribir: (id: string, v: string) => void, el: HTMLElement) {
    escribir('nombre', 'María');
    escribir('apellido', 'González');
    escribir('email', 'maria@correo.com');
    escribir('telefono', '3001234567');
    escribir('password', 'Clave1234');
    escribir('confirmarPassword', 'Clave1234');
    const terminos = el.querySelector<HTMLInputElement>('input[type=checkbox]')!;
    terminos.click();
  }

  it('muestra errores de validación y no envía si el formulario está vacío', async () => {
    const { fixture, el } = crear();

    el.querySelector<HTMLButtonElement>('button[type=submit]')!.click();
    await fixture.whenStable();

    expect(el.querySelectorAll('.field-error').length).toBeGreaterThanOrEqual(6);
    expect(el.textContent).toContain('El nombre es obligatorio');
    httpMock.expectNone('/api/auth/register');
  });

  it('valida formato de correo, celular y coincidencia de contraseñas', async () => {
    const { fixture, el, escribir } = crear();
    escribir('email', 'correo-malo');
    escribir('telefono', '12345');
    escribir('password', 'Clave1234');
    escribir('confirmarPassword', 'Otra1234');

    el.querySelector<HTMLButtonElement>('button[type=submit]')!.click();
    await fixture.whenStable();

    expect(el.textContent).toContain('Ingresa un correo válido');
    expect(el.textContent).toContain('celular de 10 dígitos');
    expect(el.textContent).toContain('Las contraseñas no coinciden');
  });

  it('exige el nombre de la tienda solo al elegir Vendedor', async () => {
    const { fixture, el, escribir } = crear();
    expect(el.querySelector('#nombreTienda')).toBeNull();

    const [, vendedor] = Array.from(el.querySelectorAll<HTMLButtonElement>('.role-option'));
    vendedor.click();
    await fixture.whenStable();
    expect(el.querySelector('#nombreTienda')).not.toBeNull();

    llenarValido(escribir, el);
    el.querySelector<HTMLButtonElement>('button[type=submit]')!.click();
    await fixture.whenStable();

    expect(el.textContent).toContain('El nombre de la tienda es obligatorio');
    httpMock.expectNone('/api/auth/register');
  });

  it('envía el registro válido y muestra errores por campo devueltos por el backend', async () => {
    const { fixture, el, escribir } = crear();
    llenarValido(escribir, el);

    el.querySelector<HTMLButtonElement>('button[type=submit]')!.click();
    const req = httpMock.expectOne('/api/auth/register');
    expect(req.request.body).toMatchObject({ email: 'maria@correo.com', rol: 'CLIENTE', nombreTienda: null });
    req.flush(
      { status: 409, mensaje: 'Ya existe una cuenta registrada', errores: { email: 'Ya existe una cuenta registrada' } },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();

    expect(el.querySelector('.alert-error')?.textContent).toContain('Ya existe una cuenta registrada');
    expect(el.querySelector('#email')?.closest('.field')?.classList).toContain('has-error');
  });

  it('tras un registro exitoso navega al inicio del rol', async () => {
    const router = TestBed.inject(Router);
    const { fixture, el, escribir } = crear();
    llenarValido(escribir, el);

    el.querySelector<HTMLButtonElement>('button[type=submit]')!.click();
    httpMock.expectOne('/api/auth/register').flush(crearAuthResponse('CLIENTE'));
    await fixture.whenStable();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/cliente');
  });
});
