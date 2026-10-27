import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { FiltrosUsuarios, Pagina, UsuarioAdmin } from '../../../admin/models/admin.models';
import { UsuariosAdminService } from '../../../admin/services/usuarios-admin.service';
import { UsuariosAdminPage } from './usuarios-admin';

describe('UsuariosAdminPage (US-14)', () => {
  let respuesta: Pagina<UsuarioAdmin>;
  const listar = vi.fn((_filtros: FiltrosUsuarios) => of(respuesta));
  const cambiarEstado = vi.fn(() => of({ ...respuesta.contenido[0] }));
  const detalle = vi.fn(() => of(respuesta.contenido[0]));

  function crearUsuario(extra: Partial<UsuarioAdmin> = {}): UsuarioAdmin {
    return {
      id: 3,
      nombre: 'María',
      apellido: 'López',
      email: 'maria@correo.com',
      telefono: '3009876543',
      nombreTienda: null,
      rol: 'CLIENTE',
      estado: 'ACTIVO',
      fechaRegistro: '2026-10-01T10:00:00',
      ...extra,
    };
  }

  /** Crea el componente con la página ya cargada (el servicio responde en línea). */
  function crear(): { fixture: ReturnType<TestBed['createComponent']>; el: HTMLElement } {
    const fixture = TestBed.createComponent(UsuariosAdminPage);
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  /** Escribe el valor en un control y dispara su evento input o change. */
  function cambiar(control: HTMLElement, valor: string, evento = 'input'): void {
    (control as HTMLInputElement).value = valor;
    control.dispatchEvent(new Event(evento));
  }

  beforeEach(async () => {
    respuesta = {
      contenido: [crearUsuario(), crearUsuario({ id: 4, rol: 'ADMINISTRADOR' }), crearUsuario({ id: 5, estado: 'ELIMINADO' })],
      pagina: 0,
      tamano: 10,
      totalElementos: 3,
      totalPaginas: 1,
    };
    listar.mockClear();
    cambiarEstado.mockClear();
    detalle.mockClear();
    await TestBed.configureTestingModule({
      imports: [UsuariosAdminPage],
      providers: [
        provideRouter([]),
        {
          provide: UsuariosAdminService,
          useFactory: () => ({ listar, cambiarEstado, detalle }),
        },
      ],
    }).compileComponents();
  });

  it('lista los usuarios con su rol y estado', () => {
    const { el } = crear();
    expect(el.textContent).toContain('María López');
    expect(el.textContent).toContain('maria@correo.com');
    expect(el.textContent).toContain('Cliente');
    expect(el.textContent).toContain('Eliminado');
    expect(listar).toHaveBeenCalledWith(expect.objectContaining({ pagina: 0, tamano: 10 }));
  });

  it('aplica los filtros de rol, estado y texto', () => {
    const { fixture, el } = crear();
    cambiar(el.querySelector<HTMLSelectElement>('#filtro-rol')!, 'VENDEDOR', 'change');
    cambiar(el.querySelector<HTMLSelectElement>('#filtro-estado')!, 'SUSPENDIDO', 'change');
    cambiar(el.querySelector<HTMLInputElement>('#filtro-texto')!, 'loay');
    el.querySelector<HTMLButtonElement>('.admin-filtros .btn-primary')!.click();
    fixture.detectChanges();

    const filtros = listar.mock.calls[listar.mock.calls.length - 1][0];
    expect(filtros).toMatchObject({ rol: 'VENDEDOR', estado: 'SUSPENDIDO', texto: 'loay' });
  });

  it('no ofrece acciones sobre administradores ni cuentas eliminadas', () => {
    const { el } = crear();
    const botones = el.querySelectorAll<HTMLButtonElement>('tbody .btn-soft');
    expect(botones.length).toBe(1);
    expect(el.textContent).toContain('Administrador');
    expect(el.textContent).toContain('Eliminado');
  });

  it('abre el diálogo con los datos del usuario y las acciones válidas', async () => {
    const { fixture, el } = crear();
    el.querySelector<HTMLButtonElement>('tbody .btn-soft')!.click();
    fixture.detectChanges();
    await new Promise((resolve) => setTimeout(resolve, 0));

    const dialogo = el.querySelector('[role="dialog"]');
    expect(dialogo).not.toBeNull();
    expect(dialogo?.getAttribute('aria-modal')).toBe('true');
    expect(el.textContent).toContain('maria@correo.com');
    expect(el.textContent).toContain('Suspender');
    expect(el.textContent).toContain('Bloquear');
    expect(el.querySelector<HTMLTextAreaElement>('#motivo-desactivacion')!.matches(':focus')).toBe(true);
  });

  it('exige un motivo de al menos 10 caracteres', () => {
    const { fixture, el } = crear();
    el.querySelector<HTMLButtonElement>('tbody .btn-soft')!.click();
    fixture.detectChanges();
    el.querySelectorAll<HTMLButtonElement>('.confirm-actions .btn-primary')[0].click();
    fixture.detectChanges();

    expect(el.textContent).toContain('al menos 10 caracteres');
    expect(cambiarEstado).not.toHaveBeenCalled();
  });

  it('confirma el cambio con PUT y recarga la lista', () => {
    const { fixture, el } = crear();
    cambiarEstado.mockReturnValueOnce(of(crearUsuario({ estado: 'SUSPENDIDO' })));
    el.querySelector<HTMLButtonElement>('tbody .btn-soft')!.click();
    fixture.detectChanges();

    cambiar(el.querySelector<HTMLTextAreaElement>('#motivo-desactivacion')!, 'Incumplimiento de las normas');
    fixture.detectChanges();
    el.querySelectorAll<HTMLButtonElement>('.confirm-actions .btn-primary')[0].click();
    fixture.detectChanges();

    expect(cambiarEstado).toHaveBeenCalledWith(3, {
      estado: 'SUSPENDIDO',
      motivo: 'Incumplimiento de las normas',
    });
    expect(el.textContent).toContain('se actualizó correctamente');
    expect(listar).toHaveBeenCalledTimes(2);
  });
});
