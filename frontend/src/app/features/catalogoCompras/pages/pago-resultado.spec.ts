import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { PerfilesService } from '../../perfilesInfantiles/services/perfiles.service';
import { ResultadoPago } from '../models/pago.models';
import { CarritoService } from '../services/carrito.service';
import { PagoService } from '../services/pago.service';
import { INTERVALO_CONSULTA_MS, PagoResultadoPage } from './pago-resultado';

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
  const pagoService = { verificar: vi.fn(), iniciar: vi.fn() };
  const carrito = {
    agregar: vi.fn(() => of({ items: [], cantidad: 0, subtotal: 0, costoEnvio: null, total: null })),
    items: vi.fn(() => []),
    cantidad: vi.fn(() => 0),
    panelAbierto: vi.fn(() => false),
  };
  let transaccionId: string | null = 'wompi-1';

  beforeEach(async () => {
    vi.useFakeTimers();
    transaccionId = 'wompi-1';
    pagoService.verificar.mockReset();
    carrito.agregar.mockClear();
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

  it('permite reintentar si el pago fue rechazado y el pedido sigue pendiente', () => {
    pagoService.verificar.mockReturnValue(of({ ...base, estadoPago: 'RECHAZADA', estadoPedido: 'PENDIENTE_PAGO' }));
    const { root } = crear();

    expect(root.textContent).toContain('Tu pago no fue aprobado');
    expect(root.querySelector('app-pago-wompi button')?.textContent).toContain('Reintentar pago');
  });

  it('avisa las prendas agotadas, el reembolso y permite continuar sin ellas', () => {
    pagoService.verificar.mockReturnValue(of({
      ...base,
      estadoPago: 'REEMBOLSO_PENDIENTE',
      estadoPedido: 'CANCELADO',
      motivoReembolso: 'STOCK_INSUFICIENTE',
      prendasAgotadas: [{ prendaId: 20, nombre: 'Pantalón Jogger', talla: '4', cantidadSolicitada: 2, stockDisponible: 0 }],
      lineasDisponibles: [{ prendaId: 21, nombre: 'Camiseta', talla: '6', cantidad: 1 }],
    }));
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const { root } = crear();

    expect(root.textContent).toContain('Algunas prendas se agotaron mientras pagabas');
    expect(root.textContent).toContain('Pantalón Jogger');
    expect(root.textContent).toContain('tu pago será reembolsado');

    const continuar = [...root.querySelectorAll<HTMLButtonElement>('button')]
      .find((boton) => boton.textContent?.includes('Continuar sin las prendas agotadas'));
    continuar?.click();

    expect(carrito.agregar).toHaveBeenCalledWith({ prendaId: 21, talla: '6', cantidad: 1 });
    expect(navigate).toHaveBeenCalledWith(['/cliente/checkout']);
  });

  it('informa cuando no llega el identificador de la transacción', () => {
    transaccionId = null;
    const { root } = crear();

    expect(pagoService.verificar).not.toHaveBeenCalled();
    expect(root.querySelector('[role="alert"]')?.textContent).toContain('No encontramos la transacción');
  });
});
