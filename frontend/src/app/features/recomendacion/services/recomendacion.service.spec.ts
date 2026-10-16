import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { RecomendacionService } from './recomendacion.service';

describe('RecomendacionService (US-07)', () => {
  let http: HttpTestingController;
  let servicio: RecomendacionService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), RecomendacionService],
    });
    http = TestBed.inject(HttpTestingController);
    servicio = TestBed.inject(RecomendacionService);
  });

  afterEach(() => http.verify());

  it('lista con el perfil como query param', () => {
    servicio.listar(3).subscribe();
    const req = http.expectOne('/api/cliente/recomendaciones?perfilId=3');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('consulta por prenda con el perfil como query param', () => {
    servicio.porPrenda(10, 3).subscribe();
    const req = http.expectOne('/api/cliente/recomendaciones/prenda/10?perfilId=3');
    expect(req.request.method).toBe('GET');
    req.flush(null);
  });

  it('pide solo identificadores para el filtro de servidor', () => {
    servicio.ids(3).subscribe();
    const req = http.expectOne('/api/cliente/recomendaciones/ids?perfilId=3');
    expect(req.request.method).toBe('GET');
    req.flush([1, 2]);
  });
});
