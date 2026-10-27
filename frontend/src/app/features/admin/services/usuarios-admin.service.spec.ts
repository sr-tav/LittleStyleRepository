import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CambioEstadoUsuario, UsuarioAdmin } from '../models/admin.models';
import { UsuariosAdminService } from './usuarios-admin.service';

describe('UsuariosAdminService (US-14)', () => {
  let http: HttpTestingController;
  let servicio: UsuariosAdminService;

  const usuario: UsuarioAdmin = {
    id: 3,
    nombre: 'Ana',
    apellido: 'Pérez',
    email: 'ana@correo.com',
    telefono: '3001234567',
    nombreTienda: null,
    rol: 'CLIENTE',
    estado: 'ACTIVO',
    fechaRegistro: '2026-10-01T10:00:00',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    servicio = TestBed.inject(UsuariosAdminService);
  });

  afterEach(() => http.verify());

  it('lista los usuarios con los filtros y la paginación en la URL', () => {
    servicio
      .listar({ rol: 'VENDEDOR', estado: 'ACTIVO', texto: 'Ana', pagina: 1, tamano: 10 })
      .subscribe();

    const req = http.expectOne(
      (p) =>
        p.url === '/api/admin/usuarios' &&
        p.params.get('rol') === 'VENDEDOR' &&
        p.params.get('estado') === 'ACTIVO' &&
        p.params.get('texto') === 'Ana' &&
        p.params.get('pagina') === '1' &&
        p.params.get('tamano') === '10',
    );
    expect(req.request.method).toBe('GET');
    req.flush({ contenido: [usuario], pagina: 1, tamano: 10, totalElementos: 1, totalPaginas: 1 });
  });

  it('omite los filtros vacíos del listado', () => {
    servicio.listar({ rol: undefined, estado: undefined, texto: '', pagina: 0, tamano: 10 }).subscribe();
    const req = http.expectOne((p) => p.url === '/api/admin/usuarios');
    expect(req.request.params.has('rol')).toBe(false);
    expect(req.request.params.has('estado')).toBe(false);
    expect(req.request.params.has('texto')).toBe(false);
    expect(req.request.params.get('pagina')).toBe('0');
    req.flush({ contenido: [], pagina: 0, tamano: 10, totalElementos: 0, totalPaginas: 0 });
  });

  it('pide el detalle de un usuario por id', () => {
    servicio.detalle(3).subscribe();
    const req = http.expectOne('/api/admin/usuarios/3');
    expect(req.request.method).toBe('GET');
    req.flush(usuario);
  });

  it('cambia el estado con PUT y envía el motivo', () => {
    const cuerpo: CambioEstadoUsuario = { estado: 'SUSPENDIDO', motivo: 'Incumplimiento de normas' };
    servicio.cambiarEstado(3, cuerpo).subscribe();

    const req = http.expectOne('/api/admin/usuarios/3/estado');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(cuerpo);
    req.flush({ ...usuario, estado: 'SUSPENDIDO' });
  });
});
