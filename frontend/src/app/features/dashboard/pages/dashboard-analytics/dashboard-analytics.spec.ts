import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Observable, of, throwError } from 'rxjs';

import { DashboardAnalyticsPage } from './dashboard-analytics';
import {
  DashboardAnalyticsService,
  DashboardOverview,
} from '../../services/dashboard-analytics.service';
import { AuthService } from '../../../../core/services/auth.service';

describe('DashboardAnalyticsPage', () => {
  let fixture: ComponentFixture<DashboardAnalyticsPage>;
  let component: DashboardAnalyticsPage;
  let serviceSpy: {
    consultar: ReturnType<typeof vi.fn>;
    consultarTopMasConsultados: ReturnType<typeof vi.fn>;
    consultarTopMasVendidos: ReturnType<typeof vi.fn>;
  };

  const datos: DashboardOverview = {
    totalVentas: 1500,
    totalPedidos: 10,
    ticketPromedio: 150,
    productosVendidos: 24,
    margenBruto: null,
    stockTotal: 50,
    stockBajo: 2,
    ventasPorCategoria: [{ label: 'BUZOS', value: 800, cantidad: 8 }],
    demandaPorTalla: [{ label: 'M', value: 800, cantidad: 12 }],
    productosPopulares: [{ label: 'Buzo Azul', value: 800, cantidad: 8 }],
    productosMasConsultados: [{ label: 'Vestido Rosa', value: 31, cantidad: 31 }],
    busquedasMasFrecuentes: [{ label: 'buzo niño', value: 4, cantidad: 4 }],
  };

  const sinActividad: DashboardOverview = {
    ...datos,
    totalVentas: 0,
    totalPedidos: 0,
    ticketPromedio: 0,
    productosVendidos: 0,
    stockTotal: 0,
    stockBajo: 0,
    ventasPorCategoria: [],
    demandaPorTalla: [],
    productosPopulares: [],
    productosMasConsultados: [],
    busquedasMasFrecuentes: [],
  };

  async function crear(
    rol: 'ADMINISTRADOR' | 'VENDEDOR',
    respuesta: Observable<DashboardOverview>,
  ): Promise<void> {
    serviceSpy = {
      consultar: vi.fn().mockReturnValue(respuesta),
      consultarTopMasConsultados: vi.fn().mockReturnValue(of([])),
      consultarTopMasVendidos: vi.fn().mockReturnValue(of([])),
    };

    await TestBed.configureTestingModule({
      imports: [DashboardAnalyticsPage, RouterTestingModule],
      providers: [
        { provide: DashboardAnalyticsService, useValue: serviceSpy },
        {
          provide: AuthService,
          useValue: {
            perfil: vi.fn().mockReturnValue(of({ id: 1, rol })),
            rol: vi.fn().mockReturnValue(rol),
            usuario: vi.fn().mockReturnValue(null),
            rutaInicio: vi.fn().mockReturnValue('/admin'),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardAnalyticsPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  const texto = () => (fixture.nativeElement as HTMLElement).textContent ?? '';

  it('pide el resumen una sola vez y no hace peticiones extra a los tops', async () => {
    await crear('ADMINISTRADOR', of(datos));

    expect(serviceSpy.consultar).toHaveBeenCalledTimes(1);
    expect(serviceSpy.consultarTopMasConsultados).not.toHaveBeenCalled();
    expect(serviceSpy.consultarTopMasVendidos).not.toHaveBeenCalled();
  });

  it('el administrador ve datos reales de consultados, vendidos y búsquedas', async () => {
    await crear('ADMINISTRADOR', of(datos));

    expect(texto()).toContain('Vestido Rosa');
    expect(texto()).toContain('Buzo Azul');
    expect(texto()).toContain('buzo niño');
    expect(texto()).toContain('Palabras más buscadas');
    expect(texto()).not.toContain('Baja rotación');
    expect(texto()).toMatch(/1\.500/); // ventas totales reales
  });

  it('el vendedor ve KPIs de inventario y su demanda, con títulos que coinciden con lo graficado', async () => {
    await crear('VENDEDOR', of(datos));

    expect(texto()).toContain('Inventario disponible');
    expect(texto()).toContain('Tallas con stock bajo');
    expect(texto()).toContain('Demanda por talla');
    expect(texto()).toContain('Ventas por categoría');
    expect(texto()).not.toContain('Ventas por período');
  });

  it('sin actividad muestra ceros y estados vacíos, nunca cifras de ejemplo', async () => {
    await crear('ADMINISTRADOR', of(sinActividad));

    const html = texto();
    expect(html).not.toMatch(/488\.000|27\.111|Vestido de lino|Pantalón cargo|Saco de lana/);
    expect(html).toContain('Sin consultas en el período seleccionado');
    expect(html).toContain('Sin ventas en el período seleccionado');
    expect(html).toContain('Sin búsquedas en el período seleccionado');
  });

  it('ante un error del servidor muestra el error y no datos falsos', async () => {
    await crear(
      'ADMINISTRADOR',
      throwError(() => new HttpErrorResponse({ status: 500 })),
    );

    expect(component.error()).toContain('No pudimos cargar las métricas');
    expect(component.overviewData()).toBeNull();
    expect(texto()).toContain('Error al cargar datos');
    expect(texto()).not.toMatch(/488\.000|Vestido de lino/);
  });

  it('un 403 explica que falta permiso y Reintentar vuelve a consultar', async () => {
    await crear('VENDEDOR', throwError(() => new HttpErrorResponse({ status: 403 })));
    expect(component.error()).toContain('No tienes permiso');

    serviceSpy.consultar.mockReturnValue(of(datos));
    component.cargarDatosDashboard();

    expect(serviceSpy.consultar).toHaveBeenCalledTimes(2);
    expect(component.error()).toBe('');
    expect(component.overviewData()).toEqual(datos);
  });

  it('al elegir una categoría envía su código y no un número', async () => {
    await crear('ADMINISTRADOR', of(datos));

    component.categoriaSeleccionada = 'VESTIDOS';
    component.onCategoriaChange();

    const [, , categoria] = serviceSpy.consultar.mock.calls.at(-1)!;
    expect(categoria).toBe('VESTIDOS');
    expect(Number.isNaN(Number(categoria))).toBe(true);
  });

  it('"Todas" no envía categoría', async () => {
    await crear('ADMINISTRADOR', of(datos));

    component.categoriaSeleccionada = 'TODAS';
    component.onCategoriaChange();

    const [, , categoria] = serviceSpy.consultar.mock.calls.at(-1)!;
    expect(categoria).toBeUndefined();
  });

  it('el selector se llena con las categorías reales del catálogo', async () => {
    await crear('ADMINISTRADOR', of(datos));

    const opciones = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('.category-field option'),
    ).map((o) => (o as HTMLOptionElement).value);
    expect(opciones).toContain('TODAS');
    expect(opciones).toContain('VESTIDOS');
    expect(opciones).toContain('ACCESORIOS');
    expect(opciones).toHaveLength(12);
  });
});
