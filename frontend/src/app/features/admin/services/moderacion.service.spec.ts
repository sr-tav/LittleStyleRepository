import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { PrendaAdmin } from '../models/admin.models';
import { ModeracionService } from './moderacion.service';

describe('ModeracionService (US-14)', () => {
  let http: HttpTestingController;
  let servicio: ModeracionService;

  const prenda: PrendaAdmin = {
    id: 9,
    nombre: 'Camiseta bears',
    precio: 25900,
    estado: 'ACTIVA',
    estadoModeracion: 'SIN_REVISAR',
    motivoModeracion: null,
    vendedorId: 5,
    vendedorEmail: 'tienda@correo.com',
    vendedorTienda: 'Mundo Pequeño',
    fechaActualizacion: '2026-10-20T08:00:00',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    servicio = TestBed.inject(ModeracionService);
  });

  afterEach(() => http.verify());

  it('lista la cola con los filtros y la paginación en la URL', () => {
    servicio
      .listar({ estadoModeracion: 'SIN_REVISAR', vendedorEmail: 'tienda@correo.com', pagina: 2, tamano: 10 })
      .subscribe();

    const req = http.expectOne(
      (p) =>
        p.url === '/api/admin/moderacion/prendas' &&
        p.params.get('estadoModeracion') === 'SIN_REVISAR' &&
        p.params.get('vendedorEmail') === 'tienda@correo.com' &&
        p.params.get('pagina') === '2' &&
        p.params.get('tamano') === '10',
    );
    expect(req.request.method).toBe('GET');
    req.flush({ contenido: [prenda], pagina: 2, tamano: 10, totalElementos: 1, totalPaginas: 1 });
  });

  it('pide el detalle completo de una prenda', () => {
    servicio.detalle(9).subscribe();
    const req = http.expectOne('/api/admin/moderacion/prendas/9');
    expect(req.request.method).toBe('GET');
    req.flush({ ...prenda, composicion: [], tallas: [], imagenes: [] });
  });

  it('aprueba una prenda con POST /aprobar', () => {
    servicio.aprobar(9).subscribe();
    const req = http.expectOne('/api/admin/moderacion/prendas/9/aprobar');
    expect(req.request.method).toBe('POST');
    req.flush({ ...prenda, estadoModeracion: 'APROBADA' });
  });

  it('desactiva una prenda enviando el motivo', () => {
    servicio.desactivar(9, 'Falta composición de materiales').subscribe();
    const req = http.expectOne('/api/admin/moderacion/prendas/9/desactivar');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ motivo: 'Falta composición de materiales' });
    req.flush({ ...prenda, estadoModeracion: 'DESACTIVADA' });
  });

  it('restaura una prenda rechazada', () => {
    servicio.restaurar(9).subscribe();
    const req = http.expectOne('/api/admin/moderacion/prendas/9/restaurar');
    expect(req.request.method).toBe('POST');
    req.flush({ ...prenda, estadoModeracion: 'SIN_REVISAR' });
  });
});
