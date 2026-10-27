import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { RegistroAuditoria, RespuestaIntegridad } from '../models/admin.models';
import { AuditoriaService } from './auditoria.service';

describe('AuditoriaService (US-14)', () => {
  let http: HttpTestingController;
  let servicio: AuditoriaService;

  const registro: RegistroAuditoria = {
    id: 1,
    fechaHora: '2026-10-21T10:00:00',
    adminId: 2,
    adminEmail: 'admin@littlestyle.com',
    rol: 'ADMINISTRADOR',
    accion: 'CAMBIO_ESTADO_USUARIO',
    tipoEntidad: 'USUARIO',
    entidadId: 3,
    valorAnterior: 'ACTIVO',
    valorNuevo: 'SUSPENDIDO',
    motivo: 'Incumplimiento de normas',
    resultado: 'EXITO',
    hash: 'abc123',
    hashAnterior: null,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    servicio = TestBed.inject(AuditoriaService);
  });

  afterEach(() => http.verify());

  it('lista los registros con los filtros en la URL (solo lectura)', () => {
    servicio
      .listar({ adminEmail: 'admin@littlestyle.com', accion: 'CAMBIO_ESTADO_USUARIO', entidadId: 3, pagina: 0, tamano: 10 })
      .subscribe();

    const req = http.expectOne(
      (p) =>
        p.url === '/api/admin/auditoria' &&
        p.params.get('adminEmail') === 'admin@littlestyle.com' &&
        p.params.get('accion') === 'CAMBIO_ESTADO_USUARIO' &&
        p.params.get('entidadId') === '3' &&
        p.params.get('pagina') === '0',
    );
    expect(req.request.method).toBe('GET');
    req.flush({ contenido: [registro], pagina: 0, tamano: 10, totalElementos: 1, totalPaginas: 1 });
  });

  it('verifica la integridad de la cadena de auditoría', () => {
    let resultado: RespuestaIntegridad | null = null;
    servicio.verificarIntegridad().subscribe((res) => (resultado = res));

    const req = http.expectOne('/api/admin/auditoria/integridad');
    expect(req.request.method).toBe('GET');
    req.flush({ totalRegistros: 12, integra: true, primerInconsistenteId: null, detalle: null });
    expect(resultado).toEqual({ totalRegistros: 12, integra: true, primerInconsistenteId: null, detalle: null });
  });
});
