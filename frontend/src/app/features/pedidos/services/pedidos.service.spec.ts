import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { PedidosService } from './pedidos.service';

describe('PedidosService (US-12)', () => {
  let http: HttpTestingController;
  let servicio: PedidosService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    servicio = TestBed.inject(PedidosService);
  });

  afterEach(() => http.verify());

  it('lista los pedidos del cliente', () => {
    servicio.listar().subscribe();
    const req = http.expectOne('/api/cliente/checkout/pedidos');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('pide el detalle por id', () => {
    servicio.detalle(7).subscribe();
    const req = http.expectOne('/api/cliente/checkout/pedidos/7');
    expect(req.request.method).toBe('GET');
    req.flush({ id: 7 });
  });

  it('cancela el pedido pendiente de pago', () => {
    servicio.cancelar(7).subscribe();
    const req = http.expectOne('/api/cliente/checkout/pedidos/7/cancelar');
    expect(req.request.method).toBe('POST');
    req.flush({ id: 7, estado: 'CANCELADO', numeroGuia: null });
  });
});
