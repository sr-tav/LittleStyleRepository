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

  it('envía filtros startDate, endDate y categoryId en query params', () => {
    service.consultar('2026-10-01', '2026-10-31', 2).subscribe();

    const req = httpMock.expectOne((request) =>
      request.url.includes('/api/v1/dashboard/overview') &&
      request.params.get('startDate') === '2026-10-01' &&
      request.params.get('endDate') === '2026-10-31' &&
      request.params.get('categoryId') === '2',
    );

    expect(req.request.method).toBe('GET');
    req.flush({
      totalVentas: 3200,
      totalPedidos: 12,
      ticketPromedio: 266.67,
      productosVendidos: 44,
      margenBruto: 400,
      stockTotal: 120,
      stockBajo: 5,
      ventasPorCategoria: [],
      demandaPorTalla: [],
      productosPopulares: [],
      busquedasMasFrecuentes: [],
    });
  });
});

