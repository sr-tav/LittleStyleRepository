import { HttpEventType, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { EstadoExportacion } from '../models/reporte.models';
import { ErrorReporte, ReportesService } from './reportes.service';

describe('ReportesService', () => {
  let service: ReportesService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ReportesService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  async function esperarValidacion(): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve, 0));
  }

  it('pide el PDF con los filtros y emite generando, descargando y listo', async () => {
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
    await esperarValidacion();

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
    req.flush(new Blob(['%PDF-1.7']));
  });

  it('usa un nombre por defecto si no llega Content-Disposition', async () => {
    let nombre = '';
    service.exportar('demanda', {}).subscribe((e) => {
      if (e.fase === 'listo') nombre = e.archivo.nombre;
    });
    httpMock.expectOne((r) => r.url === '/api/v1/reportes/demanda').flush(new Blob(['%PDF-1.7']));
    await esperarValidacion();
    expect(nombre).toBe('reporte-demanda.pdf');
  });

  it('lee el mensaje de error del Blob cuando el backend responde 400', async () => {
    const error = new Promise<ErrorReporte>((resolve) =>
      service.exportar('ventas', {}).subscribe({ error: (e: ErrorReporte) => resolve(e) }),
    );
    httpMock
      .expectOne((r) => r.url === '/api/v1/reportes/ventas')
      .flush(new Blob([JSON.stringify({ mensaje: 'La categoría no es válida' })]), {
        status: 400,
        statusText: 'Bad Request',
      });
    const resultado = await error;
    expect(resultado.message).toBe('La categoría no es válida');
    expect(resultado.resultado).toBe('ERROR_CLIENTE');
  });

  it('un 500 muestra un mensaje genérico sin detalles internos', async () => {
    const error = new Promise<ErrorReporte>((resolve) =>
      service.exportar('ventas', {}).subscribe({ error: (e: ErrorReporte) => resolve(e) }),
    );
    httpMock
      .expectOne((r) => r.url === '/api/v1/reportes/ventas')
      .flush(new Blob(['trace interno']), { status: 500, statusText: 'Server Error' });
    const resultado = await error;
    expect(resultado.message).toContain('No pudimos generar el reporte');
    expect(resultado.resultado).toBe('ERROR_CLIENTE');
  });

  it('un error de red se clasifica como ERROR_RED', async () => {
    const error = new Promise<ErrorReporte>((resolve) =>
      service.exportar('ventas', {}).subscribe({ error: (e: ErrorReporte) => resolve(e) }),
    );
    httpMock
      .expectOne((r) => r.url === '/api/v1/reportes/ventas')
      .error(new ProgressEvent('error'), { status: 0, statusText: 'Unknown Error' });
    const resultado = await error;
    expect(resultado.resultado).toBe('ERROR_RED');
  });

  it('un cuerpo vacío se clasifica como BLOB_VACIO', async () => {
    const error = new Promise<ErrorReporte>((resolve) =>
      service.exportar('ventas', {}).subscribe({ error: (e: ErrorReporte) => resolve(e) }),
    );
    httpMock
      .expectOne((r) => r.url === '/api/v1/reportes/ventas')
      .flush(new Blob([], { type: 'application/pdf' }));
    const resultado = await error;
    expect(resultado.resultado).toBe('BLOB_VACIO');
  });

  it('no entrega como PDF un HTML de error disfrazado', async () => {
    const error = new Promise<ErrorReporte>((resolve) =>
      service.exportar('ventas', {}, 'PDF').subscribe({ error: (e: ErrorReporte) => resolve(e) }),
    );
    httpMock
      .expectOne((r) => r.url === '/api/v1/reportes/ventas')
      .flush(new Blob(['<html><body>Error del proxy</body></html>']));
    const resultado = await error;
    expect(resultado.resultado).toBe('ERROR_CLIENTE');
    expect(resultado.message).toContain('PDF');
  });

  it('no entrega como XLSX un JSON de error disfrazado', async () => {
    const error = new Promise<ErrorReporte>((resolve) =>
      service.exportar('ventas', {}, 'XLSX').subscribe({ error: (e: ErrorReporte) => resolve(e) }),
    );
    httpMock
      .expectOne((r) => r.url === '/api/v1/reportes/ventas')
      .flush(new Blob([JSON.stringify({ mensaje: 'fallo interno' })]));
    const resultado = await error;
    expect(resultado.resultado).toBe('ERROR_CLIENTE');
    expect(resultado.message).toContain('XLSX');
  });

  it('acepta un XLSX real que empieza con la firma PK', async () => {
    let nombre = '';
    service.exportar('ventas', {}, 'XLSX').subscribe((e) => {
      if (e.fase === 'listo') nombre = e.archivo.nombre;
    });
    httpMock
      .expectOne((r) => r.url === '/api/v1/reportes/ventas')
      .flush(new Blob([new Uint8Array([0x50, 0x4b, 0x03, 0x04])]));
    await esperarValidacion();
    expect(nombre).toBe('reporte-ventas.xlsx');
  });

  it('acepta un CSV con BOM UTF-8', async () => {
    const fases: string[] = [];
    service.exportar('busquedas', {}, 'CSV').subscribe((e) => fases.push(e.fase));
    httpMock
      .expectOne((r) => r.url === '/api/v1/reportes/busquedas')
      .flush(new Blob([new Uint8Array([0xef, 0xbb, 0xbf, 0x61, 0x3b, 0x62])]));
    await esperarValidacion();
    expect(fases).toContain('listo');
  });

  it('no entrega como CSV un HTML de error disfrazado', async () => {
    const error = new Promise<ErrorReporte>((resolve) =>
      service.exportar('busquedas', {}, 'CSV').subscribe({ error: (e: ErrorReporte) => resolve(e) }),
    );
    httpMock
      .expectOne((r) => r.url === '/api/v1/reportes/busquedas')
      .flush(new Blob(['<!DOCTYPE html><html><body>Mantenimiento</body></html>']));
    const resultado = await error;
    expect(resultado.resultado).toBe('ERROR_CLIENTE');
    expect(resultado.message).toContain('CSV');
  });

  it('no entrega como CSV un JSON de error, aunque empiece con BOM', async () => {
    const error = new Promise<ErrorReporte>((resolve) =>
      service.exportar('busquedas', {}, 'CSV').subscribe({ error: (e: ErrorReporte) => resolve(e) }),
    );
    httpMock
      .expectOne((r) => r.url === '/api/v1/reportes/busquedas')
      .flush(
        new Blob([
          new Uint8Array([0xef, 0xbb, 0xbf]),
          '{"timestamp":"2026-11-01T21:00:00Z","status":502}',
        ]),
      );
    const resultado = await error;
    expect(resultado.resultado).toBe('ERROR_CLIENTE');
    expect(resultado.message).toContain('CSV');
  });

  it('un CSV que solo trae BOM se clasifica como BLOB_VACIO', async () => {
    const error = new Promise<ErrorReporte>((resolve) =>
      service.exportar('busquedas', {}, 'CSV').subscribe({ error: (e: ErrorReporte) => resolve(e) }),
    );
    httpMock
      .expectOne((r) => r.url === '/api/v1/reportes/busquedas')
      .flush(new Blob([new Uint8Array([0xef, 0xbb, 0xbf])]));
    const resultado = await error;
    expect(resultado.resultado).toBe('BLOB_VACIO');
  });

  it('pide el formato elegido en el query param', () => {
    service.exportar('ventas', {}, 'XLSX').subscribe();
    const req = httpMock.expectOne((r) => r.url === '/api/v1/reportes/ventas');
    expect(req.request.params.get('formato')).toBe('XLSX');
    req.flush(new Blob(['PK']));
  });

  it('usa la extensión del formato cuando no llega Content-Disposition', async () => {
    let nombre = '';
    service.exportar('busquedas', {}, 'CSV').subscribe((e) => {
      if (e.fase === 'listo') nombre = e.archivo.nombre;
    });
    httpMock
      .expectOne((r) => r.url === '/api/v1/reportes/busquedas')
      .flush(new Blob(['a;b'], { type: 'text/csv' }));
    await esperarValidacion();
    expect(nombre).toBe('reporte-busquedas.csv');
  });

  it('reporta la telemetría al backend sin bloquear al usuario', () => {
    service.reportarExportacion('ventas', 'XLSX', 'OK');

    const req = httpMock.expectOne('/api/v1/reportes/telemetria');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ tipo: 'ventas', formato: 'XLSX', resultado: 'ok', motivo: null });
    req.flush(null);
  });

  it('un fallo al enviar telemetría no rompe la experiencia de descarga', () => {
    expect(() =>
      service.reportarExportacion('ventas', 'PDF', 'ERROR_CLIENTE', 'sin permiso'),
    ).not.toThrow();

    const req = httpMock.expectOne('/api/v1/reportes/telemetria');
    expect(req.request.body.resultado).toBe('error_cliente');
    req.flush({ mensaje: 'sin servicio' }, { status: 503, statusText: 'Unavailable' });
  });
});
