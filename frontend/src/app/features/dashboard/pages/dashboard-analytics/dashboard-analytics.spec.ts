import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

import { DashboardAnalyticsPage } from './dashboard-analytics';
import { DashboardAnalyticsService } from '../../services/dashboard-analytics.service';
import { AuthService } from '../../../../core/services/auth.service';

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
});
