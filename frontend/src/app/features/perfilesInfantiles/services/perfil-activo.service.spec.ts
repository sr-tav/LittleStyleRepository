import { TestBed } from '@angular/core/testing';

import { PerfilInfantil } from '../models/perfil-infantil.models';
import { PerfilActivoService } from './perfil-activo.service';

const perfil = (id: number, nombre: string): PerfilInfantil => ({
  id,
  nombre,
  fechaNacimiento: '2021-03-10',
  edadAnios: 5,
  mesesRestantes: 0,
  contextura: 'MEDIA',
  holgura: 'REGULAR',
  alergias: [],
  otraAlergia: null,
  sinAlergias: true,
  coloresPreferidos: [],
  estampadosPreferidos: [],
  otroColor: null,
  otroEstampado: null,
  fechaCreacion: '2025-01-01T12:00:00',
  fechaActualizacion: '2025-01-01T12:00:00',
  ultimaMedicion: null,
  porcentajeCompletitud: 100,
  camposPendientes: [],
});

describe('PerfilActivoService', () => {
  let service: PerfilActivoService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(PerfilActivoService);
  });

  it('autoselecciona el primer perfil cuando no existe selección guardada', () => {
    service.sincronizar([perfil(1, 'Sofía'), perfil(2, 'Mateo')]);

    expect(service.perfil()?.nombre).toBe('Sofía');
    expect(localStorage.getItem('littleStyle.perfilActivoId')).toBe('1');
  });

  it('persiste la selección y la restaura si el perfil sigue en la lista', () => {
    service.sincronizar([perfil(1, 'Sofía'), perfil(2, 'Mateo')]);
    service.seleccionar(2);

    const nuevoServicio = new PerfilActivoService();
    nuevoServicio.sincronizar([perfil(1, 'Sofía'), perfil(2, 'Mateo')]);

    expect(nuevoServicio.perfil()?.nombre).toBe('Mateo');
    expect(localStorage.getItem('littleStyle.perfilActivoId')).toBe('2');
  });

  it('descarta un id guardado que ya no existe y selecciona el primero', () => {
    localStorage.setItem('littleStyle.perfilActivoId', '99');
    service.sincronizar([perfil(1, 'Sofía')]);

    expect(service.perfil()?.id).toBe(1);
    expect(localStorage.getItem('littleStyle.perfilActivoId')).toBe('1');
  });

  it('limpiar olvida el perfil activo y la selección guardada', () => {
    service.sincronizar([perfil(1, 'Sofía'), perfil(2, 'Mateo')]);
    service.seleccionar(2);

    service.limpiar();

    expect(service.perfil()).toBeNull();
    expect(localStorage.getItem('littleStyle.perfilActivoId')).toBeNull();
    expect(() => service.seleccionar(2)).toThrow(RangeError);
  });
});
