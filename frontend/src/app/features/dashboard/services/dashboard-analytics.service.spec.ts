import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DashboardAnalyticsService } from './dashboard-analytics.service';

describe('DashboardAnalyticsService', () => {
  let service: DashboardAnalyticsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(DashboardAnalyticsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('envía startDate, endDate y el código de categoría en query params', () => {
    service.consultar('2026-10-01', '2026-10-31', 'VESTIDOS').subscribe();

    const req = httpMock.expectOne(
      (request) =>
        request.url.includes('/api/v1/dashboard/overview') &&
        request.params.get('startDate') === '2026-10-01' &&
        request.params.get('endDate') === '2026-10-31' &&
        request.params.get('categoria') === 'VESTIDOS',
    );

    expect(req.request.method).toBe('GET');
    expect(req.request.params.has('categoryId')).toBe(false);
    req.flush({
      totalVentas: 3200,
      totalPedidos: 12,
      ticketPromedio: 266.67,
      productosVendidos: 44,
      margenBruto: null,
      stockTotal: 120,
      stockBajo: 5,
      ventasPorCategoria: [],
      demandaPorTalla: [],
      productosPopulares: [],
      productosMasConsultados: [],
      busquedasMasFrecuentes: [],
    });
  });

  it('no envía categoría ni vendedor cuando no hay filtro', () => {
    service.consultar('2026-10-01', '2026-10-31').subscribe();

    const req = httpMock.expectOne((request) => request.url.includes('/api/v1/dashboard/overview'));
    expect(req.request.params.has('categoria')).toBe(false);
    expect(req.request.params.has('vendedorId')).toBe(false);
    req.flush({});
  });

  it('los tops por separado también usan el parámetro categoria', () => {
    service.consultarTopMasConsultados('2026-10-01', '2026-10-31', 'BUZOS').subscribe();
    service.consultarTopMasVendidos('2026-10-01', '2026-10-31', 'BUZOS').subscribe();

    const vistos = httpMock.expectOne((r) => r.url.includes('/top-viewed-products'));
    const vendidos = httpMock.expectOne((r) => r.url.includes('/top-sold-products'));
    expect(vistos.request.params.get('categoria')).toBe('BUZOS');
    expect(vendidos.request.params.get('categoria')).toBe('BUZOS');
    vistos.flush([]);
    vendidos.flush([]);
  });
});
