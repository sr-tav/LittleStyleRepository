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
    httpMock.expectOne('/api/cliente/checkout/direccion').flush({
      guardada: direccion !== null,
      direccion,
    });
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

  it('muestra en la cuenta la dirección usada en checkout y permite actualizarla', () => {
    const direccion: DireccionEnvio = {
      destinatario: 'Laura Pérez',
      direccion: 'Calle 10 # 20-30',
      complemento: 'Apto. 101',
      codigoPostal: '630001',
      departamento: 'Quindío',
      municipio: 'Armenia',
      telefono: '3001234567',
    };
    const { fixture, element } = cargar(direccion);
    const panel = element.querySelector<HTMLElement>('.account-address');
    expect(panel?.textContent).toContain('Laura Pérez');
    expect(panel?.textContent).toContain('Calle 10 # 20-30');
    expect(panel?.textContent).toContain('Armenia, Quindío');

    panel?.querySelector<HTMLButtonElement>('.account-action')?.click();
    fixture.detectChanges();
    const complemento = panel?.querySelector<HTMLInputElement>('#direccionComplemento');
    if (!complemento) throw new Error('No se encontró el complemento de dirección');
    complemento.value = 'Torre 2, apto. 301';
    complemento.dispatchEvent(new Event('input', { bubbles: true }));
    panel?.querySelector<HTMLButtonElement>('button[type="submit"]')?.click();

    const request = httpMock.expectOne({ method: 'PUT', url: '/api/cliente/checkout/direccion' });
    expect(request.request.body).toEqual({
      ...direccion,
      complemento: 'Torre 2, apto. 301',
    });
    request.flush({
      guardada: true,
      direccion: { ...direccion, complemento: 'Torre 2, apto. 301' },
    });
    fixture.detectChanges();
    expect(panel?.textContent).toContain('Torre 2, apto. 301');
    expect(element.textContent).toContain('La dirección de envío se actualizó.');
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

    expect(element.textContent).toContain(
      'Se borrarán permanentemente tus datos personales y los datos de los perfiles infantiles que hayas creado.',
    );
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
