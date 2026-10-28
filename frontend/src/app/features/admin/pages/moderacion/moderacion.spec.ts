import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';

import {
  Pagina,
  FiltrosModeracion,
  PrendaAdmin,
  PrendaAdminDetalle,
} from '../../../admin/models/admin.models';
import { ModeracionService } from '../../../admin/services/moderacion.service';
import { ModeracionPage } from './moderacion';

describe('ModeracionPage (US-14)', () => {
  let respuesta: Pagina<PrendaAdmin>;
  const listar = vi.fn((_filtros: FiltrosModeracion) => of(respuesta));
  const detalle = vi.fn(() => of(null as unknown as PrendaAdminDetalle));
  const aprobar = vi.fn(() => of({ ...respuesta.contenido[0] }));
  const desactivar = vi.fn(() => of({ ...respuesta.contenido[0] }));
  const restaurar = vi.fn(() => of({ ...respuesta.contenido[0] }));

  const fila: PrendaAdmin = {
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

  const detallePrenda: PrendaAdminDetalle = {
    ...fila,
    categoria: 'CAMISETAS',
    genero: 'NINO',
    descripcion: 'Algodón orgánico suave',
    instruccionesCuidado: 'Lavar en frío',
    marca: 'LittleStyle',
    estampado: null,
    tintesSinteticos: false,
    brochesMetalicos: false,
    contieneNiquel: false,
    tratamientoFormaldehido: false,
    fechaCreacion: '2026-10-19T08:00:00',
    composicion: [{ material: 'Algodón', porcentaje: 100 }],
    tallas: [{ talla: '4', stock: 3, estaturaMinCm: 98, estaturaMaxCm: 104, pesoMinKg: 14, pesoMaxKg: 17, edadMinMeses: 36, edadMaxMeses: 48, stockMinimo: 1 }],
    imagenes: ['/media/camiseta.png'],
  };

  /**
   * Crea el componente con la cola ya cargada.
   * @returns Fixture y elemento raíz para las aserciones.
   */
  function crear() {
    const fixture = TestBed.createComponent(ModeracionPage);
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
      contenido: [fila],
      pagina: 0,
      tamano: 10,
      totalElementos: 1,
      totalPaginas: 1,
    };
    listar.mockClear();
    detalle.mockClear();
    aprobar.mockClear();
    desactivar.mockClear();
    restaurar.mockClear();
    await TestBed.configureTestingModule({
      imports: [ModeracionPage],
      providers: [
        provideRouter([]),
        {
          provide: ModeracionService,
          useFactory: () => ({ listar, detalle, aprobar, desactivar, restaurar }),
        },
      ],
    }).compileComponents();
  });

  it('muestra la cola con vendedor y estado de moderación', () => {
    const { el } = crear();
    expect(el.textContent).toContain('Camiseta bears');
    expect(el.textContent).toContain('tienda@correo.com');
    expect(el.textContent).toContain('Sin revisar');
    expect(listar).toHaveBeenCalledWith(expect.objectContaining({ pagina: 0, tamano: 10 }));
  });

  it('aplica el filtro de estado de moderación', () => {
    const { fixture, el } = crear();
    const filtro = el.querySelector<HTMLSelectElement>('#filtro-moderacion')!;
    filtro.value = 'DESACTIVADA';
    filtro.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    expect(listar.mock.calls[listar.mock.calls.length - 1][0]).toMatchObject({
      estadoModeracion: 'DESACTIVADA',
    });
  });

  it('carga el detalle completo de la prenda seleccionada', () => {
    const { fixture, el } = crear();
    detalle.mockReturnValueOnce(of(detallePrenda));
    el.querySelector<HTMLButtonElement>('tbody .btn-soft')!.click();
    fixture.detectChanges();

    expect(detalle).toHaveBeenCalledWith(9);
    expect(el.textContent).toContain('Composición');
    expect(el.textContent).toContain('Algodón · 100%');
    const celdas = el.querySelectorAll('.detalle-tabla tbody tr:first-child td');
    expect(celdas[0].textContent?.trim()).toBe('4');
    expect(celdas[1].textContent?.trim()).toBe('3');
    expect(celdas[2].textContent).toContain('98–104 cm');
    expect(el.textContent).toContain('Broches metálicos');
  });

  it('aprueba la prenda y recarga la cola', () => {
    const { fixture, el } = crear();
    detalle.mockReturnValueOnce(of(detallePrenda));
    el.querySelector<HTMLButtonElement>('tbody .btn-soft')!.click();
    fixture.detectChanges();

    el.querySelector<HTMLButtonElement>('.detalle-acciones .btn-primary')!.click();
    fixture.detectChanges();

    expect(aprobar).toHaveBeenCalledWith(9);
    expect(el.textContent).toContain('fue aprobada');
    expect(listar).toHaveBeenCalledTimes(2);
    expect(el.querySelector('[role="dialog"]')).toBeNull();
  });

  it('exige un motivo de al menos 10 caracteres para desactivar', () => {
    const { fixture, el } = crear();
    detalle.mockReturnValueOnce(of(detallePrenda));
    el.querySelector<HTMLButtonElement>('tbody .btn-soft')!.click();
    fixture.detectChanges();

    el.querySelector<HTMLButtonElement>('.detalle-acciones .btn-ghost')!.click();
    fixture.detectChanges();

    el.querySelector<HTMLButtonElement>('.confirm-actions .btn-primary')!.click();
    fixture.detectChanges();
    expect(el.textContent).toContain('al menos 10 caracteres');
    expect(desactivar).not.toHaveBeenCalled();

    escribir(el.querySelector<HTMLTextAreaElement>('#motivo-desactivacion-prenda')!, 'Falta la composición del material');
    fixture.detectChanges();
    el.querySelector<HTMLButtonElement>('.confirm-actions .btn-primary')!.click();
    fixture.detectChanges();

    expect(desactivar).toHaveBeenCalledWith(9, 'Falta la composición del material');
    expect(el.textContent).toContain('fue desactivada');
  });
});
