import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject } from 'rxjs';

import { Pagina, VitrinaItem } from '../models/vitrina.models';
import { Explorar } from './explorar';

function item(cambios: Partial<VitrinaItem> = {}): VitrinaItem {
  return {
    id: 10,
    nombre: 'Pantalón Jogger Azul',
    categoria: 'PANTALONES',
    genero: 'UNISEX',
    marca: 'Tienda Sol',
    precio: 32900,
    imagenUrl: '/media/a.png',
    disponible: true,
    tallasDisponibles: ['4', '6'],
    ...cambios,
  };
}

function pagina(items: VitrinaItem[]): Pagina<VitrinaItem> {
  return { contenido: items, pagina: 0, tamano: 12, totalElementos: items.length, totalPaginas: 1 };
}

describe('Explorar (US-09)', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Explorar],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function crear() {
    const fixture = TestBed.createComponent(Explorar);
    fixture.detectChanges();
    http.expectOne('/api/cliente/catalogo?pagina=0&tamano=12&orden=novedades').flush(pagina([item()]));
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  it('muestra las prendas con precio y enlace al detalle', () => {
    const { el } = crear();

    expect(el.textContent).toContain('Pantalón Jogger Azul');
    expect(el.textContent).toContain('32.900');
    const enlace = el.querySelector<HTMLAnchorElement>('a[href*="/cliente/catalogo/10"]');
    expect(enlace).not.toBeNull();
  });

  it('cambiar un filtro vuelve a la primera página con ese parámetro', () => {
    const { fixture, el } = crear();

    const categoria = el.querySelector<HTMLSelectElement>('select')!;
    categoria.value = 'PANTALONES';
    categoria.dispatchEvent(new Event('change'));
    http
      .expectOne((p) => p.url === '/api/cliente/catalogo' && p.params.get('categoria') === 'PANTALONES')
      .flush(pagina([item()]));
    fixture.detectChanges();

    expect(el.textContent).toContain('Pantalón Jogger Azul');
  });

  it('muestra estado vacío cuando no hay coincidencias', () => {
    const { fixture, el } = crear();

    const categoria = el.querySelector<HTMLSelectElement>('select')!;
    categoria.value = 'VESTIDOS';
    categoria.dispatchEvent(new Event('change'));
    http
      .expectOne((p) => p.url === '/api/cliente/catalogo' && p.params.get('categoria') === 'VESTIDOS')
      .flush(pagina([]));
    fixture.detectChanges();

    expect(el.textContent).toContain('Ninguna prenda coincide');
  });

  it('reacciona a los query params del menú sin recargar la página', () => {
    // Se prueba en un módulo aparte porque el proveedor debe definirse antes de instanciar
    const params = new BehaviorSubject(convertToParamMap({}));
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [Explorar],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { queryParamMap: params.asObservable() } },
      ],
    });
    const httpLocal = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(Explorar);
    fixture.detectChanges();
    httpLocal.expectOne('/api/cliente/catalogo?pagina=0&tamano=12&orden=novedades').flush(pagina([item()]));
    fixture.detectChanges();

    // Como al elegir Niñas en el menú hamburguesa
    params.next(convertToParamMap({ genero: 'NINA' }));
    httpLocal
      .expectOne((p) => p.url === '/api/cliente/catalogo' && p.params.get('genero') === 'NINA')
      .flush(pagina([item()]));
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Pantalón Jogger Azul');
    httpLocal.verify();
  });
});
