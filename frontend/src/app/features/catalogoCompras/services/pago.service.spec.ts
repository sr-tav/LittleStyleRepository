import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { HttpErrorResponse } from '@angular/common/http';

import { environment } from '../../../../environments/environment';
import { MENSAJE_SIN_RESPUESTA, PagoService, TIEMPO_MAXIMO_PAGO_MS } from './pago.service';

describe('PagoService', () => {
  let http: HttpTestingController;
  let service: PagoService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    service = TestBed.inject(PagoService);
  });

  afterEach(() => {
    vi.useRealTimers();
    http.verify();
  });

  it('solicita al backend los datos firmados para pagar un pedido', () => {
    let referencia = '';
    service.iniciar(55).subscribe((inicio) => (referencia = inicio.referencia));

    const req = http.expectOne(`${environment.apiUrl}/cliente/pagos/pedidos/55`);
    expect(req.request.method).toBe('POST');
    req.flush({ pedidoId: 55, referencia: 'LS-55-ABC', montoCentavos: 7280000 });

    expect(referencia).toBe('LS-55-ABC');
  });

  it('verifica una transacción de Wompi por su identificador', () => {
    let estado = '';
    service.verificar('wompi-1').subscribe((resultado) => (estado = resultado.estadoPago));

    const req = http.expectOne(`${environment.apiUrl}/cliente/pagos/verificar`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ transaccionId: 'wompi-1' });
    req.flush({ estadoPago: 'APROBADA' });

    expect(estado).toBe('APROBADA');
  });

  it('corta la consulta si no responde a tiempo y avisa que se intente más tarde', () => {
    vi.useFakeTimers();
    let error: unknown;
    service.verificar('wompi-1').subscribe({ error: (e: unknown) => (error = e) });
    const req = http.expectOne(`${environment.apiUrl}/cliente/pagos/verificar`);

    vi.advanceTimersByTime(TIEMPO_MAXIMO_PAGO_MS);

    expect(req.cancelled).toBe(true);
    expect(error).toBeInstanceOf(HttpErrorResponse);
    expect((error as HttpErrorResponse).status).toBe(504);
    expect((error as HttpErrorResponse).error).toEqual({ mensaje: MENSAJE_SIN_RESPUESTA });
  });
});
