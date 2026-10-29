import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { ResumenAdmin } from '../../../admin/models/admin.models';
import { ResumenAdminService } from '../../../admin/services/resumen-admin.service';
import { DashboardAdmin } from './dashboard-admin';

describe('DashboardAdmin (US-14)', () => {
  let respuesta: ResumenAdmin | null;
  const consultar = vi.fn(() => of(respuesta as ResumenAdmin));

  beforeEach(async () => {
    consultar.mockClear();
    respuesta = {
      usuariosActivos: 12,
      usuariosSuspendidos: 2,
      usuariosBloqueados: 1,
      usuariosEliminados: 1,
      prendasSinRevisar: 6,
      prendasPublicadas: 45,
      prendasRechazadas: 3,
      incidenciasAbiertas: 4,
      incidenciasEnRevision: 2,
      tasaResolucionPorcentaje: 80,
    };
    await TestBed.configureTestingModule({
      imports: [DashboardAdmin],
      providers: [
        provideRouter([]),
        { provide: ResumenAdminService, useFactory: () => ({ consultar }) },
      ],
    }).compileComponents();
  });

  it('muestra los KPIs del resumen y los enlaces a cada módulo', () => {
    const fixture = TestBed.createComponent(DashboardAdmin);
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Usuarios activos');
    expect(el.textContent).toContain('12');
    expect(el.textContent).toContain('Prendas publicadas');
    expect(el.textContent).toContain('45');
    expect(el.textContent).toContain('80%');
    for (const ruta of ['/admin/usuarios', '/admin/moderacion', '/admin/auditoria', '/admin/incidencias']) {
      expect(el.querySelector<HTMLAnchorElement>(`a[href="${ruta}"]`)).not.toBeNull();
    }
    expect(el.querySelector<HTMLAnchorElement>('.kpi-card[href="/admin/usuarios"]')).not.toBeNull();
    expect(el.querySelector<HTMLAnchorElement>('.kpi-card[href="/admin/moderacion"]')).not.toBeNull();
    expect(el.querySelector<HTMLAnchorElement>('.kpi-card[href="/admin/incidencias"]:last-of-type')).not.toBeNull();
    expect(el.querySelector('.home-tile-moderacion h2')?.textContent?.trim()).toBe('6 pendientes de revisión');
  });

  it('muestra un error amable y permite reintentar la carga', () => {
    consultar.mockReturnValueOnce(throwError(() => new Error('falla')));
    const fixture = TestBed.createComponent(DashboardAdmin);
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Ocurrió un error inesperado');

    consultar.mockReturnValueOnce(of(respuesta as ResumenAdmin));
    (el.querySelector<HTMLButtonElement>('.link-button')!).click();
    fixture.detectChanges();

    expect(el.textContent).toContain('Usuarios activos');
    expect(consultar).toHaveBeenCalledTimes(2);
  });
});
