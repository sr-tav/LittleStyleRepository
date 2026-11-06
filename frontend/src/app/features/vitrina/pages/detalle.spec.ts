import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { signal } from '@angular/core';

import { CarritoService } from '../../catalogoCompras/services/carrito.service';
import { IncidenciasService } from '../../admin/services/incidencias.service';
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
const crearIncidencia = vi.fn(() => of({ id: 21 }));

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
      {
        talla: '4',
        color: 'AZUL',
        estaturaMinCm: 100,
        estaturaMaxCm: 110,
        pesoMinKg: 15,
        pesoMaxKg: 20,
        stockDisponible: 3,
        disponible: true,
      },
      {
        talla: '6',
        color: 'AZUL',
        estaturaMinCm: 111,
        estaturaMaxCm: 122,
        pesoMinKg: 21,
        pesoMaxKg: 26,
        stockDisponible: 0,
        disponible: false,
      },
    ],
    fechaActualizacion: new Date().toISOString(),
    ...cambios,
  };
}

describe('DetalleProducto (US-09)', () => {
  let perfilId: string | null = null;

  beforeEach(async () => {
    perfilId = null;
    crearIncidencia.mockClear();
    await TestBed.configureTestingModule({
      imports: [DetalleProducto],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: CarritoService, useValue: carritoMock },
        { provide: IncidenciasService, useValue: { crear: crearIncidencia } },
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

  it('reporta la prenda asociando la incidencia y exige descripción suficiente (US-14)', () => {
    const { fixture, el, http } = crear(detalle());
    http.verify();
    el.querySelector<HTMLButtonElement>('#reportar-prenda')!.click();
    fixture.detectChanges();

    el.querySelector<HTMLButtonElement>('.confirm-actions .btn-primary')!.click();
    fixture.detectChanges();
    expect(el.textContent).toContain('descripción entre 20 y 4000');
    expect(crearIncidencia).not.toHaveBeenCalled();

    const descripcion = el.querySelector<HTMLTextAreaElement>('#descripcion-reporte')!;
    descripcion.value = 'La información de composición no coincide con la etiqueta.';
    descripcion.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    el.querySelector<HTMLButtonElement>('.confirm-actions .btn-primary')!.click();
    fixture.detectChanges();

    expect(crearIncidencia).toHaveBeenCalledWith({
      tipo: 'REPORTE_PRODUCTO',
      asunto: 'Reporte de producto: Pantalón Jogger Azul',
      descripcion: 'La información de composición no coincide con la etiqueta.',
      referenciaId: 10,
      referenciaDetalle: 'Pantalón Jogger Azul',
    });
    expect(el.textContent).toContain('reporte quedó registrado');
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
    const tabla = el.querySelector<HTMLTableElement>('.tabla-medidas');
    expect(tabla?.querySelectorAll('thead th')).toHaveLength(5);
    expect(tabla?.textContent).not.toContain('Color');
    expect(tabla?.textContent).not.toContain('Estado');

    const filas = tabla?.querySelectorAll('tbody tr');
    expect(filas).toHaveLength(2);
    expect(filas?.[0].textContent).toContain('3');
    expect(filas?.[1].textContent).toContain('0');    expect(el.querySelector('.tabla-medidas caption')).not.toBeNull();
  });

  it('muestra el stock de la variante seleccionada y limita la cantidad', () => {
    carritoMock.agregar.mockClear();
    carritoMock.abrirPanel.mockClear();

    const { fixture, el, http } = crear(detalle());
    http.verify();

    const botonColor = el.querySelector<HTMLButtonElement>(
      '.detalle-color[role="radio"]',
    )!;
    botonColor.click();
    fixture.detectChanges();

    const botonTalla = el.querySelector<HTMLButtonElement>(
      '.detalle-talla[aria-label^="Talla 4"]',
    )!;
    botonTalla.click();
    fixture.detectChanges();

    expect(el.textContent).toContain('3 disponibles');

    const aumentar = el.querySelector<HTMLButtonElement>(
      '[aria-label="Aumentar cantidad"]',
    )!;
    aumentar.click();
    aumentar.click();
    fixture.detectChanges();

    expect(el.querySelector('output')?.textContent).toBe('3');
    expect(aumentar.disabled).toBe(true);

    el.querySelector<HTMLButtonElement>(
      '[aria-label="Disminuir cantidad"]',
    )!.click();
    fixture.detectChanges();

    expect(el.querySelector('output')?.textContent).toBe('2');

    el.querySelector<HTMLButtonElement>(
      '.detalle-agregar-carrito > .btn-primary',
    )!.click();

    expect(carritoMock.agregar).toHaveBeenCalledWith({
      prendaId: 10,
      talla: '4',
      cantidad: 2,
      tallaSugerida: null,
      color: 'AZUL',
      estampado: 'LISO',
    });
    expect(carritoMock.abrirPanel).toHaveBeenCalledOnce();
  });

  it('mantiene separadas las tallas y el stock de cada color y agrega la variante elegida', () => {
    carritoMock.agregar.mockClear();
    carritoMock.abrirPanel.mockClear();

    const prendaConVariantes = detalle({
      colores: ['AZUL', 'ROSA'],
      tallas: [
        {
          talla: '4',
          color: 'AZUL',
          estaturaMinCm: 100,
          estaturaMaxCm: 110,
          pesoMinKg: 15,
          pesoMaxKg: 20,
          stockDisponible: 0,
          disponible: false,
        },
        {
          talla: '4',
          color: 'ROSA',
          estaturaMinCm: 100,
          estaturaMaxCm: 110,
          pesoMinKg: 15,
          pesoMaxKg: 20,
          stockDisponible: 2,
          disponible: true,
        },
        {
          talla: '6',
          color: 'ROSA',
          estaturaMinCm: 111,
          estaturaMaxCm: 122,
          pesoMinKg: 21,
          pesoMaxKg: 26,
          stockDisponible: 0,
          disponible: false,
        },
      ],
    });

    const { fixture, el, http } = crear(prendaConVariantes);
    http.verify();

    const botonRosa = el.querySelector<HTMLButtonElement>(
      '.detalle-color[aria-label^="Rosa"]',
    )!;
    expect(botonRosa.disabled).toBe(false);
    botonRosa.click();
    fixture.detectChanges();

    const tallas = Array.from(
      el.querySelectorAll<HTMLButtonElement>('.detalle-talla'),
    );
    expect(tallas).toHaveLength(2);
    expect(tallas[0].getAttribute('aria-label')).toContain('Talla 4');
    expect(tallas[0].disabled).toBe(false);
    expect(tallas[1].getAttribute('aria-label')).toContain('Talla 6');
    expect(tallas[1].disabled).toBe(true);

    tallas[0].click();
    fixture.detectChanges();

    el.querySelector<HTMLButtonElement>(
      '.detalle-agregar-carrito > .btn-primary',
    )!.click();

    expect(carritoMock.agregar).toHaveBeenCalledWith({
      prendaId: 10,
      talla: '4',
      cantidad: 1,
      tallaSugerida: null,
      color: 'ROSA',
      estampado: 'LISO',
    });
    expect(carritoMock.abrirPanel).toHaveBeenCalledOnce();
  });

  it('deshabilita la selección y la compra cuando el producto está agotado', () => {
    carritoMock.agregar.mockClear();

    const agotada = detalle({
      disponible: false,
      colores: ['AZUL'],
      tallas: [
        {
          talla: '4',
          color: 'AZUL',
          estaturaMinCm: 100,
          estaturaMaxCm: 110,
          pesoMinKg: 15,
          pesoMaxKg: 20,
          stockDisponible: 0,
          disponible: false,
        },
      ],
    });

    const { el, http } = crear(agotada);
    http.verify();

    expect(el.textContent).toContain(
      'En este momento no hay unidades disponibles.',
    );

    const botonColor = el.querySelector<HTMLButtonElement>('.detalle-color');
    expect(botonColor).not.toBeNull();
    expect(botonColor?.disabled).toBe(true);

    const botonAgregar = el.querySelector<HTMLButtonElement>(
      '.detalle-agregar-carrito > .btn-primary',
    );
    expect(botonAgregar?.disabled).toBe(true);

    const botonTalla = el.querySelector<HTMLButtonElement>('.detalle-talla');
    expect(botonTalla).not.toBeNull();
    expect(botonTalla?.disabled).toBe(true);
    expect(carritoMock.agregar).not.toHaveBeenCalled();
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

  describe('selección de estampado (auditoría)', () => {
    /** Elige color y talla para dejar solo el estampado pendiente. */
    function elegirColorYTalla(fixture: ComponentFixture<DetalleProducto>, el: HTMLElement): void {
      el.querySelector<HTMLButtonElement>('.detalle-color[role="radio"]')!.click();
      fixture.detectChanges();
      el.querySelector<HTMLButtonElement>('.detalle-talla[aria-label^="Talla 4"]')!.click();
      fixture.detectChanges();
    }

    it('ofrece solo los estampados de la prenda y exige elegir uno antes de agregar', () => {
      carritoMock.agregar.mockClear();
      const { fixture, el, http } = crear(detalle({ estampado: 'FLORES', estampados: ['FLORES', 'RAYAS'] }));
      http.verify();
      elegirColorYTalla(fixture, el);

      const opciones = Array.from(el.querySelectorAll<HTMLButtonElement>('.detalle-estampado'));
      expect(opciones.map((b) => b.getAttribute('aria-label'))).toEqual(['Estampado Flores', 'Estampado Rayas']);
      const agregar = el.querySelector<HTMLButtonElement>('.detalle-agregar-carrito > .btn-primary')!;
      expect(agregar.disabled).toBe(true);
      expect(el.textContent).toContain('Selecciona un estampado');

      opciones[1].click();
      fixture.detectChanges();

      expect(opciones[1].getAttribute('aria-checked')).toBe('true');
      expect(agregar.disabled).toBe(false);
      agregar.click();
      expect(carritoMock.agregar).toHaveBeenCalledWith(expect.objectContaining({ estampado: 'RAYAS' }));
    });

    it('con un solo estampado lo asume y no obliga a elegir', () => {
      carritoMock.agregar.mockClear();
      const { fixture, el, http } = crear(detalle({ estampado: 'LISO', estampados: ['LISO'] }));
      http.verify();
      elegirColorYTalla(fixture, el);

      expect(el.querySelector<HTMLButtonElement>('.detalle-agregar-carrito > .btn-primary')!.disabled).toBe(false);
      el.querySelector<HTMLButtonElement>('.detalle-agregar-carrito > .btn-primary')!.click();
      expect(carritoMock.agregar).toHaveBeenCalledWith(expect.objectContaining({ estampado: 'LISO' }));
    });

    it('en prendas antiguas sin lista usa su estampado único', () => {
      const { el, http } = crear(detalle({ estampado: 'ESTRELLAS', estampados: undefined }));
      http.verify();

      expect(Array.from(el.querySelectorAll('.detalle-estampado')).map((b) => b.textContent?.trim())).toEqual(['Estrellas']);
    });

    it('la ficha técnica lista todos los estampados de la prenda', () => {
      const { el, http } = crear(detalle({ estampado: 'FLORES', estampados: ['FLORES', 'RAYAS'] }));
      http.verify();

      expect(el.textContent).toContain('Flores, Rayas');
    });
  });
});
