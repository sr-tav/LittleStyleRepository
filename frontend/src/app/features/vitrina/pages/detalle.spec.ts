import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { signal } from '@angular/core';

import { CarritoService } from '../../catalogoCompras/services/carrito.service';
import { VitrinaDetalle } from '../models/vitrina.models';
import { DetalleProducto } from './detalle';

const carritoMock = {
  agregar: vi.fn(() => of({ items: [], cantidad: 0, subtotal: 0, costoEnvio: null, total: null })),
  abrirPanel: vi.fn(),
  panelAbierto: signal(false),
  items: signal([]),
  cantidad: signal(0),
  subtotal: signal(0),
  alternarPanel: vi.fn(),
  cerrarPanel: vi.fn(),
};

function detalle(cambios: Partial<VitrinaDetalle> = {}): VitrinaDetalle {
  return {
    id: 10,
    nombre: 'Pantalón Jogger Azul',
    categoria: 'PANTALONES',
    genero: 'UNISEX',
    descripcion: null,
    instruccionesCuidado: 'Lavar a mano en agua fría',
    marca: 'Tienda Sol',
    precio: 32900,
    imagenes: ['/media/a.png', '/media/b.png'],
    disponible: true,
    colores: ['AZUL'],
    estampado: 'LISO',
    composicion: [{ material: 'ALGODON', porcentaje: 100 }],
    tallas: [
      { talla: '4', estaturaMinCm: 100, estaturaMaxCm: 110, pesoMinKg: 15, pesoMaxKg: 20, stockDisponible: 3, disponible: true },
      { talla: '6', estaturaMinCm: 111, estaturaMaxCm: 122, pesoMinKg: 21, pesoMaxKg: 26, stockDisponible: 0, disponible: false },
    ],
    fechaActualizacion: new Date().toISOString(),
    ...cambios,
  };
}

describe('DetalleProducto (US-09)', () => {
  let perfilId: string | null = null;

  beforeEach(async () => {
    perfilId = null;
    await TestBed.configureTestingModule({
      imports: [DetalleProducto],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: CarritoService, useValue: carritoMock },
        {
          provide: ActivatedRoute,
          useFactory: () => ({
            snapshot: {
              paramMap: convertToParamMap({ id: '10' }),
              queryParamMap: convertToParamMap(perfilId ? { perfilId } : {}),
            },
          }),
        },
      ],
    }).compileComponents();
  });

  function crear(respuesta: VitrinaDetalle) {
    const fixture = TestBed.createComponent(DetalleProducto);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/cliente/catalogo/10').flush(respuesta);
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement, http };
  }

  it('muestra miniaturas, navega con flechas y marca la foto actual', () => {
    const { fixture, el, http } = crear(detalle());
    http.verify();

    const thumbs = el.querySelectorAll('.galeria-thumb');
    expect(thumbs.length).toBe(2);
    const visor = () => el.querySelector('.galeria-viewer img')?.getAttribute('src');
    expect(visor()).toBe('/media/a.png');

    el.querySelector<HTMLButtonElement>('.galeria-flecha-siguiente')!.click();
    fixture.detectChanges();
    expect(visor()).toBe('/media/b.png');
    expect(el.querySelector('.galeria-contador')?.textContent).toContain('2 / 2');

    el.querySelector<HTMLButtonElement>('.galeria-flecha-siguiente')!.click();
    fixture.detectChanges();
    expect(visor()).toBe('/media/a.png');
  });

  it('muestra la ficha técnica textil con composición y cuidado (US-09)', () => {
    const { el, http } = crear(detalle());
    http.verify();

    const ficha = el.querySelector('[aria-label="Ficha técnica textil"]');
    expect(ficha?.textContent).toContain('Algodón');
    expect(ficha?.textContent).toContain('100');
    expect(ficha?.textContent).toContain('Lavar a mano en agua fría');
  });

  it('muestra insignia de nuevo y tabla de medidas con estados', () => {
    const { el, http } = crear(detalle());
    http.verify();

    expect(el.textContent).toContain('Nuevo');
    expect(el.textContent).toContain('100 – 110');
    expect(el.textContent).toContain('Agotada');
    expect(el.querySelector('.tabla-medidas caption')).not.toBeNull();
  });

  it('muestra el stock por talla y limita la cantidad con los controles +/-', () => {
    carritoMock.agregar.mockClear();
    carritoMock.abrirPanel.mockClear();
    const { fixture, el, http } = crear(detalle());
    http.verify();

    expect(el.textContent).toContain('3');
    const selector = el.querySelector<HTMLSelectElement>('#talla-carrito')!;
    selector.value = '4';
    selector.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    const aumentar = el.querySelector<HTMLButtonElement>('[aria-label="Aumentar cantidad"]')!;
    aumentar.click();
    aumentar.click();
    fixture.detectChanges();
    expect(el.querySelector('output')?.textContent).toBe('3');
    expect(aumentar.disabled).toBe(true);

    el.querySelector<HTMLButtonElement>('[aria-label="Disminuir cantidad"]')!.click();
    fixture.detectChanges();
    expect(el.querySelector('output')?.textContent).toBe('2');

    el.querySelector<HTMLButtonElement>('.detalle-agregar-carrito > .btn-primary')!.click();
    expect(carritoMock.agregar).toHaveBeenCalledWith({ prendaId: 10, talla: '4', cantidad: 2, tallaSugerida: null });
    expect(carritoMock.abrirPanel).toHaveBeenCalledOnce();
  });

  it('deshabilita la compra y avisa cuando no hay unidades disponibles', () => {
    const agotada = detalle({
      disponible: false,
      tallas: [
        { talla: '4', estaturaMinCm: 100, estaturaMaxCm: 110, pesoMinKg: 15, pesoMaxKg: 20, stockDisponible: 0, disponible: false },
      ],
    });
    const { el, http } = crear(agotada);
    http.verify();

    expect(el.textContent).toContain('En este momento no hay unidades disponibles.');
    expect(el.querySelector<HTMLButtonElement>('.detalle-agregar-carrito > .btn-primary')?.disabled).toBe(true);
    expect(el.querySelector<HTMLSelectElement>('#talla-carrito')?.disabled).toBe(true);
  });

  it('muestra el motivo de exclusión cuando el 422 trae código PRENDA_EXCLUIDA_ALERGIA (US-07)', () => {
    perfilId = '3';
    const { fixture, el, http } = crear(detalle());
    http.expectOne((p) => p.url === '/api/cliente/recomendaciones/prenda/10')
      .flush({ mensaje: 'Prenda excluida por alergia: Contiene níquel', codigo: 'PRENDA_EXCLUIDA_ALERGIA' }, { status: 422, statusText: 'Unprocessable Entity' });
    fixture.detectChanges();
    http.verify();

    expect(el.textContent).toContain('No compatible');
    expect(el.textContent).toContain('Contiene níquel');
    expect(el.textContent).not.toContain('Si tienes alergias textiles');
  });

  it('no muestra exclusión cuando el 422 trae otro código (US-07)', () => {
    perfilId = '3';
    const { fixture, el, http } = crear(detalle());
    http.expectOne((p) => p.url === '/api/cliente/recomendaciones/prenda/10')
      .flush({ mensaje: 'Sin talla compatible', codigo: 'SIN_TALLA_COMPATIBLE' }, { status: 422, statusText: 'Unprocessable Entity' });
    fixture.detectChanges();
    http.verify();

    expect(el.textContent).not.toContain('No compatible');
  });
});
