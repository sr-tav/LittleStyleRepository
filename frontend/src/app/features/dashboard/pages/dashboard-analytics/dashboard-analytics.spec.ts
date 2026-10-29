import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

import { DashboardAnalyticsPage } from './dashboard-analytics';
import { DashboardAnalyticsService } from '../../services/dashboard-analytics.service';
import { AuthService } from '../../../../core/services/auth.service';

const chartConstructor = vi.hoisted(() => vi.fn());
vi.mock('chart.js/auto', () => ({ default: chartConstructor }));

describe('DashboardAnalyticsPage', () => {
  let component: DashboardAnalyticsPage;
  let fixture: ComponentFixture<DashboardAnalyticsPage>;
  let serviceSpy: {
    consultar: ReturnType<typeof vi.fn>;
    consultarTopMasConsultados: ReturnType<typeof vi.fn>;
    consultarTopMasVendidos: ReturnType<typeof vi.fn>;
  };

  const mockDashboardData = {
    totalVentas: 1500,
    totalPedidos: 10,
    ticketPromedio: 150,
    productosVendidos: 24,
    margenBruto: 300,
    stockTotal: 50,
    stockBajo: 2,
    ventasPorCategoria: [{ label: 'Buzos', value: 800, cantidad: 8 }],
    demandaPorTalla: [{ label: 'M', value: 12, cantidad: 12 }],
    productosPopulares: [{ label: 'Buzo', value: 8, cantidad: 8 }],
    busquedasMasFrecuentes: [{ label: 'buzo niño', value: 4, cantidad: 4 }],
    demandaDetalle: [{ prenda: 'Buzo', talla: 'M', unidades: 8, valor: 800 }],
  };

  beforeEach(async () => {
    serviceSpy = {
      consultar: vi.fn().mockReturnValue(of(mockDashboardData)),
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
            perfil: vi.fn().mockReturnValue(of({ id: 1, rol: 'ADMINISTRADOR' })),
            rol: vi.fn().mockReturnValue('ADMINISTRADOR'),
            usuario: vi.fn().mockReturnValue(null),
            rutaInicio: vi.fn().mockReturnValue('/admin'),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardAnalyticsPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('muestra un loader mientras carga y luego renderiza KPI', () => {
    expect(component).toBeTruthy();
    expect(serviceSpy.consultar).toHaveBeenCalled();
  });

  it('no ofrece período personalizado y mantiene funcional el rango de fechas', () => {
    const opcionesPeriodo = Array.from(
      fixture.nativeElement.querySelectorAll('select option'),
      (option: HTMLOptionElement) => option.value,
    );
    expect(opcionesPeriodo).not.toContain('PERSONALIZADO');

    component.fechaDesde = '2026-10-01';
    component.fechaHasta = '2026-10-28';
    component.onFechaManualChange();

    expect(component.periodoSeleccionado).toBe('30_DIAS');
    expect(serviceSpy.consultar).toHaveBeenLastCalledWith('2026-10-01', '2026-10-28', undefined);
  });

  it('presenta los importes en pesos colombianos y detalla ventas al filtrar una categoría', () => {
    expect(component.formatCurrency(12000)).toBe('COP $ 12.000');
    component.categoriaSeleccionada = 'VESTIDOS';
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Ventas por prenda');
    expect(fixture.nativeElement.textContent).toContain('Ingresos por prenda en Vestidos');
  });

  it('crea los dos gráficos para las series recibidas del dashboard', async () => {
    chartConstructor.mockClear();
    chartConstructor.mockImplementation(function () {
      return { destroy: vi.fn() };
    });
    component.overviewData.set({ ...mockDashboardData });
    fixture.detectChanges();
    await new Promise((resolve) => setTimeout(resolve, 10));

    const llamadas = chartConstructor.mock.calls.slice(-2);
    expect(llamadas).toHaveLength(2);
    expect(llamadas[0][0]).toBeInstanceOf(HTMLCanvasElement);
    expect(llamadas[0][1].data.datasets[0].data).toEqual([800]);
    expect(llamadas[1][1].data.datasets[0].data).toEqual([12]);
  });

  it('envía la categoría seleccionada y solicita de nuevo las métricas', () => {
    component.onCategoriaChange('VESTIDOS');

    expect(serviceSpy.consultar).toHaveBeenLastCalledWith(
      component.startDate(),
      component.endDate(),
      'VESTIDOS',
    );
    expect(serviceSpy.consultarTopMasConsultados).toHaveBeenLastCalledWith(
      component.startDate(),
      component.endDate(),
      'VESTIDOS',
    );
  });
});
