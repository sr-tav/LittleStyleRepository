import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { Observable, of, Subject, throwError } from 'rxjs';

import { Carrito, ItemCarrito } from '../models/carrito.models';
import { DireccionEnvio, PedidoCreado } from '../models/checkout.models';
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
  let carritoItems: ItemCarrito[] = [];
  const carritoMock = {
    items: vi.fn(() => carritoItems),
    cargar: vi.fn((): Observable<Carrito> => of(carritoRespuesta())),
    quitar: vi.fn((itemId: number): Observable<Carrito> => {
      carritoItems = carritoItems.filter((linea) => linea.id !== itemId);
      return of(carritoRespuesta());
    }),
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
    crearPedido: vi.fn((): Observable<PedidoCreado> => of({
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
    carritoItems = [item];
    carritoMock.items.mockImplementation(() => carritoItems);
    carritoMock.cargar.mockImplementation(() => of(carritoRespuesta()));
    carritoMock.quitar.mockClear();
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
        { provide: CarritoService, useValue: carritoMock },
        { provide: CheckoutService, useValue: checkout },
        { provide: PerfilesService, useValue: { listar: () => of([perfil]) } },
      ],
    })
      .compileComponents();
  });

  it('usa el perfil activo sin pedir seleccionarlo y presenta UI-12 con el pago deshabilitado', () => {
    const pedidoCreado = {
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
    } satisfies PedidoCreado;
    const respuestaPedido = new Subject<PedidoCreado>();
    checkout.crearPedido.mockReturnValue(respuestaPedido);
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

    const botonConfirmar = root.querySelector<HTMLButtonElement>('.carrito-resumen button:not([disabled])');
    botonConfirmar?.click();
    botonConfirmar?.click();
    expect(checkout.crearPedido).toHaveBeenCalledTimes(1);
    fixture.detectChanges();
    expect(botonConfirmar?.disabled).toBe(true);
    expect(botonConfirmar?.textContent).toContain('Creando pedido…');
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
    respuestaPedido.next(pedidoCreado);
    respuestaPedido.complete();
    fixture.detectChanges();
    expect(root.textContent).not.toContain('creado y pendiente de pago');
    expect(root.textContent).toContain('¡Pedido creado!');
    expect(root.textContent).toContain('Número de pedido: #55');
    expect(root.textContent).toContain('Estado: Pendiente de pago');
    expect(root.textContent).not.toContain('El pago estará disponible próximamente.');
    const botonPago = root.querySelector<HTMLButtonElement>('.carrito-resumen button[disabled]');
    expect(botonPago?.textContent)
      .toContain('Pagar');
    expect(root.querySelector('.carrito-resumen .checkout-pago-nota')).toBeNull();
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

  it('identifica la prenda agotada y permite confirmar las demás o seguir comprando', () => {
    const pantalonAgotado: ItemCarrito = { ...item, nombre: 'Pantalón de lino', disponible: false };
    const pantalonDisponibleAlInicio: ItemCarrito = { ...pantalonAgotado, disponible: true };
    const camisetaDisponible: ItemCarrito = {
      ...item,
      id: 11,
      prendaId: 21,
      nombre: 'Camiseta de algodón',
      disponible: true,
    };
    carritoItems = [pantalonDisponibleAlInicio, camisetaDisponible];
    carritoMock.items.mockImplementation(() => carritoItems);
    carritoMock.cargar.mockImplementationOnce(() => {
      carritoItems = [pantalonAgotado, camisetaDisponible];
      return of(carritoRespuesta());
    });
    checkout.obtenerDireccion.mockReturnValue(of({
      guardada: true,
      direccion: {
        destinatario: 'Ana Ruiz',
        direccion: 'Calle 1 # 2-3',
        complemento: null,
        codigoPostal: '630001',
        departamento: 'Quindío',
        municipio: 'Armenia',
        telefono: '3001234567',
      },
    }));
    checkout.crearPedido
      .mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 409 })))
      .mockReturnValueOnce(of({
        pedidoId: 56,
        estado: 'PENDIENTE_PAGO',
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
      }));

    const fixture = TestBed.createComponent(CheckoutPage);
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    root.querySelector<HTMLButtonElement>('.checkout-direccion-guardada button')?.click();
    fixture.detectChanges();
    root.querySelector<HTMLButtonElement>('.carrito-resumen button:not([disabled])')?.click();
    fixture.detectChanges();

    expect(root.textContent).toContain('Pantalón de lino');
    expect(root.textContent).toContain('¿Quieres confirmar el pedido sin estas prendas o prefieres seguir comprando?');
    expect(root.querySelector<HTMLAnchorElement>('.checkout-stock-conflicto a')?.textContent)
      .toContain('Seguir comprando');

    root.querySelector<HTMLButtonElement>('.checkout-stock-conflicto button')?.click();
    fixture.detectChanges();
    expect(carritoMock.quitar).toHaveBeenCalledWith(pantalonAgotado.id);
    expect(carritoItems.map((linea) => linea.nombre)).toEqual(['Camiseta de algodón']);
    expect(checkout.crearPedido).toHaveBeenCalledTimes(2);
    expect(root.textContent).toContain('¡Pedido creado!');
    expect(root.textContent).toContain('#56');
    expect(root.textContent).not.toContain('Pantalón de lino');
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

  function carritoRespuesta(): Carrito {
    return {
      items: [...carritoMock.items()],
      cantidad: carritoItems.reduce((total, linea) => total + linea.cantidad, 0),
      subtotal: carritoItems.reduce((total, linea) => total + linea.subtotal, 0),
      costoEnvio: null,
      total: null,
    };
  }
});
