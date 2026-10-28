import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ResumenAdmin } from '../models/admin.models';
import { ResumenAdminService } from './resumen-admin.service';

describe('ResumenAdminService (US-14)', () => {
  let http: HttpTestingController;
  let servicio: ResumenAdminService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    servicio = TestBed.inject(ResumenAdminService);
  });

  afterEach(() => http.verify());

  it('consulta los KPIs del panel con GET /admin/resumen', () => {
    let resultado: ResumenAdmin | null = null;
    servicio.consultar().subscribe((res) => (resultado = res));

    const req = http.expectOne('/api/admin/resumen');
    expect(req.request.method).toBe('GET');
    req.flush({ usuariosActivos: 12, incidenciasAbiertas: 2, tasaResolucionPorcentaje: 75 });

    expect(resultado).toEqual({
      usuariosActivos: 12,
      incidenciasAbiertas: 2,
      tasaResolucionPorcentaje: 75,
    });
  });
});
