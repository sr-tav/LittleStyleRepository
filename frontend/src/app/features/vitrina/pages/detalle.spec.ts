import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';

import { VitrinaDetalle } from '../models/vitrina.models';
import { DetalleProducto } from './detalle';

function detalle(cambios: Partial<VitrinaDetalle> = {}): VitrinaDetalle {
  return {
    id: 10,
    nombre: 'Pantalón Jogger Azul',
    categoria: 'PANTALONES',
    genero: 'UNISEX',
    descripcion: null,
    marca: 'Tienda Sol',
    precio: 32900,
    imagenes: ['/media/a.png', '/media/b.png'],
    disponible: true,
    colores: ['AZUL'],
    estampado: 'LISO',
    composicion: [{ material: 'ALGODON', porcentaje: 100 }],
    tallas: [
      { talla: '4', estaturaMinCm: 100, estaturaMaxCm: 110, pesoMinKg: 15, pesoMaxKg: 20, disponible: true },
      { talla: '6', estaturaMinCm: 111, estaturaMaxCm: 122, pesoMinKg: 21, pesoMaxKg: 26, disponible: false },
    ],
    fechaActualizacion: new Date().toISOString(),
    ...cambios,
  };
}

describe('DetalleProducto (US-09)', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DetalleProducto],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ id: '10' }),
              queryParamMap: convertToParamMap({}),
            },
          },
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

  it('muestra insignia de nuevo y tabla de medidas con estados', () => {
    const { el, http } = crear(detalle());
    http.verify();

    expect(el.textContent).toContain('Nuevo');
    expect(el.textContent).toContain('100 – 110');
    expect(el.textContent).toContain('Agotada');
    expect(el.querySelector('.tabla-medidas caption')).not.toBeNull();
  });
});
