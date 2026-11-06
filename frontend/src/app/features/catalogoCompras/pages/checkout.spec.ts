import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { Carrito, ItemCarrito } from '../models/carrito.models';
import { DireccionClienteGuardada, DireccionEnvio } from '../models/checkout.models';
import { CarritoService } from '../services/carrito.service';
import { CheckoutService } from '../services/checkout.service';
import { PagoService } from '../services/pago.service';
import { WompiWidgetService } from '../services/wompi-widget.service';
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
  const direccionPrueba: DireccionEnvio = {
    destinatario: 'Ana Ruiz',
    direccion: 'Calle 1 # 2 - 3',
    tipoVia: 'Calle',
    numeroVia: '1',
    numeroCruce: '2',
    placa: '3',
    complemento: null,
    codigoPostal: '630001',
    departamento: 'Quindío',
    municipio: 'Armenia',
    telefono: '3001234567',
  };

  const primera: DireccionEnvio = { ...direccionPrueba };

  const segunda: DireccionEnvio = {
    ...direccionPrueba,
    direccion: 'Carrera 12 # 34 - 56',
    tipoVia: 'Carrera',
    numeroVia: '12',
    numeroCruce: '34',
    placa: '56',
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
  };
  const pagoService = { iniciar: vi.fn(() => of({
    pedidoId: null,
    referencia: 'LS-C42-ABC',
    montoCentavos: 7280000,
    moneda: 'COP',
    llavePublica: 'pub_test',
    firmaIntegridad: 'firma',
    urlRedireccion: null,
    emailCliente: null,
    nombreCliente: null,
    telefonoCliente: null,
  })) };
  const widget = { abrir: vi.fn(() => of({ tipo: 'transaccion' as const, id: 'wompi-1' })) };

  beforeEach(async () => {
    carritoItems = [item];
    carritoMock.items.mockImplementation(() => carritoItems);
    carritoMock.cargar.mockImplementation(() => of(carritoRespuesta()));
    checkout.calcularResumen.mockClear();
    checkout.listarDirecciones.mockReset().mockReturnValue(of([]));
    checkout.crearDireccion.mockClear();
    checkout.actualizarDireccion.mockClear();
    checkout.eliminarDireccion.mockClear();
    checkout.marcarDireccionPredeterminada.mockClear();
    pagoService.iniciar.mockClear();
    widget.abrir.mockClear();
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [CheckoutPage],
      providers: [
        provideRouter([]),
        { provide: CarritoService, useValue: carritoMock },
        { provide: CheckoutService, useValue: checkout },
        { provide: PerfilesService, useValue: { listar: () => of([perfil]) } },
        { provide: PagoService, useValue: pagoService },
        { provide: WompiWidgetService, useValue: widget },
      ],
    })
      .compileComponents();
  });

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
    listarDirecciones: vi.fn((): Observable<DireccionClienteGuardada[]> => of([])),
    crearDireccion: vi.fn((direccion: DireccionEnvio) =>
      of({ id: 1, direccion, predeterminada: false })),
    actualizarDireccion: vi.fn((id: number, direccion: DireccionEnvio) =>
      of({ id, direccion, predeterminada: false })),
    eliminarDireccion: vi.fn((_id: number) => of(void 0)),
    marcarDireccionPredeterminada: vi.fn((id: number) =>
      of({ id, direccion: direccionPrueba, predeterminada: true })),
  };


  it('usa el perfil activo sin pedir seleccionarlo y presenta UI-12 con el pago habilitado', () => {
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
    llenar('#direccion', 'Calle 1 # 2 - 3');
    llenar('#complemento', 'Apto. 302, torre 1');
    llenar('#departamento', 'Quindío');
    llenar('#municipio', 'Armenia');
    llenar('#codigoPostal', '630001');
    llenar('#telefono', '1234567890');
    root.querySelector<HTMLFormElement>('form')?.dispatchEvent(new Event('submit', { bubbles: true }));
    fixture.detectChanges();

    expect(checkout.calcularResumen).not.toHaveBeenCalled();
    expect(root.textContent).toContain('celular colombiano válido');

    llenar('#telefono', '3001234567');
    llenar('#codigoPostal', '');
    root.querySelector<HTMLFormElement>('form')?.dispatchEvent(new Event('submit', { bubbles: true }));
    fixture.detectChanges();

    expect(checkout.calcularResumen).not.toHaveBeenCalled();
    expect(root.textContent).toContain('Selecciona el código postal del municipio');

    llenar('#codigoPostal', '630001');
    llenar('#telefono', '3001234567');
    const form = root.querySelector<HTMLFormElement>('form');
    if (!form) throw new Error('No se encontró el formulario de checkout');
    const formSubmit = vi.fn();
    form.addEventListener('submit', formSubmit);
    form.querySelector<HTMLButtonElement>('button.btn-primary')?.click();
    fixture.detectChanges();

    expect(formSubmit).not.toHaveBeenCalled();
    expect(checkout.calcularResumen).toHaveBeenCalledWith({
      perfilInfantilId: 3,
      direccion: {
        destinatario: 'Ana Ruiz',
        direccion: 'Calle 1 # 2 - 3',
        tipoVia: 'Calle',
        numeroVia: '1',
        numeroCruce: '2',
        placa: '3',
        complemento: 'Apto. 302, torre 1',
        codigoPostal: '630001',
        departamento: 'Quindío',
        municipio: 'Armenia',
        telefono: '3001234567',
      },
    });

    expect(checkout.crearDireccion).toHaveBeenCalledWith({
      destinatario: 'Ana Ruiz',
      direccion: 'Calle 1 # 2 - 3',
      tipoVia: 'Calle',
      numeroVia: '1',
      numeroCruce: '2',
      placa: '3',
      complemento: 'Apto. 302, torre 1',
      codigoPostal: '630001',
      departamento: 'Quindío',
      municipio: 'Armenia',
      telefono: '3001234567',
    });
    expect(root.textContent).toContain('Resumen de la compra');
    expect(root.textContent).toContain('Calle 1 # 2 - 3');
    expect(root.textContent).toContain('72.800');
    const botonPago = root.querySelector<HTMLButtonElement>('.carrito-resumen app-pago-wompi button');
    expect(botonPago?.textContent).toContain('Pagar ahora');
    expect(botonPago?.disabled).toBe(false);
    expect(root.textContent).not.toContain('¡Pedido creado!');

    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    botonPago?.click();
    expect(pagoService.iniciar).toHaveBeenCalledWith({
      perfilInfantilId: 3,
      direccion: {
        destinatario: 'Ana Ruiz',
        direccion: 'Calle 1 # 2 - 3',
        tipoVia: 'Calle',
        numeroVia: '1',
        numeroCruce: '2',
        placa: '3',
        complemento: 'Apto. 302, torre 1',
        codigoPostal: '630001',
        departamento: 'Quindío',
        municipio: 'Armenia',
        telefono: '3001234567',
      },
    }, expect.any(String));
    expect(widget.abrir).toHaveBeenCalled();
    expect(navegar).toHaveBeenCalledWith(['/cliente/pago/resultado'], { queryParams: { id: 'wompi-1' } });
  });

  it('solo muestra el desplegable postal después de elegir departamento y municipio', () => {
    const fixture = TestBed.createComponent(CheckoutPage);
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    expect(root.textContent).toContain('Selecciona primero un departamento y municipio.');
    expect(root.querySelector('#codigoPostal')).toBeNull();

    const elegir = (selector: string, value: string) => {
      const control = root.querySelector<HTMLSelectElement>(selector);
      if (!control) throw new Error(`No existe el campo ${selector}`);
      control.value = value;
      control.dispatchEvent(new Event('change', { bubbles: true }));
      fixture.detectChanges();
    };

    elegir('#departamento', 'Quindío');
    expect(root.querySelector('#codigoPostal')).toBeNull();
    elegir('#municipio', 'Armenia');

    expect(root.querySelector('select#codigoPostal')).not.toBeNull();
    expect(root.querySelector('input#codigoPostal')).toBeNull();
    expect(Array.from(root.querySelectorAll<HTMLOptionElement>('#codigoPostal option'))
      .map((option) => option.value).filter(Boolean))
      .toContain('630001');
  });

  it('permite reutilizar la dirección de cuenta y editarla', () => {
    const direccionConComplemento: DireccionEnvio = {
      ...direccionPrueba,
      complemento: 'Apto. 302',
    };

    checkout.listarDirecciones.mockReturnValue(of([
      {
        id: 1,
        direccion: direccionConComplemento,
        predeterminada: true,
      },
    ]));

    const fixture = TestBed.createComponent(CheckoutPage);
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;

    expect(root.textContent).toContain('Elige una dirección para esta compra');
    expect(root.querySelector<HTMLInputElement>(
      'input[name="direccionSeleccionada"]',
    )?.checked).toBe(true);
    expect(root.querySelector<HTMLInputElement>('#direccion')).toBeNull();

    Array.from(root.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('Editar dirección'))
      ?.click();
    fixture.detectChanges();

    expect(root.querySelector<HTMLInputElement>('#direccion')?.value).toBe('Calle 1 # 2 - 3');
    expect(root.querySelector<HTMLInputElement>('#complemento')?.value).toBe('Apto. 302');
    expect(root.querySelector('#tipoVia')).toBeNull();
    expect(root.querySelector('#numeroVia')).toBeNull();
    expect(root.querySelector('#numeroCruce')).toBeNull();
    expect(root.querySelector('#placa')).toBeNull();
  });

  it('no selecciona una dirección por defecto y usa la elegida para el resumen', () => {
    const primera: DireccionEnvio = {
      destinatario: 'Ana Ruiz',
      direccion: 'Calle 1 # 2 - 3',
      tipoVia: 'Calle',
      numeroVia: '1',
      numeroCruce: '2',
      placa: '3',
      complemento: null,
      codigoPostal: '630001',
      departamento: 'Quindío',
      municipio: 'Armenia',
      telefono: '3001234567',
    };
    const segunda: DireccionEnvio = {
      ...primera,
      direccion: 'Carrera 12 # 34 - 56',
      tipoVia: 'Carrera',
      numeroVia: '12',
      numeroCruce: '34',
      placa: '56',
    };
    checkout.listarDirecciones.mockReturnValue(of([
      { id: 1, direccion: primera, predeterminada: false },
      { id: 2, direccion: segunda, predeterminada: true },
    ]));
    const fixture = TestBed.createComponent(CheckoutPage);
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    const radios = root.querySelectorAll<HTMLInputElement>('input[name="direccionSeleccionada"]');
    const selectorDireccion = radios[0]?.closest('label');
    const continuar = Array.from(root.querySelectorAll<HTMLButtonElement>('button'))
      .find((button) => button.textContent?.includes('Continuar con la dirección elegida'));
    const agregar = Array.from(root.querySelectorAll<HTMLButtonElement>('button'))
      .find((button) => button.textContent?.includes('Agregar otra dirección'));
    if (!continuar) throw new Error('No se encontró el botón para continuar');
    if (!agregar) throw new Error('No se encontró el botón para agregar dirección');

    expect(selectorDireccion?.classList.contains('btn-ghost')).toBe(true);
    expect(selectorDireccion?.textContent).toContain('Seleccionar para esta compra');
    expect(continuar.parentElement).toBe(agregar.parentElement);
    expect(Array.from(radios).map((radio) => radio.checked)).toEqual([false, true]);
    expect(continuar.disabled).toBe(false);
    expect(checkout.calcularResumen).not.toHaveBeenCalled();

    radios[1].checked = true;
    radios[1].dispatchEvent(new Event('change', { bubbles: true }));
    fixture.detectChanges();
    continuar.click();
    fixture.detectChanges();

    expect(checkout.calcularResumen).toHaveBeenCalledWith({
      perfilInfantilId: 3,
      direccion: segunda,
    });
  });

  it('muestra una indicación clara si una prenda dejó de estar disponible al calcular el checkout', () => {
    const mensaje = 'Una prenda del carrito ya no está disponible. Vuelve al carrito para retirarla o elegir otra antes de continuar.';
    checkout.calcularResumen.mockReturnValueOnce(throwError(() => new HttpErrorResponse({
      status: 404,
      error: { mensaje },
    })));
    checkout.listarDirecciones.mockReturnValue(of([
      { id: 1, direccion: primera, predeterminada: true },
      { id: 2, direccion: segunda, predeterminada: false },
    ]));
    const fixture = TestBed.createComponent(CheckoutPage);
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    const seleccion = root.querySelector<HTMLInputElement>('input[name="direccionSeleccionada"]');
    if (!seleccion) throw new Error('No se encontró el selector de dirección');
    seleccion.checked = true;
    seleccion.dispatchEvent(new Event('change', { bubbles: true }));
    fixture.detectChanges();
    Array.from(root.querySelectorAll<HTMLButtonElement>('button'))
      .find((button) => button.textContent?.includes('Continuar con la dirección elegida'))
      ?.click();
    fixture.detectChanges();

    expect(root.querySelector('[role="alert"]')?.textContent).toContain(mensaje);
    expect(root.querySelector('app-pago-wompi')).toBeNull();
  });

  it('no inicia el pago cuando el carrito ya contiene productos agotados', () => {
    const agotado: ItemCarrito = { ...item, nombre: 'Pantalón de lino', disponible: false };
    carritoItems = [agotado];
    carritoMock.items.mockImplementation(() => carritoItems);
    checkout.listarDirecciones.mockReturnValue(of([
      { id: 1, direccion: primera, predeterminada: true },
      { id: 2, direccion: segunda, predeterminada: false },
    ]));
    const fixture = TestBed.createComponent(CheckoutPage);
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    const seleccion = root.querySelector<HTMLInputElement>('input[name="direccionSeleccionada"]');
    if (!seleccion) throw new Error('No se encontró el selector de dirección');
    seleccion.checked = true;
    seleccion.dispatchEvent(new Event('change', { bubbles: true }));
    fixture.detectChanges();
    Array.from(root.querySelectorAll<HTMLButtonElement>('button'))
      .find((button) => button.textContent?.includes('Continuar con la dirección elegida'))
      ?.click();
    fixture.detectChanges();

    expect(root.textContent).toContain('Pantalón Jogger');
    expect(root.textContent).toContain('Vuelve al carrito para ajustar las cantidades');
    expect(root.querySelector('app-pago-wompi')).toBeNull();
    expect(pagoService.iniciar).not.toHaveBeenCalled();
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
  it('una dirección guardada sin estructura obliga a completarla', () => {
    checkout.listarDirecciones.mockReturnValue(of([{
      id: 1,
      direccion: {
        ...direccionPrueba,
        direccion: 'Calle vieja 1',
        tipoVia: null,
        numeroVia: null,
        numeroCruce: null,
        placa: null,
      },
      predeterminada: true,
    }]));
    const fixture = TestBed.createComponent(CheckoutPage);
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;

    root.querySelector<HTMLButtonElement>('.checkout-direccion-guardada button')?.click();
    fixture.detectChanges();
    expect(root.querySelector<HTMLInputElement>('#direccion')?.value).toBe('Calle vieja 1');
    expect(root.querySelector('#tipoVia')).toBeNull();
    expect(root.querySelector('#numeroVia')).toBeNull();
  });

  it('rechaza una dirección inválida y muestra el ejemplo del formato esperado', () => {
    const fixture = TestBed.createComponent(CheckoutPage);
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    const direccion = root.querySelector<HTMLInputElement>('#direccion')!;
    direccion.value = 'Calle ABC';
    direccion.dispatchEvent(new Event('input', { bubbles: true }));
    root.querySelector<HTMLFormElement>('form')?.dispatchEvent(new Event('submit', { bubbles: true }));
    fixture.detectChanges();

    expect(root.textContent).toContain('Ingresa una dirección válida. Ejemplo: Carrera 12 # 34-56.');
    expect(direccion.getAttribute('aria-invalid')).toBe('true');
    expect(checkout.calcularResumen).not.toHaveBeenCalled();

    direccion.value = 'Carrera 13 # 7 - 8';
    direccion.dispatchEvent(new Event('input', { bubbles: true }));
    fixture.detectChanges();
    expect(direccion.getAttribute('aria-invalid')).toBeNull();
  });

  it('marca una dirección como predeterminada y actualiza las tarjetas', () => {
    const direccionDos: DireccionEnvio = {
      ...direccionPrueba,
      direccion: 'Carrera 12 # 34 - 56',
    };
    checkout.listarDirecciones.mockReturnValue(of([
      { id: 1, direccion: direccionPrueba, predeterminada: true },
      { id: 2, direccion: direccionDos, predeterminada: false },
    ]));
    checkout.marcarDireccionPredeterminada.mockReturnValue(of({
      id: 2,
      direccion: direccionDos,
      predeterminada: true,
    }));

    const fixture = TestBed.createComponent(CheckoutPage);
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    const tarjetas = root.querySelectorAll<HTMLElement>('.checkout-direccion-guardada');

    Array.from(tarjetas[1].querySelectorAll<HTMLButtonElement>('button'))
      .find((button) => button.textContent?.includes('Marcar predeterminada'))!
      .click();
    fixture.detectChanges();

    expect(checkout.marcarDireccionPredeterminada).toHaveBeenCalledWith(2);
    expect(tarjetas[0].textContent).not.toContain('Dirección predeterminada');
    expect(tarjetas[1].textContent).toContain('Dirección predeterminada');
  });

  it('cancela la eliminación sin llamar al backend y elimina al confirmar', () => {
    const direccionDos: DireccionEnvio = {
      ...direccionPrueba,
      direccion: 'Carrera 12 # 34 - 56',
    };
    checkout.listarDirecciones.mockReturnValue(of([
      { id: 1, direccion: direccionPrueba, predeterminada: true },
      { id: 2, direccion: direccionDos, predeterminada: false },
    ]));

    const fixture = TestBed.createComponent(CheckoutPage);
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    const tarjeta = root.querySelectorAll<HTMLElement>('.checkout-direccion-guardada')[1];

    Array.from(tarjeta.querySelectorAll<HTMLButtonElement>('button'))
      .find((button) => button.textContent?.includes('Eliminar dirección'))!
      .click();
    fixture.detectChanges();

    expect(tarjeta.textContent).toContain('¿Eliminar esta dirección guardada?');
    expect(checkout.eliminarDireccion).not.toHaveBeenCalled();

    Array.from(tarjeta.querySelectorAll<HTMLButtonElement>('button'))
      .find((button) => button.textContent?.trim() === 'Cancelar')!
      .click();
    fixture.detectChanges();
    expect(checkout.eliminarDireccion).not.toHaveBeenCalled();

    Array.from(tarjeta.querySelectorAll<HTMLButtonElement>('button'))
      .find((button) => button.textContent?.includes('Eliminar dirección'))!
      .click();
    fixture.detectChanges();
    Array.from(tarjeta.querySelectorAll<HTMLButtonElement>('button'))
      .find((button) => button.textContent?.includes('Sí, eliminar'))!
      .click();
    fixture.detectChanges();

    expect(checkout.eliminarDireccion).toHaveBeenCalledWith(2);
    expect(root.querySelectorAll('.checkout-direccion-guardada')).toHaveLength(1);
  });

  it('mantiene la dirección y anuncia el error si falla su eliminación', () => {
    const direccionDos: DireccionEnvio = {
      ...direccionPrueba,
      direccion: 'Carrera 12 # 34 - 56',
    };
    checkout.listarDirecciones.mockReturnValue(of([
      { id: 1, direccion: direccionPrueba, predeterminada: true },
      { id: 2, direccion: direccionDos, predeterminada: false },
    ]));
    checkout.eliminarDireccion.mockReturnValue(
      throwError(() => new HttpErrorResponse({
        status: 404,
        error: { mensaje: 'Dirección no encontrada' },
      })),
    );

    const fixture = TestBed.createComponent(CheckoutPage);
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    const tarjeta = root.querySelectorAll<HTMLElement>('.checkout-direccion-guardada')[1];

    Array.from(tarjeta.querySelectorAll<HTMLButtonElement>('button'))
      .find((button) => button.textContent?.includes('Eliminar dirección'))!
      .click();
    fixture.detectChanges();
    Array.from(tarjeta.querySelectorAll<HTMLButtonElement>('button'))
      .find((button) => button.textContent?.includes('Sí, eliminar'))!
      .click();
    fixture.detectChanges();

    expect(root.querySelectorAll('.checkout-direccion-guardada')).toHaveLength(2);
    expect(root.querySelector('[role="alert"]')?.textContent).toContain('Dirección no encontrada');
  });
});
