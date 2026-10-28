import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';

import { AuthService } from '../../../core/services/auth.service';
import { CrearIncidencia, Incidencia } from '../models/admin.models';
import { IncidenciasService } from './incidencias.service';

describe('IncidenciasService (US-14)', () => {
  let http: HttpTestingController;
  let servicio: IncidenciasService;
  const rol = signal<'CLIENTE' | 'VENDEDOR'>('CLIENTE');

  const incidencia: Incidencia = {
    id: 4,
    reportanteEmail: 'cliente@correo.com',
    tipo: 'SOPORTE_GENERAL',
    referenciaId: null,
    referenciaDetalle: null,
    asunto: 'No puedo cambiar mi correo',
    descripcion: 'El formulario de mi cuenta no guarda el nuevo correo.',
    estado: 'ABIERTA',
    respuesta: null,
    fechaCreacion: '2026-10-21T09:00:00',
    fechaResolucion: null,
  };

  beforeEach(() => {
    rol.set('CLIENTE');
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { rol } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    servicio = TestBed.inject(IncidenciasService);
  });

  afterEach(() => http.verify());

  it('lista la bandeja del administrador con los filtros en la URL', () => {
    servicio
      .listar({ estado: 'ABIERTA', tipo: 'SOPORTE_GENERAL', rol: 'CLIENTE', texto: 'correo', pagina: 1, tamano: 10 })
      .subscribe();

    const req = http.expectOne(
      (p) =>
        p.url === '/api/admin/incidencias' &&
        p.params.get('estado') === 'ABIERTA' &&
        p.params.get('tipo') === 'SOPORTE_GENERAL' &&
        p.params.get('rol') === 'CLIENTE' &&
        p.params.get('texto') === 'correo' &&
        p.params.get('pagina') === '1',
    );
    expect(req.request.method).toBe('GET');
    req.flush({ contenido: [incidencia], pagina: 1, tamano: 10, totalElementos: 1, totalPaginas: 1 });
  });

  it('pide el detalle de una incidencia', () => {
    servicio.detalle(4).subscribe();
    const req = http.expectOne('/api/admin/incidencias/4');
    expect(req.request.method).toBe('GET');
    req.flush(incidencia);
  });

  it('toma una incidencia abierta con POST /tomar', () => {
    servicio.tomar(4).subscribe();
    const req = http.expectOne('/api/admin/incidencias/4/tomar');
    expect(req.request.method).toBe('POST');
    req.flush({ ...incidencia, estado: 'EN_REVISION' });
  });

  it('resuelve una incidencia enviando la respuesta', () => {
    servicio.resolver(4, { respuesta: 'Actualizamos el correo manualmente.' }).subscribe();
    const req = http.expectOne('/api/admin/incidencias/4/resolver');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ respuesta: 'Actualizamos el correo manualmente.' });
    req.flush({ ...incidencia, estado: 'RESUELTA' });
  });

  it('rechaza una incidencia con POST /rechazar', () => {
    servicio.rechazar(4, { respuesta: 'No procede según las políticas.' }).subscribe();
    const req = http.expectOne('/api/admin/incidencias/4/rechazar');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ respuesta: 'No procede según las políticas.' });
    req.flush({ ...incidencia, estado: 'RECHAZADA' });
  });

  it('lista las incidencias propias del usuario autenticado', () => {
    servicio.listarPropias(2, 10).subscribe();
    const req = http.expectOne(
      (p) => p.url === '/api/cliente/incidencias' && p.params.get('pagina') === '2' && p.params.get('tamano') === '10',
    );
    expect(req.request.method).toBe('GET');
    req.flush({ contenido: [], pagina: 2, tamano: 10, totalElementos: 0, totalPaginas: 0 });
  });

  it('crea una incidencia con los datos del usuario', () => {
    const cuerpo: CrearIncidencia = {
      tipo: 'APELACION_PRODUCTO',
      asunto: 'Mi prenda fue rechazada',
      descripcion: 'Apelo la decisión porque incluí la composición.',
      referenciaId: 9,
    };
    servicio.crear(cuerpo).subscribe();

    const req = http.expectOne('/api/cliente/incidencias');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(cuerpo);
    req.flush({ ...incidencia, tipo: 'APELACION_PRODUCTO', referenciaId: 9 });
  });

  it('usa la ruta de incidencias del vendedor autenticado', () => {
    rol.set('VENDEDOR');
    servicio.listarPropias().subscribe();
    const req = http.expectOne('/api/vendedor/incidencias?pagina=0&tamano=10');
    expect(req.request.method).toBe('GET');
    req.flush({ contenido: [], pagina: 0, tamano: 10, totalElementos: 0, totalPaginas: 0 });
  });
});
