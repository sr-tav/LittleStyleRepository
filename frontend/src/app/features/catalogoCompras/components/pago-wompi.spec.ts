import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { NEVER, of, throwError } from 'rxjs';

import { PagoService } from '../services/pago.service';
import { WompiWidgetService } from '../services/wompi-widget.service';
import { PagoWompi } from './pago-wompi';

describe('PagoWompi', () => {
  const inicio = {
    pedidoId: 55,
    referencia: 'LS-55-ABC',
    montoCentavos: 7280000,
    moneda: 'COP',
    llavePublica: 'pub_test',
    firmaIntegridad: 'firma',
    urlRedireccion: null,
    emailCliente: null,
    nombreCliente: null,
    telefonoCliente: null,
  };
  const pagoService = { iniciar: vi.fn() };
  const widget = { abrir: vi.fn() };

  beforeEach(async () => {
    pagoService.iniciar.mockReset().mockReturnValue(of(inicio));
    widget.abrir.mockReset();
    await TestBed.configureTestingModule({
      imports: [PagoWompi],
      providers: [
        provideRouter([]),
        { provide: PagoService, useValue: pagoService },
        { provide: WompiWidgetService, useValue: widget },
      ],
    }).compileComponents();
  });

  function crear(etiqueta?: string) {
    const fixture = TestBed.createComponent(PagoWompi);
    fixture.componentRef.setInput('pedidoId', 55);
    if (etiqueta) fixture.componentRef.setInput('etiqueta', etiqueta);
    fixture.detectChanges();
    return { fixture, root: fixture.nativeElement as HTMLElement };
  }

  it('inicia el pago, abre Wompi y navega al resultado con el id de la transacción', () => {
    widget.abrir.mockReturnValue(of('wompi-1'));
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const { root } = crear();

    root.querySelector<HTMLButtonElement>('button')?.click();

    expect(pagoService.iniciar).toHaveBeenCalledWith(55);
    expect(widget.abrir).toHaveBeenCalledWith(inicio, expect.any(Function));
    expect(navigate).toHaveBeenCalledWith(['/cliente/pago/resultado'], { queryParams: { id: 'wompi-1' } });
  });

  it('libera el botón cuando el widget se abre aunque el cliente lo cierre sin pagar', () => {
    widget.abrir.mockImplementation((_inicio: unknown, alAbrir: () => void) => {
      alAbrir();
      return NEVER;
    });
    const { fixture, root } = crear('Reintentar pago');

    root.querySelector<HTMLButtonElement>('button')?.click();
    fixture.detectChanges();

    const boton = root.querySelector<HTMLButtonElement>('button');
    expect(boton?.disabled).toBe(false);
    expect(boton?.textContent).toContain('Reintentar pago');
  });

  it('muestra el error del backend si no se puede iniciar el pago', () => {
    pagoService.iniciar.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 409,
      error: { mensaje: 'El pedido no está pendiente de pago' },
    })));
    const { fixture, root } = crear();

    root.querySelector<HTMLButtonElement>('button')?.click();
    fixture.detectChanges();

    expect(root.querySelector('[role="alert"]')).not.toBeNull();
    expect(widget.abrir).not.toHaveBeenCalled();
    expect(root.querySelector<HTMLButtonElement>('button')?.disabled).toBe(false);
  });

  it('muestra el mensaje si el script de Wompi no carga', () => {
    widget.abrir.mockReturnValue(throwError(() => new Error('No fue posible cargar la pasarela de pagos.')));
    const { fixture, root } = crear();

    root.querySelector<HTMLButtonElement>('button')?.click();
    fixture.detectChanges();

    expect(root.querySelector('[role="alert"]')?.textContent)
      .toContain('No fue posible cargar la pasarela de pagos.');
  });
});
