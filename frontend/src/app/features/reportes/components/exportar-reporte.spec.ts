import { TestBed } from '@angular/core/testing';
import { NEVER, Observable, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { ExportarReporte } from './exportar-reporte';
import { ArchivoDescargado, EstadoExportacion, FiltrosReporte } from '../models/reporte.models';
import { ErrorReporte, ReportesService } from '../services/reportes.service';

describe('ExportarReporte (US-16)', () => {
  let exportar: ReturnType<typeof vi.fn>;
  let descargar: ReturnType<typeof vi.fn>;
  let imprimir: ReturnType<typeof vi.fn>;
  let reportarExportacion: ReturnType<typeof vi.fn>;

  const archivo: ArchivoDescargado = { blob: new Blob(['x']), nombre: 'reporte-ventas.pdf' };

  beforeEach(async () => {
    exportar = vi.fn();
    descargar = vi.fn();
    imprimir = vi.fn().mockResolvedValue('impreso');
    reportarExportacion = vi.fn();

    await TestBed.configureTestingModule({
      imports: [ExportarReporte],
      providers: [
        {
          provide: ReportesService,
          useFactory: () => ({ exportar, descargar, imprimir, reportarExportacion }),
        },
      ],
    }).compileComponents();
  });

  afterEach(() => vi.restoreAllMocks());

  function crear() {
    const fixture = TestBed.createComponent(ExportarReporte);
    fixture.detectChanges();
    return fixture;
  }

  function listo(): EstadoExportacion {
    return { fase: 'listo', archivo };
  }

  function ventanaFalsa(): Window {
    return {
      closed: false,
      close: vi.fn(),
      location: { href: '' },
    } as unknown as Window;
  }

  it('ofrece los cuatro reportes y los tres formatos', () => {
    const el: HTMLElement = crear().nativeElement;

    const tipos = Array.from(el.querySelectorAll<HTMLOptionElement>('#tipo-reporte option')).map(
      (o) => o.textContent?.trim(),
    );
    expect(tipos).toEqual(['Ventas', 'Inventario', 'Demanda', 'Búsquedas']);

    const formatos = Array.from(el.querySelectorAll<HTMLOptionElement>('#formato-reporte option')).map(
      (o) => o.value,
    );
    expect(formatos).toEqual(['PDF', 'XLSX', 'CSV']);
  });

  it('deshabilita imprimir y lo explica cuando el formato no es PDF', () => {
    const fixture = crear();
    const el: HTMLElement = fixture.nativeElement;
    const botonImprimir = () => el.querySelectorAll<HTMLButtonElement>('button')[1];

    expect(botonImprimir().disabled).toBe(false);

    fixture.componentInstance.formato.set('XLSX');
    fixture.detectChanges();

    expect(botonImprimir().disabled).toBe(true);
    expect(el.textContent).toContain('la impresión está disponible solo en PDF');
  });

  it('bloquea los controles mientras hay una exportación en curso', () => {
    const fixture = crear();
    const el: HTMLElement = fixture.nativeElement;
    exportar.mockReturnValue(NEVER);

    fixture.componentInstance.exportar('descargar');
    fixture.detectChanges();

    const botones = Array.from(el.querySelectorAll<HTMLButtonElement>('button'));
    expect(botones[0].disabled).toBe(true);
    expect(botones[1].disabled).toBe(true);
  });

  it('exporta con el tipo, los filtros y el formato elegidos', () => {
    const fixture = crear();
    const componente = fixture.componentInstance;
    exportar.mockReturnValue(of(listo()));

    componente.exportar('descargar');

    expect(exportar).toHaveBeenCalledWith('ventas', {}, 'PDF');
    expect(descargar).toHaveBeenCalledWith(archivo);
    expect(reportarExportacion).toHaveBeenCalledWith(
      'ventas',
      'PDF',
      'OK',
      'archivo_validado_entregado',
    );
    expect(componente.exito()).toContain('Reporte PDF descargado');
  });

  it('envía el formato elegido al servicio', () => {
    const fixture = crear();
    exportar.mockReturnValue(of(listo()));

    fixture.componentInstance.formato.set('CSV');
    fixture.componentInstance.exportar('descargar');

    expect(exportar).toHaveBeenCalledWith('ventas', {}, 'CSV');
  });

  it('no abre la petición de impresión si el formato no es PDF', () => {
    const fixture = crear();
    fixture.componentInstance.formato.set('XLSX');

    fixture.componentInstance.exportar('imprimir');

    expect(exportar).not.toHaveBeenCalled();
    expect(fixture.componentInstance.error()).toContain('Solo puedes imprimir reportes en PDF');
  });

  it('imprime en el iframe sin depender de la ventana de respaldo', async () => {
    const fixture = crear();
    vi.spyOn(window, 'open').mockReturnValue(null);
    exportar.mockReturnValue(of(listo()));
    imprimir.mockResolvedValue('impreso');

    fixture.componentInstance.exportar('imprimir');
    await fixture.whenStable();

    expect(imprimir).toHaveBeenCalledWith(archivo, null);
    expect(fixture.componentInstance.exito()).toContain('diálogo de impresión');
    expect(reportarExportacion).toHaveBeenCalledWith(
      'ventas',
      'PDF',
      'OK',
      'archivo_validado_impreso',
    );
  });

  it('usa la pestaña de respaldo cuando el iframe no puede imprimir', async () => {
    const fixture = crear();
    const ventana = ventanaFalsa();
    vi.spyOn(window, 'open').mockReturnValue(ventana);
    exportar.mockReturnValue(of(listo()));
    imprimir.mockResolvedValue('pestana');

    fixture.componentInstance.exportar('imprimir');
    await fixture.whenStable();

    expect(imprimir).toHaveBeenCalledWith(archivo, ventana);
    expect(fixture.componentInstance.exito()).toContain('pestaña que se abrió');
  });

  it('informa cuando no hay iframe ni ventana disponible para imprimir', async () => {
    const fixture = crear();
    vi.spyOn(window, 'open').mockReturnValue(null);
    exportar.mockReturnValue(of(listo()));
    imprimir.mockResolvedValue('sin-ventana');

    fixture.componentInstance.exportar('imprimir');
    await fixture.whenStable();

    expect(fixture.componentInstance.exito()).toContain('Descarga el PDF');
    expect(reportarExportacion).toHaveBeenCalledWith(
      'ventas',
      'PDF',
      'ERROR_CLIENTE',
      'No se pudo abrir la impresión',
    );
  });

  it('reporta el fallo de exportación con su clasificación', () => {
    const fixture = crear();
    exportar.mockReturnValue(
      throwError(() => new ErrorReporte('La categoría no es válida', 'ERROR_CLIENTE')),
    );

    fixture.componentInstance.exportar('descargar');

    expect(fixture.componentInstance.error()).toBe('La categoría no es válida');
    expect(reportarExportacion).toHaveBeenCalledWith(
      'ventas',
      'PDF',
      'ERROR_CLIENTE',
      'La categoría no es válida',
    );
    expect(fixture.componentInstance.ocupado()).toBe(false);
  });

  it('clasifica un error de red como ERROR_RED', () => {
    const fixture = crear();
    exportar.mockReturnValue(throwError(() => new ErrorReporte('Sin conexión', 'ERROR_RED')));

    fixture.componentInstance.exportar('descargar');

    expect(reportarExportacion).toHaveBeenCalledWith('ventas', 'PDF', 'ERROR_RED', 'Sin conexión');
  });

  it('clasifica un archivo vacío como BLOB_VACIO', () => {
    const fixture = crear();
    exportar.mockReturnValue(throwError(() => new ErrorReporte('Archivo vacío', 'BLOB_VACIO')));

    fixture.componentInstance.exportar('descargar');

    expect(reportarExportacion).toHaveBeenCalledWith('ventas', 'PDF', 'BLOB_VACIO', 'Archivo vacío');
  });

  it('un error no clasificado se reporta como ERROR_CLIENTE', () => {
    const fixture = crear();
    exportar.mockReturnValue(throwError(() => new Error('fallo inesperado')));

    fixture.componentInstance.exportar('descargar');

    expect(reportarExportacion).toHaveBeenCalledWith(
      'ventas',
      'PDF',
      'ERROR_CLIENTE',
      'fallo inesperado',
    );
  });

  it('si el navegador no puede entregar el archivo también lo reporta', () => {
    const fixture = crear();
    exportar.mockReturnValue(of(listo()));
    descargar.mockImplementation(() => {
      throw new Error('sin permisos');
    });

    fixture.componentInstance.exportar('descargar');

    expect(fixture.componentInstance.error()).toContain('permisos de descarga');
    expect(reportarExportacion).toHaveBeenLastCalledWith(
      'ventas',
      'PDF',
      'ERROR_CLIENTE',
      'El navegador no pudo entregar el archivo',
    );
  });

  it('cancelar libera la suscripción, cierra la ventana y reporta la cancelación', () => {
    const fixture = crear();
    const ventana = ventanaFalsa();
    vi.spyOn(window, 'open').mockReturnValue(ventana);
    let liberada = false;
    exportar.mockReturnValue(
      new Observable<EstadoExportacion>(() => () => {
        liberada = true;
      }),
    );

    fixture.componentInstance.exportar('imprimir');
    fixture.componentInstance.cancelar();

    expect(liberada).toBe(true);
    expect(ventana.close).toHaveBeenCalled();
    expect(fixture.componentInstance.ocupado()).toBe(false);
    expect(reportarExportacion).toHaveBeenCalledWith(
      'ventas',
      'PDF',
      'CANCELADO',
      'El usuario canceló la exportación',
    );
  });

  it('al destruir el componente libera la suscripción y cierra la ventana', () => {
    const fixture = crear();
    const ventana = ventanaFalsa();
    vi.spyOn(window, 'open').mockReturnValue(ventana);
    let liberada = false;
    exportar.mockReturnValue(
      new Observable<EstadoExportacion>(() => () => {
        liberada = true;
      }),
    );

    fixture.componentInstance.exportar('imprimir');
    fixture.destroy();

    expect(liberada).toBe(true);
    expect(ventana.close).toHaveBeenCalled();
  });

  it('pasa las fechas y la categoría recibidas del dashboard', () => {
    const fixture = TestBed.createComponent(ExportarReporte);
    fixture.componentRef.setInput('startDate', '2026-10-01');
    fixture.componentRef.setInput('endDate', '2026-10-28');
    fixture.componentRef.setInput('categoria', 'VESTIDOS');
    fixture.detectChanges();
    exportar.mockReturnValue(of(listo()));

    fixture.componentInstance.exportar('descargar');

    const filtros = exportar.mock.calls[0][1] as FiltrosReporte;
    expect(filtros).toEqual({ startDate: '2026-10-01', endDate: '2026-10-28', categoria: 'VESTIDOS' });
  });
});
