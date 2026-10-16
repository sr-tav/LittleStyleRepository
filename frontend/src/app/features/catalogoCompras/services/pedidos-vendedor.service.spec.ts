import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { PedidosVendedorService } from './pedidos-vendedor.service';

describe('PedidosVendedorService', () => {
  let http: HttpTestingController;
  let servicio: PedidosVendedorService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    servicio = TestBed.inject(PedidosVendedorService);
  });

  afterEach(() => http.verify());

  it('lista los pedidos del vendedor', () => {
    servicio.listar().subscribe();
    const req = http.expectOne('/api/vendedor/pedidos');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('avanza el estado con guía opcional', () => {
    servicio.avanzarEstado(7, 'ENVIADO', 'COL123').subscribe();
    const req = http.expectOne('/api/vendedor/pedidos/7/estado');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ nuevo: 'ENVIADO', numeroGuia: 'COL123' });
    req.flush({ id: 7 });
  });

  it('pide el detalle por id', () => {
    servicio.detalle(7).subscribe();
    const req = http.expectOne('/api/vendedor/pedidos/7');
    expect(req.request.method).toBe('GET');
    req.flush({ id: 7 });
  });
});
