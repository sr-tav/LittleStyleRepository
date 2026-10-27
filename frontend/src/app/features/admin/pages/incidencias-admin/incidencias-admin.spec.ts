import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { Incidencia, Pagina } from '../../../admin/models/admin.models';
import { IncidenciasService } from '../../../admin/services/incidencias.service';
import { IncidenciasAdminPage } from './incidencias-admin';

describe('IncidenciasAdminPage (US-14)', () => {
  let respuesta: Pagina<Incidencia>;
  const listar = vi.fn(() => of(respuesta));
  const tomar = vi.fn(() => of({ ...respuesta.contenido[0] }));
  const resolver = vi.fn(() => of({ ...respuesta.contenido[0] }));
  const rechazar = vi.fn(() => of({ ...respuesta.contenido[0] }));

  const incidencia: Incidencia = {
    id: 4,
    reportanteEmail: 'vendedor@correo.com',
    tipo: 'APELACION_SUSPENSION',
    referenciaId: null,
    referenciaDetalle: null,
    asunto: 'Mi cuenta fue suspendida sin aviso',
    descripcion: 'No recibí ningún aviso de suspensión.',
    estado: 'ABIERTA',
    respuesta: null,
    fechaCreacion: '2026-10-21T09:00:00',
    fechaResolucion: null,
  };

  /**
   * Crea el componente con la bandeja ya cargada.
   * @returns Fixture y elemento raíz para las aserciones.
   */
  function crear() {
    const fixture = TestBed.createComponent(IncidenciasAdminPage);
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  /**
   * Escribe un valor y dispara el evento input del control.
   * @template T Tipo del control.
   * @param control Control cuyo valor cambia.
   * @param valor Nuevo valor.
   */
  function escribir<T extends HTMLElement>(control: T, valor: string): void {
    (control as unknown as HTMLInputElement).value = valor;
    control.dispatchEvent(new Event('input'));
  }

  beforeEach(async () => {
    respuesta = {
      contenido: [incidencia],
      pagina: 0,
      tamano: 10,
      totalElementos: 1,
      totalPaginas: 1,
    };
    listar.mockClear();
    tomar.mockClear();
    resolver.mockClear();
    rechazar.mockClear();
    await TestBed.configureTestingModule({
      imports: [IncidenciasAdminPage],
      providers: [
        provideRouter([]),
        { provide: IncidenciasService, useFactory: () => ({ listar, tomar, resolver, rechazar }) },
      ],
    }).compileComponents();
  });

  it('muestra la bandeja con tipo, estado y fechas de atención', () => {
    const { el } = crear();
    expect(el.textContent).toContain('Mi cuenta fue suspendida sin aviso');
    expect(el.textContent).toContain('Apelación de suspensión');
    expect(el.textContent).toContain('Abierta');
    expect(listar).toHaveBeenCalledWith(expect.objectContaining({ pagina: 0, tamano: 10 }));
  });

  it('toma una incidencia abierta y pasa a revisión', () => {
    const { fixture, el } = crear();
    const acciones = el.querySelector<HTMLSelectElement>('.acciones-select')!;
    acciones.value = 'TOMAR';
    acciones.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    expect(tomar).toHaveBeenCalledWith(4);
    expect(el.textContent).toContain('pasó a revisión');
    expect(listar).toHaveBeenCalledTimes(2);
  });

  it('resuelve con una respuesta y recarga la bandeja', () => {
    respuesta.contenido = [{ ...incidencia, estado: 'EN_REVISION' }];
    const { fixture, el } = crear();
    const acciones = el.querySelector<HTMLSelectElement>('.acciones-select')!;
    expect(Array.from(acciones.options).map((option) => option.value)).toEqual(['', 'RESOLVER']);
    acciones.value = 'RESOLVER';
    acciones.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    escribir(el.querySelector<HTMLTextAreaElement>('#respuesta-incidencia')!, 'Reactivamos tu cuenta y enviamos el aviso.');
    fixture.detectChanges();
    el.querySelector<HTMLButtonElement>('.confirm-actions .btn-primary')!.click();
    fixture.detectChanges();

    expect(resolver).toHaveBeenCalledWith(4, { respuesta: 'Reactivamos tu cuenta y enviamos el aviso.' });
    expect(el.textContent).toContain('se resolvió');
    expect(el.querySelector('[role="dialog"]')).toBeNull();
  });

  it('exige respuesta de al menos 10 caracteres para decidir', () => {
    respuesta.contenido = [{ ...incidencia, estado: 'ABIERTA' }];
    const { fixture, el } = crear();
    const acciones = el.querySelector<HTMLSelectElement>('.acciones-select')!;
    expect(Array.from(acciones.options).map((option) => option.value)).toEqual(['', 'TOMAR', 'RECHAZAR']);
    acciones.value = 'RECHAZAR';
    acciones.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    el.querySelector<HTMLButtonElement>('.confirm-actions .btn-primary')!.click();
    fixture.detectChanges();

    expect(el.textContent).toContain('al menos 10 caracteres');
    expect(rechazar).not.toHaveBeenCalled();

    escribir(el.querySelector<HTMLTextAreaElement>('#respuesta-incidencia')!, 'La apelación no procede según las políticas.');
    fixture.detectChanges();
    el.querySelector<HTMLButtonElement>('.confirm-actions .btn-primary')!.click();
    fixture.detectChanges();

    expect(rechazar).toHaveBeenCalledWith(4, { respuesta: 'La apelación no procede según las políticas.' });
  });
});
