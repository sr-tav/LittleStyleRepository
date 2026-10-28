import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { AuthService } from '../../../../core/services/auth.service';
import { Incidencia, Pagina } from '../../../admin/models/admin.models';
import { IncidenciasService } from '../../../admin/services/incidencias.service';
import { MisIncidencias } from './mis-incidencias';

describe('MisIncidencias (US-14)', () => {
  const rol = vi.fn(() => 'CLIENTE' as 'CLIENTE' | 'VENDEDOR');
  let parametros: Record<string, string> = {};
  let respuesta: Pagina<Incidencia>;
  const listarPropias = vi.fn(() => of(respuesta));
  const crearSolicitud = vi.fn(() => of(respuesta.contenido[0]));

  const propia: Incidencia = {
    id: 8,
    reportanteEmail: 'yo@correo.com',
    tipo: 'SOPORTE_GENERAL',
    referenciaId: null,
    referenciaDetalle: null,
    asunto: 'No llega mi pedido',
    descripcion: 'El pedido no se ha movido de preparación.',
    estado: 'RESUELTA',
    respuesta: 'El pedido salió de despacho con guía COL45.',
    fechaCreacion: '2026-10-20T09:00:00',
    fechaResolucion: '2026-10-21T15:00:00',
  };

  /**
   * Crea el componente con el listado propio ya cargado.
   * @returns Fixture y elemento raíz para las aserciones.
   */
  function crear() {
    const fixture = TestBed.createComponent(MisIncidencias);
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
      contenido: [propia],
      pagina: 0,
      tamano: 10,
      totalElementos: 1,
      totalPaginas: 1,
    };
    listarPropias.mockClear();
    crearSolicitud.mockClear();
    parametros = {};
    rol.mockReturnValue('CLIENTE');
    await TestBed.configureTestingModule({
      imports: [MisIncidencias],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useFactory: () => ({ snapshot: { queryParamMap: convertToParamMap(parametros) } }),
        },
        { provide: AuthService, useValue: { rol, usuario: () => null, rutaInicio: () => '/cliente', logout: vi.fn() } },
        { provide: IncidenciasService, useFactory: () => ({ listarPropias, crear: crearSolicitud }) },
      ],
    }).compileComponents();
  });

  it('muestra sus incidencias con estado y respuesta del equipo', () => {
    const { el } = crear();
    expect(el.textContent).toContain('No llega mi pedido');
    expect(el.textContent).toContain('Resuelta');
    expect(el.textContent).toContain('El pedido salió de despacho con guía COL45.');
    expect(listarPropias).toHaveBeenCalledWith(0, 10);
  });

  it('crea una incidencia con el formulario válido', () => {
    const { fixture, el } = crear();
    escribir(el.querySelector<HTMLInputElement>('#incidencia-asunto')!, 'No puedo usar mi cupón');
    escribir(el.querySelector<HTMLTextAreaElement>('#incidencia-descripcion')!, 'El cupón de descuento no se aplica al pagar.');
    el.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    fixture.detectChanges();

    expect(crearSolicitud).toHaveBeenCalledWith({
      tipo: 'SOPORTE_GENERAL',
      asunto: 'No puedo usar mi cupón',
      descripcion: 'El cupón de descuento no se aplica al pagar.',
    });
    expect(el.textContent).toContain('fue registrada');
    expect(listarPropias).toHaveBeenCalledTimes(2);
  });

  it('permite al cliente reportar una prenda con referencia', () => {
    const { fixture, el } = crear();
    const tipo = el.querySelector<HTMLSelectElement>('#incidencia-tipo')!;
    tipo.value = 'REPORTE_PRODUCTO';
    tipo.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    const referencia = el.querySelector<HTMLInputElement>('#incidencia-referencia')!;
    expect(referencia).not.toBeNull();

    escribir(el.querySelector<HTMLInputElement>('#incidencia-asunto')!, 'Revisen mi prenda');
    escribir(el.querySelector<HTMLTextAreaElement>('#incidencia-descripcion')!, 'Apelo el rechazo porque las tallas son correctas.');
    escribir(referencia, '9');
    fixture.detectChanges();
    el.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    fixture.detectChanges();

    expect(crearSolicitud).toHaveBeenCalledWith({
      tipo: 'REPORTE_PRODUCTO',
      asunto: 'Revisen mi prenda',
      descripcion: 'Apelo el rechazo porque las tallas son correctas.',
      referenciaId: 9,
    });

  });

  it('preselecciona la apelación solicitada desde el catálogo del vendedor', () => {
    rol.mockReturnValue('VENDEDOR');
    parametros = { tipo: 'APELACION_PRODUCTO', referenciaId: '9' };
    const { fixture, el } = crear();
    expect(el.querySelector<HTMLSelectElement>('#incidencia-tipo')?.value).toBe('APELACION_PRODUCTO');
    expect(el.querySelector<HTMLInputElement>('#incidencia-referencia')?.value).toBe('9');
    expect(el.querySelector<HTMLInputElement>('#incidencia-asunto')?.value).toBe('Apelación de la prenda #9');

    escribir(el.querySelector<HTMLTextAreaElement>('#incidencia-descripcion')!, 'Solicito revisar la desactivación de mi publicación.');
    el.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    fixture.detectChanges();

    expect(crearSolicitud).toHaveBeenCalledWith(expect.objectContaining({
      tipo: 'APELACION_PRODUCTO',
      referenciaId: 9,
    }));
  });

  it('no ofrece apelación de producto al cliente', () => {
    const { el } = crear();
    const tipos = Array.from(el.querySelectorAll<HTMLSelectElement>('#incidencia-tipo option'))
      .map((opcion) => opcion.value);
    expect(tipos).not.toContain('APELACION_PRODUCTO');
    expect(tipos).toContain('REPORTE_PRODUCTO');
  });

  it('no envía con asunto o descripción demasiado cortos', () => {
    const { fixture, el } = crear();
    escribir(el.querySelector<HTMLInputElement>('#incidencia-asunto')!, 'No');
    escribir(el.querySelector<HTMLTextAreaElement>('#incidencia-descripcion')!, 'Muy breve.');
    el.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    fixture.detectChanges();

    expect(el.textContent).toContain('entre 5 y 200 caracteres');
    expect(el.textContent).toContain('entre 20 y 4000 caracteres');
    expect(crearSolicitud).not.toHaveBeenCalled();
  });
});
