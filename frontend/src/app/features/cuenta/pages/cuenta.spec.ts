import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { signal } from '@angular/core';

import { crearAuthResponse } from '../../../core/testing/auth-testing';
import { CarritoService } from '../../catalogoCompras/services/carrito.service';
import { DireccionEnvio } from '../../catalogoCompras/models/checkout.models';
import { Cuenta } from './cuenta';

describe('Cuenta (US-06)', () => {
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Cuenta],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: CarritoService,
          useValue: {
            items: signal([]),
            cantidad: () => 0,
            subtotal: () => 0,
            panelAbierto: signal(false),
            alternarPanel: vi.fn(),
            cerrarPanel: vi.fn(),
          },
        },
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => httpMock.verify());

  function cargar(direccion: DireccionEnvio | null = null) {
    const fixture = TestBed.createComponent(Cuenta);
    fixture.detectChanges();
    httpMock.expectOne('/api/auth/me').flush({
      id: 1,
      nombre: 'Laura',
      apellido: 'Pérez',
      email: 'laura@correo.com',
      telefono: '3001234567',
      nombreTienda: null,
      rol: 'CLIENTE',
    });
    httpMock.expectOne('/api/cliente/checkout/direcciones').flush(
      direccion === null ? [] : [{ id: 1, direccion }],
    );
    fixture.detectChanges();
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  it('muestra los datos personales y permite editarlos', () => {
    const { fixture, element } = cargar();
    expect(element.textContent).toContain('Laura Pérez');
    expect(element.textContent).toContain('laura@correo.com');
    Array.from(element.querySelectorAll<HTMLButtonElement>('button'))
      .find((button) => button.textContent?.trim() === 'Editar')!
      .click();
    fixture.detectChanges();
    const nombre = element.querySelector<HTMLInputElement>('#nombre')!;
    nombre.value = 'Laura Isabel';
    nombre.dispatchEvent(new Event('input'));
    element.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();

    const request = httpMock.expectOne({ method: 'PUT', url: '/api/auth/me' });
    expect(request.request.body).toMatchObject({
      nombre: 'Laura Isabel',
      apellido: 'Pérez',
      email: 'laura@correo.com',
      nuevaPassword: null,
    });
    request.flush({
      ...crearAuthResponse('CLIENTE'),
      usuario: {
        id: 1,
        nombre: 'Laura Isabel',
        apellido: 'Pérez',
        email: 'laura@correo.com',
        telefono: '3001234567',
        nombreTienda: null,
        rol: 'CLIENTE',
      },
    });
    fixture.detectChanges();
    expect(element.textContent).toContain('Los datos de tu cuenta se actualizaron.');
  });

  it('solo muestra el desplegable postal después de elegir departamento y municipio', () => {
    const { fixture, element } = cargar();
    element.querySelector<HTMLButtonElement>('.account-address .account-action')?.click();
    fixture.detectChanges();

    expect(element.textContent).toContain('Selecciona primero un departamento y municipio.');
    expect(element.querySelector('#direccionCodigoPostal')).toBeNull();

    const elegir = (id: string, value: string) => {
      const control = element.querySelector<HTMLSelectElement>(`#${id}`);
      if (!control) throw new Error(`No existe el campo ${id}`);
      control.value = value;
      control.dispatchEvent(new Event('change', { bubbles: true }));
      fixture.detectChanges();
    };

    elegir('direccionDepartamento', 'Quindío');
    expect(element.querySelector('#direccionCodigoPostal')).toBeNull();
    elegir('direccionMunicipio', 'Armenia');

    expect(element.querySelector('select#direccionCodigoPostal')).not.toBeNull();
    expect(element.querySelector('input#direccionCodigoPostal')).toBeNull();
    expect(Array.from(element.querySelectorAll<HTMLOptionElement>('#direccionCodigoPostal option'))
      .map((option) => option.value).filter(Boolean))
      .toContain('630001');
  });

  it('muestra en la cuenta la dirección usada en checkout y permite actualizarla', () => {
    const direccion: DireccionEnvio = {
      destinatario: 'Laura Pérez',
      direccion: 'Calle 10 # 20 - 30',
      tipoVia: 'Calle',
      numeroVia: '10',
      numeroCruce: '20',
      placa: '30',
      complemento: 'Apto. 101',
      codigoPostal: '630001',
      departamento: 'Quindío',
      municipio: 'Armenia',
      telefono: '3001234567',
    };
    const { fixture, element } = cargar(direccion);
    const panel = element.querySelector<HTMLElement>('.account-address');
    expect(panel?.textContent).toContain('Laura Pérez');
    expect(panel?.textContent).toContain('Calle 10 # 20 - 30');
    expect(panel?.textContent).toContain('Armenia, Quindío');

    panel?.querySelector<HTMLButtonElement>('.checkout-direccion .account-secondary-action')?.click();
    fixture.detectChanges();
    const direccionInput = panel?.querySelector<HTMLInputElement>('#direccionCalle');
    if (!direccionInput) throw new Error('No se encontró el campo de dirección');
    direccionInput.value = 'Calle 45';
    direccionInput.dispatchEvent(new Event('input', { bubbles: true }));
    panel?.querySelector<HTMLButtonElement>('.account-form-actions button.account-action')?.click();
    fixture.detectChanges();
    expect(panel?.textContent).toContain('Ingresa una dirección válida. Ejemplo: Carrera 12 # 34-56.');
    expect(direccionInput.getAttribute('aria-invalid')).toBe('true');
    expect(direccionInput.closest('.field')?.classList.contains('has-error')).toBe(true);
    httpMock.expectNone({ method: 'PUT', url: '/api/cliente/checkout/direcciones/1' });

    direccionInput.value = 'Carrera 13 # 7 - 8';
    direccionInput.dispatchEvent(new Event('input', { bubbles: true }));
    fixture.detectChanges();
    expect(direccionInput.getAttribute('aria-invalid')).toBeNull();
    expect(direccionInput.closest('.field')?.classList.contains('has-error')).toBe(false);
    expect(panel?.querySelector('#direccionTipoVia')).toBeNull();
    expect(panel?.querySelector('#direccionNumeroVia')).toBeNull();
    expect(panel?.querySelector('#direccionNumeroCruce')).toBeNull();
    expect(panel?.querySelector('#direccionPlaca')).toBeNull();
    panel?.querySelector<HTMLButtonElement>('.account-form-actions button.account-action')?.click();

    const request = httpMock.expectOne({ method: 'PUT', url: '/api/cliente/checkout/direcciones/1' });
    expect(request.request.body).toEqual({
      ...direccion,
      direccion: 'Carrera 13 # 7 - 8',
      tipoVia: 'Carrera',
      numeroVia: '13',
      numeroCruce: '7',
      placa: '8',
    });
    request.flush({
      id: 1,
      direccion: {
        ...direccion,
        direccion: 'Carrera 13 # 7 - 8',
        tipoVia: 'Carrera',
        numeroVia: '13',
        numeroCruce: '7',
        placa: '8',
      },
    });
    fixture.detectChanges();
    expect(panel?.textContent).toContain('Carrera 13 # 7 - 8');
    expect(element.textContent).toContain('La dirección de envío se actualizó.');
  });

  it('permite agregar una dirección sin reemplazar las direcciones existentes', () => {
    const primera: DireccionEnvio = {
      destinatario: 'Laura Pérez',
      direccion: 'Calle 10 # 20 - 30',
      tipoVia: 'Calle',
      numeroVia: '10',
      numeroCruce: '20',
      placa: '30',
      complemento: null,
      codigoPostal: '630001',
      departamento: 'Quindío',
      municipio: 'Armenia',
      telefono: '3001234567',
    };
    const { fixture, element } = cargar(primera);
    element.querySelector<HTMLButtonElement>('.account-address .account-action')?.click();
    fixture.detectChanges();

    const llenar = (id: string, value: string) => {
      const control = element.querySelector<HTMLInputElement | HTMLSelectElement>(`#${id}`);
      if (!control) throw new Error(`No existe el campo ${id}`);
      control.value = value;
      control.dispatchEvent(new Event('input', { bubbles: true }));
      control.dispatchEvent(new Event('change', { bubbles: true }));
      fixture.detectChanges();
    };
    llenar('direccionDestinatario', 'Laura Pérez');
    llenar('direccionTelefono', '3001234567');
    llenar('direccionCalle', 'Carrera 12 # 34-56');
    llenar('direccionDepartamento', 'Quindío');
    llenar('direccionMunicipio', 'Armenia');
    llenar('direccionCodigoPostal', '630001');
    const form = element.querySelector<HTMLFormElement>('.account-address form');
    if (!form) throw new Error('No se encontró el formulario de dirección');
    const formSubmit = vi.fn();
    form.addEventListener('submit', formSubmit);
    form.querySelector<HTMLButtonElement>('.account-form-actions button.account-action')?.click();
    fixture.detectChanges();
    expect(formSubmit).not.toHaveBeenCalled();

    const request = httpMock.expectOne({ method: 'POST', url: '/api/cliente/checkout/direcciones' });
    expect(request.request.body).toMatchObject({
      destinatario: 'Laura Pérez',
      direccion: 'Carrera 12 # 34-56',
    });
    request.flush({
      id: 2,
      direccion: {
        ...primera,
        direccion: 'Carrera 12 # 34-56',
        tipoVia: 'Carrera',
        numeroVia: '12',
        numeroCruce: '34',
        placa: '56',
      },
    });
    fixture.detectChanges();

    expect(element.querySelectorAll('.account-address .checkout-direccion')).toHaveLength(2);
    expect(element.textContent).toContain('Calle 10 # 20 - 30');
    expect(element.textContent).toContain('Carrera 12 # 34-56');

    const tarjetaNueva = Array.from(element.querySelectorAll<HTMLElement>('.account-address .checkout-direccion'))
      .find((tarjeta) => tarjeta.textContent?.includes('Carrera 12 # 34-56'));
    tarjetaNueva?.querySelector<HTMLButtonElement>('.account-secondary-action')?.click();
    fixture.detectChanges();
    const direccionInput = element.querySelector<HTMLInputElement>('#direccionCalle');
    if (!direccionInput) throw new Error('No se encontró el campo de dirección');
    direccionInput.value = 'Calle 80 # 12 - 34';
    direccionInput.dispatchEvent(new Event('input', { bubbles: true }));
    fixture.detectChanges();
    const formActualizado = element.querySelector<HTMLFormElement>('.account-address form');
    if (!formActualizado) throw new Error('No se encontró el formulario de dirección editada');
    const updateFormSubmit = vi.fn();
    formActualizado.addEventListener('submit', updateFormSubmit);
    formActualizado.querySelector<HTMLButtonElement>('.account-form-actions button.account-action')?.click();
    fixture.detectChanges();
    expect(updateFormSubmit).not.toHaveBeenCalled();

    const updateRequest = httpMock.expectOne({ method: 'PUT', url: '/api/cliente/checkout/direcciones/2' });
    expect(updateRequest.request.body).toMatchObject({
      direccion: 'Calle 80 # 12 - 34',
      tipoVia: 'Calle',
      numeroVia: '80',
      numeroCruce: '12',
      placa: '34',
    });
    updateRequest.flush({
      id: 2,
      direccion: {
        ...primera,
        direccion: 'Calle 80 # 12 - 34',
        tipoVia: 'Calle',
        numeroVia: '80',
        numeroCruce: '12',
        placa: '34',
      },
    });
    fixture.detectChanges();

    expect(element.textContent).toContain('Calle 10 # 20 - 30');
    expect(element.textContent).toContain('Calle 80 # 12 - 34');
    expect(element.textContent).not.toContain('Carrera 12 # 34-56');
  });

  it('permite abrir el formulario de seguridad y exige los datos del cambio de contraseña', () => {
    const { fixture, element } = cargar();
    Array.from(element.querySelectorAll<HTMLButtonElement>('button'))
      .find((button) => button.textContent?.trim() === 'Cambiar')!
      .click();
    fixture.detectChanges();
    const nueva = element.querySelector<HTMLInputElement>('#nuevaPassword')!;
    nueva.value = 'ClaveNueva123';
    nueva.dispatchEvent(new Event('input'));
    element.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    fixture.detectChanges();

    expect(element.textContent).toContain('Escribe tu contraseña actual');
    httpMock.expectNone({ method: 'PUT', url: '/api/auth/me' });
  });

  it('actualiza la contraseña por separado de la información personal', () => {
    const { fixture, element } = cargar();
    Array.from(element.querySelectorAll<HTMLButtonElement>('button'))
      .find((button) => button.textContent?.trim() === 'Cambiar')!
      .click();
    fixture.detectChanges();
    for (const [id, value] of [
      ['passwordActual', 'ClaveActual123'],
      ['nuevaPassword', 'ClaveNueva123'],
      ['confirmarNuevaPassword', 'ClaveNueva123'],
    ]) {
      const input = element.querySelector<HTMLInputElement>(`#${id}`)!;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    }
    element.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    const request = httpMock.expectOne({ method: 'PUT', url: '/api/auth/me' });
    expect(request.request.body).toMatchObject({
      email: 'laura@correo.com',
      passwordActual: 'ClaveActual123',
      nuevaPassword: 'ClaveNueva123',
      confirmarNuevaPassword: 'ClaveNueva123',
    });
    request.flush({
      ...crearAuthResponse('CLIENTE'),
      usuario: {
        id: 1,
        nombre: 'Laura',
        apellido: 'Pérez',
        email: 'laura@correo.com',
        telefono: '3001234567',
        nombreTienda: null,
        rol: 'CLIENTE',
      },
    });
    fixture.detectChanges();
    expect(element.textContent).toContain('La contraseña se actualizó correctamente.');
  });

  it('requiere confirmar la desactivación antes de llamar al servidor', () => {
    const { fixture, element } = cargar();
    Array.from(element.querySelectorAll<HTMLButtonElement>('button'))
      .find((button) => button.textContent?.includes('Eliminar mi cuenta'))!
      .click();
    fixture.detectChanges();

    expect(element.textContent).toContain('Eliminar cuenta');
    expect(element.textContent).toContain('los datos de los perfiles infantiles que hayas creado');
    httpMock.expectNone({ method: 'DELETE', url: '/api/auth/me' });
  });

  it('desactiva la cuenta al confirmar y cierra la sesión local', () => {
    const { fixture, element } = cargar();
    Array.from(element.querySelectorAll<HTMLButtonElement>('button'))
      .find((button) => button.textContent?.includes('Eliminar mi cuenta'))!
      .click();
    fixture.detectChanges();
    Array.from(element.querySelectorAll<HTMLButtonElement>('button'))
      .find((button) => button.textContent?.includes('Sí, eliminar mi cuenta'))!
      .click();
    httpMock.expectOne({ method: 'DELETE', url: '/api/auth/me' }).flush(null);

    expect(router.navigate).toHaveBeenCalledWith(['/auth/login']);
  });
});
