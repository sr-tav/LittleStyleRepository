import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { PerfilesService } from '../../perfilesInfantiles/services/perfiles.service';
import { ResultadoPago } from '../models/pago.models';
import { CarritoService } from '../services/carrito.service';
import { PagoService } from '../services/pago.service';
import { INTERVALO_CONSULTA_MS, MAX_CONSULTAS, PagoResultadoPage } from './pago-resultado';

describe('PagoResultadoPage', () => {
  const base: ResultadoPago = {
    pedidoId: 55,
    referencia: 'LS-55-ABC',
    estadoPago: 'APROBADA',
    estadoPedido: 'PAGADO',
    motivoReembolso: null,
    metodoPago: 'CARD',
    total: 72800,
    prendasAgotadas: [],
    lineasDisponibles: [],
  };
  const pagoService = { verificar: vi.fn() };
  const carrito = {
    cargar: vi.fn(() => of({ items: [], cantidad: 0, subtotal: 0, costoEnvio: null, total: null })),
    panelAbierto: signal(false),
    items: signal([]),
    cantidad: signal(0),
    subtotal: signal(0),
    alternarPanel: vi.fn(),
    cerrarPanel: vi.fn(),
  };
  let transaccionId: string | null = 'wompi-1';

  beforeEach(async () => {
    vi.useFakeTimers();
    transaccionId = 'wompi-1';
    pagoService.verificar.mockReset();
    carrito.cargar.mockClear();
    await TestBed.configureTestingModule({
      imports: [PagoResultadoPage],
      providers: [
        provideRouter([]),
        { provide: PagoService, useValue: pagoService },
        { provide: CarritoService, useValue: carrito },
        { provide: PerfilesService, useValue: { listar: () => of([]) } },
        {
          provide: ActivatedRoute,
          useFactory: () => ({
            snapshot: {
              queryParamMap: convertToParamMap(transaccionId ? { id: transaccionId } : {}),
            },
          }),
        },
      ],
    }).compileComponents();
  });

  afterEach(() => vi.useRealTimers());

  function crear() {
    const fixture = TestBed.createComponent(PagoResultadoPage);
    fixture.detectChanges();
    vi.advanceTimersByTime(0);
    fixture.detectChanges();
    return { fixture, root: fixture.nativeElement as HTMLElement };
  }

  it('confirma el pago aprobado con el número de pedido', () => {
    pagoService.verificar.mockReturnValue(of(base));
    const { root } = crear();

    expect(pagoService.verificar).toHaveBeenCalledWith('wompi-1');
    expect(root.textContent).toContain('¡Pago aprobado!');
    expect(root.textContent).toContain('#55');
    expect(root.textContent).toContain('LS-55-ABC');
    expect(carrito.cargar).toHaveBeenCalledTimes(1);
  });

  it('vuelve a consultar mientras Wompi mantiene el pago pendiente', () => {
    pagoService.verificar
      .mockReturnValueOnce(of({ ...base, estadoPago: 'PENDIENTE', estadoPedido: 'PENDIENTE_PAGO' }))
      .mockReturnValueOnce(of(base));
    const { fixture, root } = crear();
    expect(root.textContent).toContain('Tu pago está en proceso');

    vi.advanceTimersByTime(INTERVALO_CONSULTA_MS);
    fixture.detectChanges();

    expect(pagoService.verificar).toHaveBeenCalledTimes(2);
    expect(root.textContent).toContain('¡Pago aprobado!');
    vi.advanceTimersByTime(INTERVALO_CONSULTA_MS * 3);
    expect(pagoService.verificar).toHaveBeenCalledTimes(2);
  });

  it('informa que el carrito se conserva cuando el pago es rechazado y aún no hay pedido', () => {
    pagoService.verificar.mockReturnValue(of({
      ...base,
      pedidoId: null,
      estadoPago: 'RECHAZADA',
      estadoPedido: null,
    }));
    const { root } = crear();

    expect(root.textContent).toContain('Tu pago no fue aprobado');
    expect(root.textContent).toContain('no se creó el pedido');
    expect(root.textContent).toContain('Los productos siguen en tu carrito');
    expect(root.querySelector<HTMLAnchorElement>('a[routerLink="/cliente/checkout"]')).not.toBeNull();
  });

  it('sigue consultando si una verificación falla de forma momentánea (504)', () => {
    const timeout = new HttpErrorResponse({ status: 504, error: { mensaje: 'Wompi no respondió a tiempo' } });
    pagoService.verificar
      .mockReturnValueOnce(throwError(() => timeout))
      .mockReturnValueOnce(of(base));
    const { fixture, root } = crear();
    expect(root.querySelector('[role="alert"]')).toBeNull();

    vi.advanceTimersByTime(INTERVALO_CONSULTA_MS);
    fixture.detectChanges();

    expect(pagoService.verificar).toHaveBeenCalledTimes(2);
    expect(root.textContent).toContain('¡Pago aprobado!');
  });

  it('muestra el error si todas las consultas fallan de forma momentánea', () => {
    const timeout = new HttpErrorResponse({ status: 504, error: { mensaje: 'Wompi no respondió a tiempo' } });
    pagoService.verificar.mockReturnValue(throwError(() => timeout));
    const { fixture, root } = crear();

    vi.advanceTimersByTime(INTERVALO_CONSULTA_MS * MAX_CONSULTAS);
    fixture.detectChanges();

    expect(pagoService.verificar).toHaveBeenCalledTimes(MAX_CONSULTAS);
    expect(root.querySelector('[role="alert"]')?.textContent).toContain('Wompi no respondió a tiempo');
  });

  it('detiene las consultas ante un error que no es momentáneo', () => {
    const noEncontrada = new HttpErrorResponse({ status: 404, error: { mensaje: 'Transacción no encontrada' } });
    pagoService.verificar.mockReturnValue(throwError(() => noEncontrada));
    const { fixture, root } = crear();

    vi.advanceTimersByTime(INTERVALO_CONSULTA_MS * 3);
    fixture.detectChanges();

    expect(pagoService.verificar).toHaveBeenCalledTimes(1);
    expect(root.querySelector('[role="alert"]')?.textContent).toContain('Transacción no encontrada');
  });

  it('permite reintentar si el pago fue rechazado y el pedido sigue pendiente', () => {
    pagoService.verificar.mockReturnValue(of({ ...base, estadoPago: 'RECHAZADA', estadoPedido: 'PENDIENTE_PAGO' }));
    const { root } = crear();

    expect(root.textContent).toContain('Tu pago no fue aprobado');
    expect(root.textContent).toContain('no se creó el pedido');
    expect(root.textContent).toContain('Los productos siguen en tu carrito');
    expect(root.querySelector<HTMLAnchorElement>('a[routerLink="/cliente/checkout"]')).not.toBeNull();
  });

  it('avisa las prendas agotadas, el reembolso y mantiene el carrito para revisión', () => {
    pagoService.verificar.mockReturnValue(of({
      ...base,
      estadoPago: 'REEMBOLSO_PENDIENTE',
      estadoPedido: 'CANCELADO',
      motivoReembolso: 'STOCK_INSUFICIENTE',
      prendasAgotadas: [{ prendaId: 20, nombre: 'Pantalón Jogger', talla: '4', cantidadSolicitada: 2, stockDisponible: 0 }],
      lineasDisponibles: [{ prendaId: 21, nombre: 'Camiseta', talla: '6', cantidad: 1 }],
    }));
    const { root } = crear();

    expect(root.textContent).toContain('Algunas prendas se agotaron mientras pagabas');
    expect(root.textContent).toContain('Pantalón Jogger');
    expect(root.textContent).toContain('tu pago será reembolsado');

    expect(root.textContent).toContain('Los productos siguen en tu carrito');
    expect(root.querySelector<HTMLAnchorElement>('a[routerLink="/cliente/carrito"]')).not.toBeNull();
  });

  it('informa cuando no llega el identificador de la transacción', () => {
    transaccionId = null;
    const { root } = crear();

    expect(pagoService.verificar).not.toHaveBeenCalled();
    expect(root.querySelector('[role="alert"]')?.textContent).toContain('No encontramos la transacción');
  });
});
