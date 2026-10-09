import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { Pagina, VitrinaItem } from '../models/vitrina.models';
import { VitrinaService } from './vitrina.service';

describe('VitrinaService (US-09)', () => {
  let http: HttpTestingController;
  let servicio: VitrinaService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    servicio = TestBed.inject(VitrinaService);
  });

  afterEach(() => http.verify());

  function pagina(): Pagina<VitrinaItem> {
    return {
      contenido: [
        {
          id: 10,
          nombre: 'Pantalón Jogger Azul',
          categoria: 'PANTALONES',
          genero: 'UNISEX',
          marca: 'Tienda Sol',
          precio: 32900,
          imagenUrl: '/media/a.png',
          disponible: true,
        },
      ],
      pagina: 0,
      tamano: 12,
      totalElementos: 1,
      totalPaginas: 1,
    };
  }

  it('envía los filtros como query params', () => {
    servicio
      .explorar({ categoria: 'PANTALONES', material: 'ALGODON', talla: '4', pagina: 0, orden: 'precioAsc' })
      .subscribe();
    const req = http.expectOne(
      (p) =>
        p.url === '/api/cliente/catalogo' &&
        p.params.get('categoria') === 'PANTALONES' &&
        p.params.get('material') === 'ALGODON' &&
        p.params.get('talla') === '4' &&
        p.params.get('orden') === 'precioAsc',
    );
    expect(req.request.method).toBe('GET');
    req.flush(pagina());
  });

  it('omite los filtros vacíos', () => {
    servicio.explorar({ q: '', categoria: '', pagina: 0 }).subscribe();
    const req = http.expectOne((p) => p.url === '/api/cliente/catalogo');
    expect(req.request.params.has('q')).toBe(false);
    expect(req.request.params.has('categoria')).toBe(false);
    expect(req.request.params.get('pagina')).toBe('0');
    req.flush(pagina());
  });

  it('pide el detalle por id', () => {
    servicio.detalle(10).subscribe();
    const req = http.expectOne('/api/cliente/catalogo/10');
    expect(req.request.method).toBe('GET');
    req.flush({ id: 10 });
  });
});
