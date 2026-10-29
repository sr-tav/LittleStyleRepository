import { HttpEventType, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { EstadoExportacion } from '../models/reporte.models';
import { ReportesService } from './reportes.service';

describe('ReportesService', () => {
  let service: ReportesService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ReportesService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('pide el PDF con los filtros y emite generando, descargando y listo', () => {
    const estados: EstadoExportacion[] = [];
    service
      .exportar('ventas', { startDate: '2026-10-01', endDate: '2026-10-28', categoria: 'VESTIDOS' })
      .subscribe((e) => estados.push(e));

    const req = httpMock.expectOne((r) => r.url === '/api/v1/reportes/ventas');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('formato')).toBe('PDF');
    expect(req.request.params.get('startDate')).toBe('2026-10-01');
    expect(req.request.params.get('categoria')).toBe('VESTIDOS');
    expect(req.request.responseType).toBe('blob');

    // HttpClientTestingBackend emite Sent automáticamente al suscribirse.
    req.event({ type: HttpEventType.DownloadProgress, loaded: 50, total: 200 });
    req.flush(new Blob(['%PDF-1.7'], { type: 'application/pdf' }), {
      headers: { 'Content-Disposition': `attachment; filename="reporte-ventas_2026-10-01_2026-10-28.pdf"` },
    });

    expect(estados.map((e) => e.fase)).toEqual(['generando', 'descargando', 'listo']);
    expect(estados[1]).toEqual({ fase: 'descargando', progreso: 25 });
    const listo = estados[2];
    expect(listo.fase === 'listo' && listo.archivo.nombre).toBe('reporte-ventas_2026-10-01_2026-10-28.pdf');
  });

  it('sin Content-Length el progreso queda indeterminado (null)', () => {
    const estados: EstadoExportacion[] = [];
    service.exportar('inventario', {}).subscribe((e) => estados.push(e));
    const req = httpMock.expectOne((r) => r.url === '/api/v1/reportes/inventario');
    req.event({ type: HttpEventType.DownloadProgress, loaded: 1000 });
    expect(estados.at(-1)).toEqual({ fase: 'descargando', progreso: null });
    req.flush(new Blob(['x']));
  });

  it('usa un nombre por defecto si no llega Content-Disposition', () => {
    let nombre = '';
    service.exportar('demanda', {}).subscribe((e) => {
      if (e.fase === 'listo') nombre = e.archivo.nombre;
    });
    httpMock.expectOne((r) => r.url === '/api/v1/reportes/demanda').flush(new Blob(['%PDF']));
    expect(nombre).toBe('reporte-demanda.pdf');
  });

  it('lee el mensaje de error del Blob cuando el backend responde 400', async () => {
    const mensaje = new Promise<string>((resolve) =>
      service.exportar('ventas', {}).subscribe({ error: (e: Error) => resolve(e.message) }),
    );
    httpMock
      .expectOne((r) => r.url === '/api/v1/reportes/ventas')
      .flush(new Blob([JSON.stringify({ mensaje: 'La categoría no es válida' })]), {
        status: 400,
        statusText: 'Bad Request',
      });
    expect(await mensaje).toBe('La categoría no es válida');
  });

  it('un 500 muestra un mensaje genérico sin detalles internos', async () => {
    const mensaje = new Promise<string>((resolve) =>
      service.exportar('ventas', {}).subscribe({ error: (e: Error) => resolve(e.message) }),
    );
    httpMock
      .expectOne((r) => r.url === '/api/v1/reportes/ventas')
      .flush(new Blob(['trace interno']), { status: 500, statusText: 'Server Error' });
    expect(await mensaje).toContain('No pudimos generar el reporte');
  });
});
