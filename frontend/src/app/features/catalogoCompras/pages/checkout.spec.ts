import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { DireccionEnvio } from '../models/checkout.models';
import { CarritoService } from '../services/carrito.service';
import { CheckoutService } from '../services/checkout.service';
import { PerfilesService } from '../../perfilesInfantiles/services/perfiles.service';
import { CheckoutPage } from './checkout';

describe('CheckoutPage', () => {
  const perfil = {
    id: 3,
    nombre: 'Sofía',
    fechaNacimiento: '2021-01-01',
    edadAnios: 5,
    mesesRestantes: 0,
    contextura: 'MEDIA' as const,
    holgura: 'REGULAR' as const,
    alergias: [],
    otraAlergia: null,
    sinAlergias: true,
    coloresPreferidos: [],
    estampadosPreferidos: [],
    otroColor: null,
    otroEstampado: null,
    fechaCreacion: '2025-01-01T12:00:00',
    fechaActualizacion: '2025-01-01T12:00:00',
    ultimaMedicion: null,
    porcentajeCompletitud: 100,
    camposPendientes: [],
  };
  const item = {
    id: 10,
    prendaId: 20,
    nombre: 'Pantalón Jogger',
    imagenUrl: null,
    talla: '4',
    cantidad: 2,
    precioUnitario: 32900,
    subtotal: 65800,
    disponible: true,
  };
  const checkout = {
    calcularResumen: vi.fn(() => of({
      perfilInfantilId: 3,
      items: [{
        prendaId: 20,
        nombre: 'Pantalón Jogger',
        talla: '4',
        cantidad: 2,
        precioUnitario: 32900,
        subtotal: 65800,
      }],
      subtotal: 65800,
      costoEnvio: 7000,
      total: 72800,
    })),
    obtenerDireccion: vi.fn(() => of({
      guardada: false,
      direccion: null as DireccionEnvio | null,
    })),
    guardarDireccion: vi.fn((direccion: DireccionEnvio) => of({ guardada: true, direccion })),
    crearPedido: vi.fn(() => of({
      pedidoId: 55,
      estado: 'PENDIENTE_PAGO' as const,
      perfilInfantilId: 3,
      direccion: {
        destinatario: 'Ana Ruiz',
        direccion: 'Calle 1 # 2-3',
        complemento: null,
        codigoPostal: '630001',
        departamento: 'Quindío',
        municipio: 'Armenia',
        telefono: '3001234567',
      },
      items: [],
      subtotal: 65800,
      costoEnvio: 7000,
      total: 72800,
    })),
  };

  beforeEach(async () => {
    checkout.calcularResumen.mockClear();
    checkout.obtenerDireccion.mockReset().mockReturnValue(of({
      guardada: false,
      direccion: null,
    }));
    checkout.guardarDireccion.mockClear();
    checkout.crearPedido.mockClear();
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [CheckoutPage],
      providers: [
        provideRouter([]),
        { provide: CarritoService, useValue: { items: () => [item], cargar: () => of({ items: [] }) } },
        { provide: CheckoutService, useValue: checkout },
        { provide: PerfilesService, useValue: { listar: () => of([perfil]) } },
      ],
    })
      .compileComponents();
  });

  it('usa el perfil activo sin pedir seleccionarlo y presenta UI-12 con el pago deshabilitado', () => {
    const fixture = TestBed.createComponent(CheckoutPage);
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    expect(root.textContent).toContain('Usaremos tu perfil infantil activo: Sofía');
    expect(root.querySelector('#checkout-perfil')).toBeNull();
    expect(root.textContent).not.toContain('UI-11');
    expect(root.textContent).not.toContain('UI-12');
    const llenar = (selector: string, value: string) => {
      const control = root.querySelector<HTMLInputElement | HTMLSelectElement>(selector);
      if (!control) throw new Error(`No existe el campo ${selector}`);
      control.value = value;
      control.dispatchEvent(new Event('input', { bubbles: true }));
      control.dispatchEvent(new Event('change', { bubbles: true }));
      fixture.detectChanges();
    };

    llenar('#destinatario', 'Ana Ruiz');
    llenar('#direccion', 'Calle 1 # 2-3');
    llenar('#complemento', 'Apto. 302, torre 1');
    llenar('#codigoPostal', '630001');
    llenar('#departamento', 'Quindío');
    llenar('#municipio', 'Armenia');
    llenar('#telefono', '1234567890');
    root.querySelector<HTMLFormElement>('form')?.dispatchEvent(new Event('submit', { bubbles: true }));
    fixture.detectChanges();

    expect(checkout.calcularResumen).not.toHaveBeenCalled();
    expect(root.textContent).toContain('celular colombiano válido');

    llenar('#telefono', '3001234567');
    llenar('#codigoPostal', '63001');
    root.querySelector<HTMLFormElement>('form')?.dispatchEvent(new Event('submit', { bubbles: true }));
    fixture.detectChanges();

    expect(checkout.calcularResumen).not.toHaveBeenCalled();
    expect(root.textContent).toContain('código postal colombiano de 6 dígitos');

    llenar('#codigoPostal', '630001');
    llenar('#telefono', '3001234567');
    root.querySelector<HTMLFormElement>('form')?.dispatchEvent(new Event('submit', { bubbles: true }));
    fixture.detectChanges();

    expect(checkout.calcularResumen).toHaveBeenCalledWith({
      perfilInfantilId: 3,
      direccion: {
        destinatario: 'Ana Ruiz',
        direccion: 'Calle 1 # 2-3',
        complemento: 'Apto. 302, torre 1',
        codigoPostal: '630001',
        departamento: 'Quindío',
        municipio: 'Armenia',
        telefono: '3001234567',
      },
    });
    expect(checkout.guardarDireccion).toHaveBeenCalledWith({
      destinatario: 'Ana Ruiz',
      direccion: 'Calle 1 # 2-3',
      complemento: 'Apto. 302, torre 1',
      codigoPostal: '630001',
      departamento: 'Quindío',
      municipio: 'Armenia',
      telefono: '3001234567',
    });
    expect(root.textContent).toContain('Resumen de la compra');
    expect(root.textContent).toContain('Calle 1 # 2-3');
    expect(root.textContent).toContain('72.800');
    expect(root.querySelector<HTMLButtonElement>('.carrito-resumen button:not([disabled])')?.textContent)
      .toContain('Confirmar pedido');

    root.querySelector<HTMLButtonElement>('.carrito-resumen button:not([disabled])')?.click();
    fixture.detectChanges();
    expect(checkout.crearPedido).toHaveBeenCalledWith({
      perfilInfantilId: 3,
      direccion: {
        destinatario: 'Ana Ruiz',
        direccion: 'Calle 1 # 2-3',
        complemento: 'Apto. 302, torre 1',
        codigoPostal: '630001',
        departamento: 'Quindío',
        municipio: 'Armenia',
        telefono: '3001234567',
      },
    });
    expect(root.textContent).not.toContain('creado y pendiente de pago');
    const botonPago = root.querySelector<HTMLButtonElement>('.carrito-resumen button[disabled]');
    expect(botonPago?.textContent)
      .toContain('Pagar');
    expect(root.querySelector('.carrito-resumen .checkout-pago-nota')).toBeNull();
    expect(root.querySelector('#pago-pendiente')).toBeNull();
    expect(root.querySelectorAll('.carrito-resumen button')).toHaveLength(1);
  });

  it('permite reutilizar la dirección de cuenta y editarla', () => {
    checkout.obtenerDireccion.mockReturnValue(of({
      guardada: true,
      direccion: {
        destinatario: 'Ana Ruiz',
        direccion: 'Calle 1 # 2-3',
        complemento: 'Apto. 302',
        codigoPostal: '630001',
        departamento: 'Quindío',
        municipio: 'Armenia',
        telefono: '3001234567',
      },
    }));
    const fixture = TestBed.createComponent(CheckoutPage);
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;

    expect(root.textContent).toContain('Dirección guardada en tu cuenta');
    expect(root.querySelector<HTMLInputElement>('#direccion')).toBeNull();

    Array.from(root.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('Editar dirección'))
      ?.click();
    fixture.detectChanges();

    expect(root.querySelector<HTMLInputElement>('#direccion')?.value).toBe('Calle 1 # 2-3');
    expect(root.querySelector<HTMLInputElement>('#complemento')?.value).toBe('Apto. 302');
  });

  it('filtra municipios por departamento y limpia la selección anterior al cambiarlo', () => {
    const fixture = TestBed.createComponent(CheckoutPage);
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    const departamento = root.querySelector<HTMLSelectElement>('#departamento');
    const municipio = root.querySelector<HTMLSelectElement>('#municipio');
    if (!departamento || !municipio) throw new Error('No se encontraron los selectores de ubicación');

    departamento.value = 'Quindío';
    departamento.dispatchEvent(new Event('change', { bubbles: true }));
    fixture.detectChanges();
    expect(Array.from(municipio.options).map((option) => option.value)).toContain('Calarcá');
    expect(Array.from(municipio.options).map((option) => option.value)).not.toContain('Medellín');

    municipio.value = 'Calarcá';
    municipio.dispatchEvent(new Event('change', { bubbles: true }));
    departamento.value = 'Antioquia';
    departamento.dispatchEvent(new Event('change', { bubbles: true }));
    fixture.detectChanges();

    expect(municipio.value).toBe('');
    expect(Array.from(municipio.options).map((option) => option.value)).toContain('Medellín');
    expect(Array.from(municipio.options).map((option) => option.value)).not.toContain('Calarcá');
  });
});
